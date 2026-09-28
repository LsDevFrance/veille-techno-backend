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

import com.example.kanban.dto.CreateListRequest;
import com.example.kanban.dto.ListResponse;
import com.example.kanban.dto.UpdateListRequest;
import com.example.kanban.service.CurrentUserProvider;
import com.example.kanban.service.ListService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@Tag(name = "Lists", description = "Gestion des listes du Kanban")
@SecurityRequirement(name = "bearerAuth")
public class ListsController {

    private final ListService listService;
    private final CurrentUserProvider currentUserProvider;

    public ListsController(ListService listService, CurrentUserProvider currentUserProvider) {
        this.listService = listService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/api/lists")
    @Operation(summary = "Lister toutes les listes")
    @ApiResponse(responseCode = "200", description = "Tableau de listes",
            content = @Content(schema = @Schema(implementation = ListResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide")
    public List<ListResponse> findAll(HttpServletRequest request) {
        return listService.findAll(currentUserProvider.current(request));
    }

    @PostMapping("/api/lists")
    @Operation(summary = "Créer une nouvelle liste")
    @ApiResponse(responseCode = "201", description = "Liste créée",
            content = @Content(schema = @Schema(implementation = ListResponse.class)))
    @ApiResponse(responseCode = "400", description = "Titre manquant ou vide")
    @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide")
    public ResponseEntity<ListResponse> create(
            @Valid @RequestBody CreateListRequest body,
            HttpServletRequest request) {
        ListResponse created = listService.create(body, currentUserProvider.current(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/api/lists/{id}")
    @Operation(summary = "Modifier une liste (titre, position)", description = "Réservé à l'auteur de la liste.")
    @ApiResponse(responseCode = "200", description = "Liste mise à jour",
            content = @Content(schema = @Schema(implementation = ListResponse.class)))
    @ApiResponse(responseCode = "400", description = "Requête invalide")
    @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    @ApiResponse(responseCode = "404", description = "Liste introuvable")
    public ListResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListRequest body,
            HttpServletRequest request) {
        return listService.update(id, body, currentUserProvider.current(request));
    }

    @DeleteMapping("/api/lists/{id}")
    @Operation(summary = "Supprimer une liste", description = "Réservé à l'auteur de la liste. Les cartes de la liste sont supprimées avec elle.")
    @ApiResponse(responseCode = "204", description = "Liste supprimée")
    @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    @ApiResponse(responseCode = "404", description = "Liste introuvable")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        listService.delete(id, currentUserProvider.current(request));
        return ResponseEntity.noContent().build();
    }
}
