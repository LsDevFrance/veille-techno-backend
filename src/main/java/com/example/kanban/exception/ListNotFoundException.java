package com.example.kanban.exception;

public class ListNotFoundException extends RuntimeException {

    public ListNotFoundException() {
        super("Liste introuvable");
    }
}
