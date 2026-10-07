package com.edstem.interviewprep.repository;

import com.edstem.interviewprep.entity.Task;
import com.edstem.interviewprep.entity.TaskStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);
}
