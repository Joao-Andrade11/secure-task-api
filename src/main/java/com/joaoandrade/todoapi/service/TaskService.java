package com.joaoandrade.todoapi.service;

import com.joaoandrade.todoapi.dto.TaskCreateRequest;
import com.joaoandrade.todoapi.dto.TaskResponse;
import com.joaoandrade.todoapi.dto.TaskUpdateRequest;
import com.joaoandrade.todoapi.dto.PageResponse;
import com.joaoandrade.todoapi.exception.TaskNotFoundException;
import com.joaoandrade.todoapi.model.Task;
import com.joaoandrade.todoapi.model.TaskStatus;
import com.joaoandrade.todoapi.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public PageResponse<TaskResponse> findAll(TaskStatus status, Pageable pageable) {
        Page<Task> tasks = status == null
                ? taskRepository.findAll(pageable)
                : taskRepository.findByStatus(status, pageable);

        return PageResponse.from(tasks.map(TaskResponse::fromEntity));
    }

    public TaskResponse findById(Long id) {
        return TaskResponse.fromEntity(getTaskOrThrow(id));
    }

    @Transactional
    public TaskResponse create(TaskCreateRequest request) {
        Task task = new Task(normalizeTitle(request.title()), normalizeDescription(request.description()), request.status());
        Task saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved);
    }

    @Transactional
    public TaskResponse update(Long id, TaskUpdateRequest request) {
        Task task = getTaskOrThrow(id);
        task.setTitle(normalizeTitle(request.title()));
        task.setDescription(normalizeDescription(request.description()));
        task.setStatus(request.status());
        Task updated = taskRepository.save(task);
        return TaskResponse.fromEntity(updated);
    }

    @Transactional
    public void delete(Long id) {
        Task task = getTaskOrThrow(id);
        taskRepository.delete(task);
    }

    private Task getTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    private String normalizeTitle(String title) {
        return title.strip();
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.strip();
    }
}
