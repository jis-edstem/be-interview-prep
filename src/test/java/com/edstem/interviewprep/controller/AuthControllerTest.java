package com.edstem.interviewprep.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.entity.Role;
import com.edstem.interviewprep.entity.User;
import com.edstem.interviewprep.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class AuthControllerTest {

    private static final String PASSWORD = "correct-horse-battery";

    private final MockMvcTester mvc;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

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

    private MvcTestResult register(String email, String password) {
        return mvc.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password))
                .exchange();
    }
}
