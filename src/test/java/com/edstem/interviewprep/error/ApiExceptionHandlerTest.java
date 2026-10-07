package com.edstem.interviewprep.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.edstem.interviewprep.task.TaskController;
import com.edstem.interviewprep.task.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(TaskController.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ApiExceptionHandlerTest {

    private final MockMvcTester mvc;

    @MockitoBean
    private TaskService taskService;

    ApiExceptionHandlerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void unexpectedErrorReturnsGenericJsonWithoutInternals() {
        given(taskService.get(1L)).willThrow(new IllegalStateException("connection pool exhausted"));

        MvcTestResult result = mvc.get().uri("/api/tasks/1").exchange();

        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(500);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Internal Server Error");
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Unexpected error");
        assertThat(result).bodyText().doesNotContain("connection pool exhausted");
    }
}
