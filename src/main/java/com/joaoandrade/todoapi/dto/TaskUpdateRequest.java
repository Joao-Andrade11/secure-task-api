package com.joaoandrade.todoapi.dto;

import com.joaoandrade.todoapi.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskUpdateRequest(

        @NotBlank(message = "O titulo e obrigatorio")
        @Size(max = 120, message = "O titulo deve ter no maximo 120 caracteres")
        String title,

        @Size(max = 1000, message = "A descricao deve ter no maximo 1000 caracteres")
        String description,

        @NotNull(message = "O status e obrigatorio")
        TaskStatus status
) {
}
