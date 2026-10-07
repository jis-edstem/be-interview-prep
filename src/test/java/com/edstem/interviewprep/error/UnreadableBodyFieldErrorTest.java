package com.edstem.interviewprep.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class UnreadableBodyFieldErrorTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void listElementPathIncludesIndex() {
        MismatchedInputException mismatch = MismatchedInputException.from(null, String.class, "wrong type");
        mismatch.prependPath(new Reference(null, "name"));
        mismatch.prependPath(new Reference(null, 1));
        mismatch.prependPath(new Reference(null, "items"));

        ProblemDetail problem = handle(mismatch);

        assertThat(problem.getProperties()).containsEntry("errors",
                List.of(new FieldErrorResponse("items[1].name", "items[1].name has the wrong type")));
    }

    private ProblemDetail handle(MismatchedInputException mismatch) {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("unreadable", mismatch, new MockHttpInputMessage(new byte[0]));
        return (ProblemDetail) handler.handleHttpMessageNotReadable(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()))
                .getBody();
    }
}
