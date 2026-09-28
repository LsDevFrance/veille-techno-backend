package com.example.kanban.dto;

import java.time.Instant;

import com.example.kanban.model.Role;
import com.example.kanban.model.User;

public record UserResponse(
        String id,
        String email,
        String name,
        Role role,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                String.valueOf(user.getId()),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getCreatedAt());
    }
}
