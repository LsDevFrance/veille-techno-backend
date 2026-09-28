package com.example.kanban.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.kanban.dto.AuthResponse;
import com.example.kanban.dto.LoginRequest;
import com.example.kanban.dto.RegisterRequest;
import com.example.kanban.dto.UpdateUserRequest;
import com.example.kanban.dto.UserResponse;
import com.example.kanban.exception.EmailAlreadyUsedException;
import com.example.kanban.exception.ForbiddenException;
import com.example.kanban.exception.InvalidCredentialsException;
import com.example.kanban.exception.UserNotFoundException;
import com.example.kanban.model.Role;
import com.example.kanban.model.User;
import com.example.kanban.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        User user = new User();
        user.setEmail(email);
        user.setName(request.name().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.user);

        return UserResponse.from(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(InvalidCredentialsException::new);

        return new AuthResponse(jwtService.generateToken(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, User currentUser) {
        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        boolean admin = currentUser.getRole() == Role.admin;
        if (!admin && !user.getId().equals(currentUser.getId())) {
            throw new ForbiddenException();
        }
        if (request.role() != null && !admin) {
            throw new ForbiddenException();
        }

        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        if (request.email() != null) {
            String email = request.email().trim().toLowerCase();
            if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
                throw new EmailAlreadyUsedException();
            }
            user.setEmail(email);
        }
        if (request.role() != null) {
            user.setRole(Role.valueOf(request.role()));
        }

        return UserResponse.from(userRepository.save(user));
    }
}
