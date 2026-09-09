package com.joaoandrade.todoapi.exception;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long id) {
        super("Tarefa nao encontrada com id: " + id);
    }
}
