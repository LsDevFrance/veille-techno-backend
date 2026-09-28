package com.example.kanban.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("Authentification manquante ou invalide");
    }
}
