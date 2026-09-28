package com.example.kanban.exception;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException() {
        super("Accès refusé");
    }
}
