package com.edstem.interviewprep.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class AdminPropertiesTest {

    @Test
    void halfConfiguredAdminIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AdminProperties("admin@example.com", ""))
                .withMessage("Set both app.admin.email and app.admin.password, or neither");
        assertThatIllegalArgumentException().isThrownBy(() -> new AdminProperties(null, "secret-password"));
    }

    @Test
    void adminIsOptional() {
        assertThat(new AdminProperties("", "").isConfigured()).isFalse();
        assertThat(new AdminProperties("admin@example.com", "secret-password").isConfigured())
                .isTrue();
    }
}
