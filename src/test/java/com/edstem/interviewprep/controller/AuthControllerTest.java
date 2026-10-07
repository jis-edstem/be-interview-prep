package com.edstem.interviewprep.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.entity.Role;
import com.edstem.interviewprep.entity.User;
import com.edstem.interviewprep.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class AuthControllerTest {

    private static final String PASSWORD = "correct-horse-battery";

    private final MockMvcTester mvc;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtDecoder jwtDecoder;

    @BeforeEach
    void clearUsers() {
        repository.deleteAll();
    }

    @Test
    void registerCreatesUserWithHashedPassword() {
        MvcTestResult result = register("Ada@Example.com", PASSWORD);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.email").isEqualTo("ada@example.com");
        assertThat(result).bodyJson().extractingPath("$.role").isEqualTo("USER");
        assertThat(result).bodyText().doesNotContain(PASSWORD).doesNotContain("password");
        User stored = repository.findByEmail("ada@example.com").orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2a$");
        assertThat(passwordEncoder.matches(PASSWORD, stored.getPasswordHash())).isTrue();
        assertThat(stored.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registeringSameEmailTwiceReturnsConflict() {
        register("ada@example.com", PASSWORD);

        MvcTestResult result = register("ADA@example.com", "another-password");

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("An account with this email already exists");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() {
        MvcTestResult result = register("not-an-email", "short");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("email", "password");
    }

    @Test
    void passwordOverSeventyTwoUtf8BytesIsRejected() {
        MvcTestResult result = register("ada@example.com", "é".repeat(40));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors[0].message")
                .isEqualTo("password must be at most 72 bytes in UTF-8");
        assertThat(repository.count()).isZero();
    }

    @Test
    void loginReturnsBearerTokenThatExpiresInFifteenMinutes() {
        register("ada@example.com", PASSWORD);

        MvcTestResult result = login("ADA@example.com", PASSWORD);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
        assertThat(result).bodyJson().extractingPath("$.expiresIn").isEqualTo(900);
        Jwt jwt = jwtDecoder.decode(accessToken(result));
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
    }

    @Test
    void tokenFromLoginAuthenticatesApiRequests() {
        register("ada@example.com", PASSWORD);
        String token = accessToken(login("ada@example.com", PASSWORD));

        assertThat(mvc.get().uri("/api/tasks").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatusOk();
    }

    @Test
    void loginIgnoresAStaleBearerToken() {
        register("ada@example.com", PASSWORD);

        MvcTestResult result = mvc.post()
                .uri("/api/auth/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentialsJson("ada@example.com", PASSWORD))
                .exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameJson401() {
        register("ada@example.com", PASSWORD);

        MvcTestResult wrongPassword = login("ada@example.com", "wrong-password");
        MvcTestResult unknownEmail = login("nobody@example.com", PASSWORD);

        for (MvcTestResult result : new MvcTestResult[] {wrongPassword, unknownEmail}) {
            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Invalid email or password");
        }
    }

    private MvcTestResult login(String email, String password) {
        return mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentialsJson(email, password))
                .exchange();
    }

    private MvcTestResult register(String email, String password) {
        return mvc.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentialsJson(email, password))
                .exchange();
    }

    private static String accessToken(MvcTestResult result) {
        return JsonPath.read(
                new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8), "$.accessToken");
    }

    private static String credentialsJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }
}
