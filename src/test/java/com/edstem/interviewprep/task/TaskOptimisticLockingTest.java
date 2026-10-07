package com.edstem.interviewprep.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class TaskOptimisticLockingTest {

    private final TaskService service;
    private final TaskRepository repository;
    private final TransactionTemplate transaction;
    private final TransactionTemplate concurrentTransaction;

    TaskOptimisticLockingTest(
            TaskService service, TaskRepository repository, PlatformTransactionManager transactionManager) {
        this.service = service;
        this.repository = repository;
        this.transaction = new TransactionTemplate(transactionManager);
        this.concurrentTransaction = new TransactionTemplate(transactionManager);
        this.concurrentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void updateRacingAConcurrentCommitPassesVersionCheckButFailsOnFlush() {
        long id = repository.save(new Task("Original", null, TaskStatus.TODO, null)).getId();
        TaskRequest staleUpdate = new TaskRequest("Lost edit", null, "IN_PROGRESS", null, 0L);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            repository.findById(id).orElseThrow();
            concurrentTransaction.executeWithoutResult(inner -> repository.findById(id).orElseThrow()
                    .update("Concurrent edit", null, TaskStatus.DONE, null));
            service.update(id, staleUpdate);
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Task stored = repository.findById(id).orElseThrow();
        assertThat(stored.getTitle()).isEqualTo("Concurrent edit");
        assertThat(stored.getVersion()).isEqualTo(1L);
    }
}
