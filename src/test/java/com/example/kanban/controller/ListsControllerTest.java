package com.example.kanban.controller;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.example.kanban.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class ListsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findAllReturnsOnlyCurrentUserLists() throws Exception {
        String aliceEmail = uniqueEmail("alice-lists");
        String alice = registerAndLogin(aliceEmail, "Alice");
        String bob = registerAndLogin(uniqueEmail("bob-lists"), "Bob");

        mockMvc.perform(get("/api/lists").header(HttpHeaders.AUTHORIZATION, bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        createList(alice, "À faire", 1);
        createList(bob, "Secret", 1);

        String aliceId = userRepository.findByEmail(aliceEmail).orElseThrow().getId().toString();
        mockMvc.perform(get("/api/lists").header(HttpHeaders.AUTHORIZATION, bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("À faire"))
                .andExpect(jsonPath("$[0].ownerId").value(aliceId));
    }

    @Test
    void createListReturnsCreatedListOwnedByCurrentUser() throws Exception {
        String email = uniqueEmail("create-list");
        String token = registerAndLogin(email, "Alice");
        String ownerId = userRepository.findByEmail(email).orElseThrow().getId().toString();

        mockMvc.perform(post("/api/lists")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"En cours","position":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("En cours"))
                .andExpect(jsonPath("$.position").value(2))
                .andExpect(jsonPath("$.ownerId").value(ownerId))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void createListReturnsBadRequestWhenTitleIsMissingOrBlank() throws Exception {
        String token = registerAndLogin(uniqueEmail("blank-list"), "Alice");

        mockMvc.perform(post("/api/lists")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Le titre est obligatoire"));

        mockMvc.perform(post("/api/lists")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Le titre est obligatoire"));
    }

    @Test
    void updateListReturnsUpdatedListForOwner() throws Exception {
        String token = registerAndLogin(uniqueEmail("update-list"), "Alice");
        String id = createList(token, "À faire", null);

        mockMvc.perform(patch("/api/lists/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Fait","position":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Fait"))
                .andExpect(jsonPath("$.position").value(3));
    }

    @Test
    void updateListReturnsForbiddenWhenListBelongsToSomeoneElse() throws Exception {
        String owner = registerAndLogin(uniqueEmail("owner-list"), "Alice");
        String id = createList(owner, "À faire", null);
        String other = registerAndLogin(uniqueEmail("other-list"), "Bob");

        mockMvc.perform(patch("/api/lists/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Piraté"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));

        mockMvc.perform(get("/api/lists").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(jsonPath("$[0].title").value("À faire"));
    }

    @Test
    void updateListReturnsNotFoundWhenListDoesNotExist() throws Exception {
        String token = registerAndLogin(uniqueEmail("missing-list"), "Alice");

        mockMvc.perform(patch("/api/lists/999999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Inconnue"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Liste introuvable"));
    }

    @Test
    void updateListReturnsBadRequestWhenTitleIsBlank() throws Exception {
        String token = registerAndLogin(uniqueEmail("invalid-list"), "Alice");
        String id = createList(token, "À faire", null);

        mockMvc.perform(patch("/api/lists/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Le titre ne peut pas être vide"));
    }

    @Test
    void deleteListRemovesListAndItsCards() throws Exception {
        String token = registerAndLogin(uniqueEmail("delete-list"), "Alice");
        String listId = createList(token, "À faire", null);
        String cardId = createCard(token, listId, "Tâche");

        mockMvc.perform(delete("/api/lists/" + listId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEmpty());

        mockMvc.perform(get("/api/lists").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/cards/" + cardId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Carte introuvable"));
    }

    @Test
    void deleteListReturnsForbiddenWhenListBelongsToSomeoneElse() throws Exception {
        String owner = registerAndLogin(uniqueEmail("delete-owner"), "Alice");
        String id = createList(owner, "À faire", null);
        String other = registerAndLogin(uniqueEmail("delete-other"), "Bob");

        mockMvc.perform(delete("/api/lists/" + id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));

        mockMvc.perform(get("/api/lists").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deleteListReturnsNotFoundWhenListDoesNotExist() throws Exception {
        String token = registerAndLogin(uniqueEmail("delete-missing"), "Alice");

        mockMvc.perform(delete("/api/lists/999999999").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Liste introuvable"));
    }

    @Test
    void listRoutesReturnUnauthorizedWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/lists"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentification manquante ou invalide"));

        mockMvc.perform(post("/api/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"À faire"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/lists/1"))
                .andExpect(status().isUnauthorized());
    }

    private String createList(String token, String title, Integer position) throws Exception {
        String body = position == null
                ? """
                {"title":"%s"}
                """.formatted(title)
                : """
                {"title":"%s","position":%d}
                """.formatted(title, position);
        MvcResult result = mockMvc.perform(post("/api/lists")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String createCard(String token, String listId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/lists/" + listId + "/cards")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s"}
                                """.formatted(title)))
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
