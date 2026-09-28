package com.example.kanban.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateListRequest(
        @NotBlank(message = "Le titre est obligatoire")
        String title,
        Integer position) {
}
