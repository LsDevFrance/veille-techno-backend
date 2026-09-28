package com.example.kanban.exception;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("Cet email est déjà utilisé");
    }
}
