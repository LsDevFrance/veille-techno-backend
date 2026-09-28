package com.example.kanban.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.kanban.dto.UpdateUserRequest;
import com.example.kanban.dto.UserResponse;
import com.example.kanban.service.CurrentUserProvider;
import com.example.kanban.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@Tag(name = "Users", description = "Gestion des utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class UsersController {

    private final CurrentUserProvider currentUserProvider;
    private final UserService userService;

    public UsersController(CurrentUserProvider currentUserProvider, UserService userService) {
        this.currentUserProvider = currentUserProvider;
        this.userService = userService;
    }

    @GetMapping("/api/users/me")
    @Operation(summary = "Récupérer le profil de l'utilisateur connecté")
    @ApiResponse(responseCode = "200", description = "Profil utilisateur",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide")
    public UserResponse me(HttpServletRequest request) {
        return UserResponse.from(currentUserProvider.current(request));
    }

    @PatchMapping("/api/users/{id}")
    @Operation(summary = "Modifier les informations d'un utilisateur (y compris ses droits)")
    @ApiResponse(responseCode = "200", description = "Utilisateur mis à jour",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "400", description = "Requête invalide")
    @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    @ApiResponse(responseCode = "409", description = "Email déjà utilisé")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest) {
        return userService.update(id, request, currentUserProvider.current(httpRequest));
    }
}
