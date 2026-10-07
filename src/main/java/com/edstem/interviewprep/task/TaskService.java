package com.edstem.interviewprep.task;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public List<TaskResponse> list(TaskStatus status) {
        List<Task> tasks = status == null ? repository.findAll() : repository.findByStatus(status);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    public TaskResponse get(Long id) {
        return TaskResponse.from(find(id));
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = new Task(request.title(), request.description(), request.status(), request.dueDate());
        return TaskResponse.from(repository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = find(id);
        task.update(request.title(), request.description(), request.status(), request.dueDate());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Task find(Long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }
}
