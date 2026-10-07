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
        MvcTestResult result = postTask(taskJson("Write tests", "TODO"));

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

    @Test
    void notFoundUsesErrorFormat() {
        MvcTestResult result = mvc.get().uri(TASKS + "/999").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(404);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Not Found");
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Task 999 not found");
        assertThat(result).bodyJson().extractingPath("$.instance").isEqualTo(TASKS + "/999");
    }

    @Test
    void invalidFieldsReturnFieldLevelMessages() {
        String body = """
                {"title": " ", "status": "BLOCKED", "dueDate": "%s"}
                """.formatted(LocalDate.now().minusDays(1));

        MvcTestResult result = postTask(body);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray()
                .containsExactlyInAnyOrder("title", "status", "dueDate");
        assertThat(result).bodyJson().extractingPath("$.errors[?(@.field == 'status')].message").asArray()
                .containsExactly("status must be one of [TODO, IN_PROGRESS, DONE]");
        assertThat(result).bodyJson().extractingPath("$.errors[?(@.field == 'dueDate')].message").asArray()
                .containsExactly("dueDate cannot be in the past");
    }

    @Test
    void missingStatusIsRejected() {
        MvcTestResult result = postTask("""
                {"title": "Write tests"}
                """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].message").isEqualTo("status is required");
    }

    @Test
    void titleOverLimitIsRejected() {
        MvcTestResult result = postTask(taskJson("x".repeat(Task.TITLE_MAX_LENGTH + 1), "TODO"));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].message")
                .isEqualTo("title must be at most " + Task.TITLE_MAX_LENGTH + " characters");
    }

    @Test
    void unknownStatusValueIsRejected() {
        MvcTestResult result = postTask(taskJson("Write tests", "BLOCKED"));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("status");
        assertThat(result).bodyJson().extractingPath("$.errors[0].message")
                .isEqualTo("status must be one of [TODO, IN_PROGRESS, DONE]");
    }

    @Test
    void malformedDueDateIsRejected() {
        MvcTestResult result = postTask("""
                {"title": "Write tests", "status": "TODO", "dueDate": "not-a-date"}
                """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("dueDate");
    }

    @Test
    void wrongJsonTypeNamesTheField() {
        MvcTestResult result = postTask("""
                {"title": ["Write tests"], "status": "TODO"}
                """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("title");
        assertThat(result).bodyJson().extractingPath("$.errors[0].message").isEqualTo("title has the wrong type");
    }

    private MvcTestResult postTask(String body) {
        return mvc.post().uri(TASKS).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
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
