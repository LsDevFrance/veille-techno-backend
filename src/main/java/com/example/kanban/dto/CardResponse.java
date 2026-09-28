package com.example.kanban.dto;

import java.time.Instant;

import com.example.kanban.model.Card;

public record CardResponse(
        String id,
        String title,
        String description,
        Integer position,
        String listId,
        Instant createdAt,
        Instant updatedAt) {

    public static CardResponse from(Card card) {
        return new CardResponse(
                String.valueOf(card.getId()),
                card.getTitle(),
                card.getDescription(),
                card.getPosition(),
                String.valueOf(card.getList().getId()),
                card.getCreatedAt(),
                card.getUpdatedAt());
    }
}
