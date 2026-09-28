package com.example.kanban.service;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.example.kanban.exception.UnauthorizedException;
import com.example.kanban.model.User;
import com.example.kanban.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class CurrentUserProvider {

    public static final String CURRENT_USER = "currentUser";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public CurrentUserProvider(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public User require(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new UnauthorizedException();
        }

        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedException();
        }

        Long userId = jwtService.parseUserId(token);
        return userRepository.findById(userId).orElseThrow(UnauthorizedException::new);
    }

    public User current(HttpServletRequest request) {
        Object value = request.getAttribute(CURRENT_USER);
        if (value instanceof User user) {
            return user;
        }
        throw new UnauthorizedException();
    }
}
