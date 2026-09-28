package com.example.kanban.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCardRequest(
        @NotBlank(message = "Le titre est obligatoire")
        String title,
        String description,
        Integer position) {
}
