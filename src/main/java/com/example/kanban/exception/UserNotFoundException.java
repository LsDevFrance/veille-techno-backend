package com.example.kanban.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("Utilisateur introuvable");
    }
}
