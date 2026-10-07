package com.edstem.interviewprep.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.edstem.interviewprep.controller.TaskController;
import com.edstem.interviewprep.entity.Task;
import com.edstem.interviewprep.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(TaskController.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ApiExceptionHandlerTest {

    private final MockMvcTester mvc;

    @MockitoBean
    private TaskService taskService;

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

    @Test
    void concurrentModificationReturnsConflict() {
        given(taskService.update(eq(1L), any())).willThrow(new ObjectOptimisticLockingFailureException(Task.class, 1L));

        MvcTestResult result = mvc.put()
                .uri("/api/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Write tests", "status": "TODO", "version": 0}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Resource was modified by another request; reload it and retry");
    }
}
