package com.example.kanban.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateListRequest(
        @Pattern(regexp = ".*\\S.*", message = "Le titre ne peut pas être vide")
        String title,
        Integer position) {
}
