package com.example.kanban.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/user")
public class UserController {

    @PostMapping("/createWithList")
    @Operation(summary = "Create list of users with given input array", description = "Create a user with an array of users")
    public String createUserWithList() {
        return "User created with list";
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get user by username", description = "Get a user by username")
    public String getUser(@PathVariable String username) {
        return "User " + username;
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update user", description = "Update a user by username")
    public String updateUser(@PathVariable String username) {
        return "User " + username + " updated";
    }

    @DeleteMapping("/{username}")
    @Operation(summary = "Delete user", description = "Delete a user by username")
    public String deleteUser(@PathVariable String username) {
        return "User " + username + " deleted";
    }

    @GetMapping("/login")
    @Operation(summary = "Logs user into the system", description = "Login a user")
    public String login() {
        return "User logged in";
    }

    @GetMapping("/logout")
    @Operation(summary = "Logs out current logged in user session", description = "Logout a user")
    public String logout() {
        return "User logged out";
    }

     @PostMapping("/createWithArray")
    @Operation(summary = "Create list of users with given input array", description = "Create a user with an array of users")
    public String createUserWithArray() {
        return "User created with name:";
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Create a user")
    public String createUser() {
        return "User created";
    }
}