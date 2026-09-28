package com.example.kanban.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class CardsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void findByListReturnsCardsOrderedByPosition() throws Exception {
        String token = registerAndLogin(uniqueEmail("cards"), "Alice");
        String listId = createList(token, "À faire");

        mockMvc.perform(get("/api/lists/" + listId + "/cards").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        createCard(token, listId, "Deuxième", 2);
        createCard(token, listId, "Première", 1);

        mockMvc.perform(get("/api/lists/" + listId + "/cards").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Première"))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[1].title").value("Deuxième"))
                .andExpect(jsonPath("$[0].listId").value(listId));
    }

    @Test
    void findByListReturnsForbiddenWhenListBelongsToSomeoneElse() throws Exception {
        String owner = registerAndLogin(uniqueEmail("cards-owner"), "Alice");
        String listId = createList(owner, "À faire");
        String other = registerAndLogin(uniqueEmail("cards-other"), "Bob");

        mockMvc.perform(get("/api/lists/" + listId + "/cards").header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));
    }

    @Test
    void findByListReturnsNotFoundWhenListDoesNotExist() throws Exception {
        String token = registerAndLogin(uniqueEmail("cards-missing-list"), "Alice");

        mockMvc.perform(get("/api/lists/999999999/cards").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Liste introuvable"));
    }

    @Test
    void createCardReturnsCreatedCardInTheList() throws Exception {
        String token = registerAndLogin(uniqueEmail("create-card"), "Alice");
        String listId = createList(token, "À faire");

        mockMvc.perform(post("/api/lists/" + listId + "/cards")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Rédiger","description":"Le README","position":4}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Rédiger"))
                .andExpect(jsonPath("$.description").value("Le README"))
                .andExpect(jsonPath("$.position").value(4))
                .andExpect(jsonPath("$.listId").value(listId))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void createCardReturnsForbiddenWhenListBelongsToSomeoneElse() throws Exception {
        String owner = registerAndLogin(uniqueEmail("create-card-owner"), "Alice");
        String listId = createList(owner, "À faire");
        String other = registerAndLogin(uniqueEmail("create-card-other"), "Bob");

        mockMvc.perform(post("/api/lists/" + listId + "/cards")
                        .header(HttpHeaders.AUTHORIZATION, bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Intrus"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));
    }

    @Test
    void createCardReturnsBadRequestWhenTitleIsMissing() throws Exception {
        String token = registerAndLogin(uniqueEmail("create-card-blank"), "Alice");
        String listId = createList(token, "À faire");

        mockMvc.perform(post("/api/lists/" + listId + "/cards")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Le titre est obligatoire"));
    }

    @Test
    void createCardReturnsNotFoundWhenListDoesNotExist() throws Exception {
        String token = registerAndLogin(uniqueEmail("create-card-missing"), "Alice");

        mockMvc.perform(post("/api/lists/999999999/cards")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Orpheline"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Liste introuvable"));
    }

    @Test
    void findCardReturnsCardForListOwner() throws Exception {
        String token = registerAndLogin(uniqueEmail("get-card"), "Alice");
        String listId = createList(token, "À faire");
        String cardId = createCard(token, listId, "Tâche", null);

        mockMvc.perform(get("/api/cards/" + cardId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.title").value("Tâche"))
                .andExpect(jsonPath("$.listId").value(listId));
    }

    @Test
    void findCardReturnsForbiddenWhenListBelongsToSomeoneElse() throws Exception {
        String owner = registerAndLogin(uniqueEmail("get-card-owner"), "Alice");
        String listId = createList(owner, "À faire");
        String cardId = createCard(owner, listId, "Tâche", null);
        String other = registerAndLogin(uniqueEmail("get-card-other"), "Bob");

        mockMvc.perform(get("/api/cards/" + cardId).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));
    }

    @Test
    void findCardReturnsNotFoundWhenCardDoesNotExist() throws Exception {
        String token = registerAndLogin(uniqueEmail("get-card-missing"), "Alice");

        mockMvc.perform(get("/api/cards/999999999").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Carte introuvable"));
    }

    @Test
    void updateCardUpdatesFieldsAndCanMoveToAnotherOwnedList() throws Exception {
        String token = registerAndLogin(uniqueEmail("patch-card"), "Alice");
        String sourceId = createList(token, "À faire");
        String targetId = createList(token, "Fait");
        String cardId = createCard(token, sourceId, "Tâche", null);

        mockMvc.perform(patch("/api/cards/" + cardId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Tâche faite","description":"Relue","position":5,"listId":"%s"}
                                """.formatted(targetId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.title").value("Tâche faite"))
                .andExpect(jsonPath("$.description").value("Relue"))
                .andExpect(jsonPath("$.position").value(5))
                .andExpect(jsonPath("$.listId").value(targetId));
    }

    @Test
    void updateCardReturnsForbiddenWhenCardBelongsToSomeoneElse() throws Exception {
        String owner = registerAndLogin(uniqueEmail("patch-card-owner"), "Alice");
        String listId = createList(owner, "À faire");
        String cardId = createCard(owner, listId, "Tâche", null);
        String other = registerAndLogin(uniqueEmail("patch-card-other"), "Bob");

        mockMvc.perform(patch("/api/cards/" + cardId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Piraté"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));

        mockMvc.perform(get("/api/cards/" + cardId).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(jsonPath("$.title").value("Tâche"));
    }

    @Test
    void updateCardReturnsForbiddenWhenTargetListBelongsToSomeoneElse() throws Exception {
        String owner = registerAndLogin(uniqueEmail("move-owner"), "Alice");
        String listId = createList(owner, "À faire");
        String cardId = createCard(owner, listId, "Tâche", null);
        String other = registerAndLogin(uniqueEmail("move-other"), "Bob");
        String foreignListId = createList(other, "Ailleurs");

        mockMvc.perform(patch("/api/cards/" + cardId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"listId":"%s"}
                                """.formatted(foreignListId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));

        mockMvc.perform(get("/api/cards/" + cardId).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(jsonPath("$.listId").value(listId));
    }

    @Test
    void updateCardReturnsNotFoundWhenCardOrTargetListDoesNotExist() throws Exception {
        String token = registerAndLogin(uniqueEmail("patch-missing"), "Alice");
        String listId = createList(token, "À faire");
        String cardId = createCard(token, listId, "Tâche", null);

        mockMvc.perform(patch("/api/cards/999999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Inconnue"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Carte introuvable"));

        mockMvc.perform(patch("/api/cards/" + cardId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"listId":"999999999"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Liste introuvable"));
    }

    @Test
    void updateCardReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
        String token = registerAndLogin(uniqueEmail("patch-invalid"), "Alice");
        String listId = createList(token, "À faire");
        String cardId = createCard(token, listId, "Tâche", null);

        mockMvc.perform(patch("/api/cards/" + cardId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","listId":"abc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Le titre ne peut pas être vide"))
                .andExpect(jsonPath("$.errors.listId").value("L'identifiant de liste n'est pas valide"));
    }

    @Test
    void deleteCardRemovesItForListOwner() throws Exception {
        String token = registerAndLogin(uniqueEmail("delete-card"), "Alice");
        String listId = createList(token, "À faire");
        String cardId = createCard(token, listId, "Tâche", null);

        mockMvc.perform(delete("/api/cards/" + cardId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cards/" + cardId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCardReturnsForbiddenWhenListBelongsToSomeoneElse() throws Exception {
        String owner = registerAndLogin(uniqueEmail("delete-card-owner"), "Alice");
        String listId = createList(owner, "À faire");
        String cardId = createCard(owner, listId, "Tâche", null);
        String other = registerAndLogin(uniqueEmail("delete-card-other"), "Bob");

        mockMvc.perform(delete("/api/cards/" + cardId).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));

        mockMvc.perform(get("/api/cards/" + cardId).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteCardReturnsNotFoundWhenCardDoesNotExist() throws Exception {
        String token = registerAndLogin(uniqueEmail("delete-card-missing"), "Alice");

        mockMvc.perform(delete("/api/cards/999999999").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Carte introuvable"));
    }

    @Test
    void cardRoutesReturnUnauthorizedWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/lists/1/cards"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentification manquante ou invalide"));

        mockMvc.perform(post("/api/lists/1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Tâche"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/cards/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/cards/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Tâche"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/cards/1"))
                .andExpect(status().isUnauthorized());
    }

    private String createList(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/lists")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s"}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String createCard(String token, String listId, String title, Integer position) throws Exception {
        String body = position == null
                ? """
                {"title":"%s"}
                """.formatted(title)
                : """
                {"title":"%s","position":%d}
                """.formatted(title, position);
        MvcResult result = mockMvc.perform(post("/api/lists/" + listId + "/cards")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String registerAndLogin(String email, String name) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"motdepasse","name":"%s"}
                                """.formatted(email, name)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"motdepasse"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
