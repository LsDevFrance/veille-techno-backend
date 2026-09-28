package com.example.kanban.exception;

public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException() {
        super("Carte introuvable");
    }
}
