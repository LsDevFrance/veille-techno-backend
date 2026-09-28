package com.example.kanban.dto;

import java.time.Instant;

import com.example.kanban.model.BoardList;

public record ListResponse(
        String id,
        String title,
        Integer position,
        String ownerId,
        Instant createdAt) {

    public static ListResponse from(BoardList list) {
        return new ListResponse(
                String.valueOf(list.getId()),
                list.getTitle(),
                list.getPosition(),
                String.valueOf(list.getOwner().getId()),
                list.getCreatedAt());
    }
}
