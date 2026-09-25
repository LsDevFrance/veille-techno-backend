# Kanban Board API

API REST pour une application de tableau Kanban. Elle permet de gérer les utilisateurs, les listes et les cartes.

Stack : Java 25, Spring Boot, Spring Web, documentation OpenAPI via SpringDoc.

## Fonctionnalités

- Inscrire un nouvel utilisateur et connecter un utilisateur
- Modifier les informations d’un utilisateur, y compris ses droits
- Créer une liste
- Créer une carte dans une liste et la modifier (titre, description, etc.)
- Supprimer une carte ou une liste
- Consulter la documentation interactive de l’API

## Démarrage

Prérequis : JDK 25 et Maven (ou le wrapper `./mvnw`).

```bash
./mvnw spring-boot:run
```

L’API écoute sur `http://localhost:8080`.

La documentation interactive est disponible sur :

- Swagger UI : [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Spécification OpenAPI : [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## Modèle

| Ressource | Champs principaux |
| --- | --- |
| Utilisateur | `id`, `username`, `email`, `password`, `role` |
| Liste | `id`, `title` |
| Carte | `id`, `title`, `description`, `listId` |

Le champ `role` vaut `USER` ou `ADMIN`. `ADMIN` peut modifier les droits des autres utilisateurs.

## Authentification

Les routes autres que l’inscription et la connexion attendent un jeton Bearer :

```http
Authorization: Bearer <token>
```

Le jeton est renvoyé par `POST /api/auth/login`.

## Utilisateurs

### Inscrire un utilisateur

`POST /api/users`

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "motdepasse"
}
```

Réponse `201 Created` :

```json
{
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "role": "USER"
}
```

Le mot de passe n’est jamais renvoyé. Un nom d’utilisateur ou un e-mail déjà utilisé renvoie `409 Conflict`.

### Connecter un utilisateur

`POST /api/auth/login`

```json
{
  "username": "alice",
  "password": "motdepasse"
}
```

Réponse `200 OK` :

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "alice",
    "email": "alice@example.com",
    "role": "USER"
  }
}
```

Identifiants incorrects : `401 Unauthorized`.

### Modifier un utilisateur

`PUT /api/users/{id}`

```json
{
  "username": "alice",
  "email": "alice.martin@example.com",
  "password": "nouveauMotDePasse",
  "role": "ADMIN"
}
```

Tous les champs sont optionnels : seuls ceux envoyés sont mis à jour.

Réponse `200 OK` :

```json
{
  "id": 1,
  "username": "alice",
  "email": "alice.martin@example.com",
  "role": "ADMIN"
}
```

Un utilisateur peut modifier son propre profil. Seul un `ADMIN` peut changer le champ `role`. Utilisateur introuvable : `404 Not Found`. Droit insuffisant : `403 Forbidden`.

## Listes

### Créer une liste

`POST /api/lists`

```json
{
  "title": "À faire"
}
```

Réponse `201 Created` :

```json
{
  "id": 1,
  "title": "À faire"
}
```

### Supprimer une liste

`DELETE /api/lists/{id}`

Réponse `204 No Content`. Les cartes de la liste sont supprimées avec elle. Liste introuvable : `404 Not Found`.

## Cartes

### Créer une carte dans une liste

`POST /api/lists/{listId}/cards`

```json
{
  "title": "Rédiger le README",
  "description": "Documenter les endpoints de l’API Kanban"
}
```

Réponse `201 Created` :

```json
{
  "id": 1,
  "title": "Rédiger le README",
  "description": "Documenter les endpoints de l’API Kanban",
  "listId": 1
}
```

Liste introuvable : `404 Not Found`.

### Modifier une carte

`PUT /api/cards/{id}`

```json
{
  "title": "Rédiger le README",
  "description": "Ajouter les exemples de requêtes et les codes de réponse",
  "listId": 2
}
```

`title` et `description` mettent à jour le contenu. `listId` déplace la carte vers une autre liste. Tous les champs sont optionnels.

Réponse `200 OK` :

```json
{
  "id": 1,
  "title": "Rédiger le README",
  "description": "Ajouter les exemples de requêtes et les codes de réponse",
  "listId": 2
}
```

Carte ou liste cible introuvable : `404 Not Found`.

### Supprimer une carte

`DELETE /api/cards/{id}`

Réponse `204 No Content`. Carte introuvable : `404 Not Found`.

## Codes de réponse

| Code | Signification |
| --- | --- |
| `200` | Succès |
| `201` | Ressource créée |
| `204` | Suppression effectuée, pas de contenu |
| `400` | Corps de requête invalide |
| `401` | Non authentifié |
| `403` | Droit insuffisant |
| `404` | Ressource introuvable |
| `409` | Conflit (utilisateur déjà existant) |

## Récapitulatif des routes

| Méthode | Route | Description |
| --- | --- | --- |
| `POST` | `/api/users` | Inscrire un utilisateur |
| `POST` | `/api/auth/login` | Connecter un utilisateur |
| `PUT` | `/api/users/{id}` | Modifier un utilisateur et ses droits |
| `POST` | `/api/lists` | Créer une liste |
| `DELETE` | `/api/lists/{id}` | Supprimer une liste |
| `POST` | `/api/lists/{listId}/cards` | Créer une carte dans une liste |
| `PUT` | `/api/cards/{id}` | Modifier une carte |
| `DELETE` | `/api/cards/{id}` | Supprimer une carte |
