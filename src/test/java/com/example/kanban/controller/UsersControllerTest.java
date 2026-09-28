package com.example.kanban.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.kanban.model.Role;
import com.example.kanban.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest
@AutoConfigureMockMvc
class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Test
    void meReturnsCurrentUserWithoutPassword() throws Exception {
        String email = uniqueEmail("me");
        String password = "motdepasse";
        String token = registerAndLogin(email, password, "Alice");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.role").value("user"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).doesNotContain(password));
    }

    @Test
    void meReturnsUnauthorizedWhenTokenIsMissingInvalidOrExpired() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentification manquante ou invalide"));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer pas-un-jeton"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentification manquante ou invalide"));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentification manquante ou invalide"));
    }

    @Test
    void updateOwnProfileReturnsUpdatedUserWithoutPassword() throws Exception {
        String email = uniqueEmail("own");
        String password = "motdepasse";
        String token = registerAndLogin(email, password, "Alice");
        String id = userRepository.findByEmail(email).orElseThrow().getId().toString();
        String newEmail = uniqueEmail("own-new");

        mockMvc.perform(patch("/api/users/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alicia","email":"%s"}
                                """.formatted(newEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Alicia"))
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.role").value("user"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).doesNotContain(password));
    }

    @Test
    void updateReturnsForbiddenWhenNonAdminEditsAnotherUser() throws Exception {
        String ownerEmail = uniqueEmail("owner");
        registerAndLogin(ownerEmail, "motdepasse", "Alice");
        String ownerId = userRepository.findByEmail(ownerEmail).orElseThrow().getId().toString();

        String token = registerAndLogin(uniqueEmail("other"), "motdepasse", "Bob");

        mockMvc.perform(patch("/api/users/" + ownerId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Piraté"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));

        assertThat(userRepository.findByEmail(ownerEmail).orElseThrow().getName()).isEqualTo("Alice");
    }

    @Test
    void updateReturnsForbiddenWhenNonAdminChangesOwnRole() throws Exception {
        String email = uniqueEmail("role");
        String token = registerAndLogin(email, "motdepasse", "Alice");
        String id = userRepository.findByEmail(email).orElseThrow().getId().toString();

        mockMvc.perform(patch("/api/users/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alicia","role":"admin"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));

        var saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getRole()).isEqualTo(Role.user);
    }

    @Test
    void updateReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        String token = registerAndLogin(uniqueEmail("missing"), "motdepasse", "Alice");

        mockMvc.perform(patch("/api/users/999999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Personne"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Utilisateur introuvable"));
    }

    @Test
    void updateReturnsBadRequestWhenRoleIsUnknown() throws Exception {
        String email = uniqueEmail("invalid-role");
        String token = registerAndLogin(email, "motdepasse", "Alice");
        String id = userRepository.findByEmail(email).orElseThrow().getId().toString();

        mockMvc.perform(patch("/api/users/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"superuser"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.role").value("Le rôle n'est pas reconnu"));
    }

    @Test
    void updateReturnsUnauthorizedWhenTokenIsMissing() throws Exception {
        mockMvc.perform(patch("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentification manquante ou invalide"));
    }

    @Test
    void adminCanChangeAnotherUsersRole() throws Exception {
        String targetEmail = uniqueEmail("target");
        registerAndLogin(targetEmail, "motdepasse", "Alice");
        String targetId = userRepository.findByEmail(targetEmail).orElseThrow().getId().toString();

        String adminToken = registerAndLoginAsAdmin(uniqueEmail("admin"), "motdepasse", "Admin");

        mockMvc.perform(patch("/api/users/" + targetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"admin"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId))
                .andExpect(jsonPath("$.role").value("admin"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    private String registerAndLogin(String email, String password, String name) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","name":"%s"}
                                """.formatted(email, password, name)))
                .andExpect(status().isCreated());
        return login(email, password);
    }

    private String registerAndLoginAsAdmin(String email, String password, String name) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","name":"%s"}
                                """.formatted(email, password, name)))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(Role.admin);
        userRepository.save(user);
        return login(email, password);
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String expiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("1")
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
