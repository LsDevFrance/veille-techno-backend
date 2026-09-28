package com.example.kanban.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.kanban.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Test
    void registerReturnsCreatedUserWithoutPassword() throws Exception {
        String email = "alice-" + UUID.randomUUID() + "@example.com";
        String password = "motdepasse";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","name":"Alice"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.role").value("user"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).doesNotContain(password));

        var saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo(password);
        assertThat(saved.getPassword()).startsWith("$2");
    }

    @Test
    void registerReturnsConflictWhenEmailAlreadyUsed() throws Exception {
        String email = "bob-" + UUID.randomUUID() + "@example.com";
        String body = """
                {"email":"%s","password":"motdepasse","name":"Bob"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cet email est déjà utilisé"));
    }

    @Test
    void registerReturnsBadRequestWhenEmailOrPasswordIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"pas-un-email","password":"court","name":"Alice"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("L'email n'est pas valide"))
                .andExpect(jsonPath("$.errors.password").value("Le mot de passe doit contenir au moins 8 caractères"));
    }

    @Test
    void loginReturnsJwtWithoutPassword() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        String password = "motdepasse";
        register(email, password, "Alice");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(password);

        String token = JsonPath.read(body, "$.accessToken");
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        String userId = String.valueOf(userRepository.findByEmail(email).orElseThrow().getId());
        assertThat(claims.getSubject()).isEqualTo(userId);
        assertThat(claims.getExpiration().toInstant())
                .isCloseTo(Instant.now().plus(1, ChronoUnit.HOURS), within(1, ChronoUnit.MINUTES));
        assertThat(claims).doesNotContainValue(password);
    }

    @Test
    void loginReturnsUnauthorizedWhenEmailIsUnknownOrPasswordIsWrong() throws Exception {
        String email = "login-bad-" + UUID.randomUUID() + "@example.com";
        register(email, "motdepasse", "Alice");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"inconnu-%s","password":"motdepasse"}
                                """.formatted(UUID.randomUUID() + "@example.com")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"mauvais-mot-de-passe"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    private void register(String email, String password, String name) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","name":"%s"}
                                """.formatted(email, password, name)))
                .andExpect(status().isCreated());
    }
}
