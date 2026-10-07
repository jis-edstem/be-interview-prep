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

    private final TaskRepository repository;
    private final TransactionTemplate transaction;
    private final TransactionTemplate concurrentTransaction;

    TaskOptimisticLockingTest(TaskRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transaction = new TransactionTemplate(transactionManager);
        this.concurrentTransaction = new TransactionTemplate(transactionManager);
        this.concurrentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void updateBasedOnSameVersionAsCommittedConcurrentUpdateFails() {
        long id = repository.save(new Task("Original", null, TaskStatus.TODO, null)).getId();

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            Task loaded = repository.findById(id).orElseThrow();
            concurrentTransaction.executeWithoutResult(inner -> repository.findById(id).orElseThrow()
                    .update("Concurrent edit", null, TaskStatus.DONE, null));
            loaded.update("Lost edit", null, TaskStatus.IN_PROGRESS, null);
            repository.flush();
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Task stored = repository.findById(id).orElseThrow();
        assertThat(stored.getTitle()).isEqualTo("Concurrent edit");
        assertThat(stored.getVersion()).isEqualTo(1L);
    }
}
