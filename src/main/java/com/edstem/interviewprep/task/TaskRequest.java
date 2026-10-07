package com.edstem.interviewprep.task;

import com.edstem.interviewprep.validation.EnumValue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequest(
        @NotBlank(message = "title is required")
        @Size(max = Task.TITLE_MAX_LENGTH, message = "title must be at most {max} characters")
        String title,

        @Size(max = Task.DESCRIPTION_MAX_LENGTH, message = "description must be at most {max} characters")
        String description,

        @NotNull(message = "status is required")
        @EnumValue(value = TaskStatus.class, message = "status must be one of {allowed}")
        String status,

        @FutureOrPresent(message = "dueDate cannot be in the past")
        LocalDate dueDate) {

    TaskStatus taskStatus() {
        return TaskStatus.valueOf(status);
    }
}
