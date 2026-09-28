package com.example.kanban.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateCardRequest(
        @Pattern(regexp = ".*\\S.*", message = "Le titre ne peut pas être vide")
        String title,
        String description,
        Integer position,
        @Pattern(regexp = "\\d+", message = "L'identifiant de liste n'est pas valide")
        String listId) {
}
