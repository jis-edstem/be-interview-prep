package com.edstem.interviewprep.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class TaskControllerTest {

    private static final String TASKS = "/api/tasks";
    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    private final MockMvcTester mvc;
    private final TaskRepository repository;

    TaskControllerTest(MockMvcTester mvc, TaskRepository repository) {
        this.mvc = mvc;
        this.repository = repository;
    }

    @BeforeEach
    void clearTasks() {
        repository.deleteAll();
    }

    @Test
    void createReturnsTaskWithLocation() {
        MvcTestResult result = mvc.post().uri(TASKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(taskJson("Write tests", "TODO"))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        long id = repository.findAll().getFirst().getId();
        assertThat(result.getResponse().getHeader("Location")).endsWith(TASKS + "/" + id);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Write tests");
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("TODO");
        assertThat(result).bodyJson().extractingPath("$.dueDate").isEqualTo(TOMORROW.toString());
        assertThat(result).bodyJson().extractingPath("$.createdAt").isNotNull();
    }

    @Test
    void getReturnsTask() {
        long id = saveTask("Write tests", TaskStatus.TODO);

        MvcTestResult result = mvc.get().uri(TASKS + "/" + id).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.id").convertTo(Long.class).isEqualTo(id);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Write tests");
    }

    @Test
    void listReturnsAllTasks() {
        saveTask("First", TaskStatus.TODO);
        saveTask("Second", TaskStatus.DONE);

        MvcTestResult result = mvc.get().uri(TASKS).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$").asArray().hasSize(2);
    }

    @Test
    void listFiltersByStatus() {
        saveTask("First", TaskStatus.TODO);
        saveTask("Second", TaskStatus.DONE);

        MvcTestResult result = mvc.get().uri(TASKS).param("status", "DONE").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[*].title").asArray().containsExactly("Second");
    }

    @Test
    void updateReplacesTaskFields() {
        long id = saveTask("Write tests", TaskStatus.TODO);

        MvcTestResult result = mvc.put().uri(TASKS + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(taskJson("Write more tests", "IN_PROGRESS"))
                .exchange();

        assertThat(result).hasStatusOk();
        Task updated = repository.findById(id).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Write more tests");
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void deleteRemovesTask() {
        long id = saveTask("Write tests", TaskStatus.TODO);

        assertThat(mvc.delete().uri(TASKS + "/" + id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void unknownTaskReturnsNotFound() {
        String unknown = TASKS + "/999";

        assertThat(mvc.get().uri(unknown)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.put().uri(unknown).contentType(MediaType.APPLICATION_JSON).content(taskJson("x", "TODO")))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri(unknown)).hasStatus(HttpStatus.NOT_FOUND);
    }

    private long saveTask(String title, TaskStatus status) {
        return repository.save(new Task(title, "Cover the API", status, TOMORROW)).getId();
    }

    private static String taskJson(String title, String status) {
        return """
                {"title": "%s", "description": "Cover the API", "status": "%s", "dueDate": "%s"}
                """.formatted(title, status, TOMORROW);
    }
}
