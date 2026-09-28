package com.example.kanban.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRequest(
        @Pattern(regexp = ".*\\S.*", message = "Le nom ne peut pas être vide")
        String name,

        @Email(message = "L'email n'est pas valide")
        String email,

        @Pattern(regexp = "user|admin", message = "Le rôle n'est pas reconnu")
        String role) {
}
