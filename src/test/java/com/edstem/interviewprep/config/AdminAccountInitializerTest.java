package com.edstem.interviewprep.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.entity.Role;
import com.edstem.interviewprep.entity.User;
import com.edstem.interviewprep.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {"app.admin.email=Root@Example.com", "app.admin.password=admin-password-123"})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class AdminAccountInitializerTest {

    private final AdminAccountInitializer initializer;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Test
    void configuredAdminIsCreatedOnceAtStartup() {
        initializer.run(new DefaultApplicationArguments());

        User admin = repository.findByEmail("root@example.com").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches("admin-password-123", admin.getPasswordHash()))
                .isTrue();
        assertThat(repository.findAll())
                .filteredOn(user -> user.getEmail().equals("root@example.com"))
                .hasSize(1);
    }
}
