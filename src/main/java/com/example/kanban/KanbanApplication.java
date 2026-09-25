package com.example.kanban;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class KanbanApplication {
    public static void main(String[] args) {
        SpringApplication.run(KanbanApplication.class, args);
    }
}