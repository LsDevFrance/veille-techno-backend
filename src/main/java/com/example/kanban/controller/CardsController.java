package com.example.kanban.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.kanban.dto.CardResponse;
import com.example.kanban.dto.CreateCardRequest;
import com.example.kanban.dto.UpdateCardRequest;
import com.example.kanban.service.CardService;
import com.example.kanban.service.CurrentUserProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@Tag(name = "Cards", description = "Gestion des cartes du Kanban")
@SecurityRequirement(name = "bearerAuth")
public class CardsController {

    private final CardService cardService;
    private final CurrentUserProvider currentUserProvider;

    public CardsController(CardService cardService, CurrentUserProvider currentUserProvider) {
        this.cardService = cardService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/api/lists/{listId}/cards")
    @Operation(summary = "Lister les cartes d'une liste", description = "Réservé à l'auteur de la liste.")
    @ApiResponse(responseCode = "200", description = "Tableau de cartes",
            content = @Content(schema = @Schema(implementation = CardResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    @ApiResponse(responseCode = "404", description = "Liste introuvable")
    public List<CardResponse> findByList(@PathVariable Long listId, HttpServletRequest request) {
        return cardService.findByList(listId, currentUserProvider.current(request));
    }

    @PostMapping("/api/lists/{listId}/cards")
    @Operation(summary = "Créer une nouvelle carte dans une liste", description = "Réservé à l'auteur de la liste.")
    @ApiResponse(responseCode = "201", description = "Carte créée",
            content = @Content(schema = @Schema(implementation = CardResponse.class)))
    @ApiResponse(responseCode = "400", description = "Titre manquant ou vide")
    @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    @ApiResponse(responseCode = "404", description = "Liste introuvable")
    public ResponseEntity<CardResponse> create(
            @PathVariable Long listId,
            @Valid @RequestBody CreateCardRequest body,
            HttpServletRequest request) {
        CardResponse created = cardService.create(listId, body, currentUserProvider.current(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/cards/{id}")
    @Operation(summary = "Récupérer une carte", description = "Réservé à l'auteur de la liste parente.")
    @ApiResponse(responseCode = "200", description = "Carte",
            content = @Content(schema = @Schema(implementation = CardResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    @ApiResponse(responseCode = "404", description = "Carte introuvable")
    public CardResponse findById(@PathVariable Long id, HttpServletRequest request) {
        return cardService.findById(id, currentUserProvider.current(request));
    }

    @PatchMapping("/api/cards/{id}")
    @Operation(summary = "Modifier une carte (titre, description, position, liste)",
            description = "Réservé à l'auteur de la liste parente. En cas de déplacement, l'utilisateur doit aussi être l'auteur de la liste cible.")
    @ApiResponse(responseCode = "200", description = "Carte mise à jour",
            content = @Content(schema = @Schema(implementation = CardResponse.class)))
    @ApiResponse(responseCode = "400", description = "Requête invalide")
    @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    @ApiResponse(responseCode = "404", description = "Carte ou liste cible introuvable")
    public CardResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCardRequest body,
            HttpServletRequest request) {
        return cardService.update(id, body, currentUserProvider.current(request));
    }

    @DeleteMapping("/api/cards/{id}")
    @Operation(summary = "Supprimer une carte", description = "Réservé à l'auteur de la liste parente.")
    @ApiResponse(responseCode = "204", description = "Carte supprimée")
    @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    @ApiResponse(responseCode = "404", description = "Carte introuvable")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        cardService.delete(id, currentUserProvider.current(request));
        return ResponseEntity.noContent().build();
    }
    
}
