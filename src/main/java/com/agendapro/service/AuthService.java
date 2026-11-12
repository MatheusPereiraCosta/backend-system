package com.agendapro.service;

import com.agendapro.domain.User;
import com.agendapro.domain.enums.UserRole;
import com.agendapro.dto.AuthResponse;
import com.agendapro.dto.LoginRequest;
import com.agendapro.dto.RegisterRequest;
import com.agendapro.dto.UserDTO;
import com.agendapro.exception.DuplicateResourceException;
import com.agendapro.exception.InvalidCredentialsException;
import com.agendapro.exception.ResourceNotFoundException;
import com.agendapro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Já existe uma conta com este e-mail.");
        }
        User user = userRepository.save(User.builder()
                .name(request.getName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ADMIN)
                .build());
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Mesma mensagem para e-mail inexistente e senha errada, para não revelar quais e-mails existem
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDTO me(UUID userId) {
        return userRepository.findById(userId)
                .map(UserDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    public AuthResponse toResponse(User user) {
        return AuthResponse.builder()
                .token(jwtService.generate(user))
                .user(UserDTO.fromEntity(user))
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
