package com.edstem.interviewprep.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.entity.Role;
import com.edstem.interviewprep.entity.User;
import com.edstem.interviewprep.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class UserControllerTest {

    private static final String PASSWORD = "correct-horse-battery";

    private final MockMvcTester mvc;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @BeforeEach
    void createUsers() {
        repository.deleteAll();
        repository.save(new User("user@example.com", passwordEncoder.encode(PASSWORD), Role.USER));
        repository.save(new User("admin@example.com", passwordEncoder.encode(PASSWORD), Role.ADMIN));
    }

    @Test
    void loggedInUserSeesOwnProfile() {
        MvcTestResult result = getAs("/api/users/me", "user@example.com");

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.email").isEqualTo("user@example.com");
        assertThat(result).bodyJson().extractingPath("$.role").isEqualTo("USER");
        assertThat(result).bodyText().doesNotContain("passwordHash");
    }

    @Test
    void userCannotListAllUsers() {
        MvcTestResult result = getAs("/api/users", "user@example.com");

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("You do not have permission to access this resource");
    }

    @Test
    void userCannotReachAdminEndpointWithHead() {
        MvcTestResult result = mvc.head()
                .uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login("user@example.com"))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanListAllUsers() {
        MvcTestResult result = getAs("/api/users", "admin@example.com");

        assertThat(result).hasStatusOk();
        assertThat(result)
                .bodyJson()
                .extractingPath("$[*].email")
                .asArray()
                .containsExactlyInAnyOrder("user@example.com", "admin@example.com");
    }

    @Test
    void profileRequiresLogin() {
        assertThat(mvc.get().uri("/api/users/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult getAs(String uri, String email) {
        return mvc.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login(email))
                .exchange();
    }

    private String login(String email) {
        MvcTestResult result = mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, PASSWORD))
                .exchange();
        return JsonPath.read(
                new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8), "$.accessToken");
    }
}
