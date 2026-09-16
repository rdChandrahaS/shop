package com.shop.authservice.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.authservice.dto.AuthResponseDTO;
import com.shop.authservice.dto.LoginRequestDTO;
import com.shop.authservice.dto.RegisterRequestDTO;
import com.shop.authservice.model.User;
import com.shop.authservice.repo.UserRepository;
import com.shop.authservice.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public void register(RegisterRequestDTO request) {
        String username = request.getUsername().trim();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        newUser.setName(request.getName().trim());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));

        // Never trust roles supplied by a public registration request.
        // Administrative accounts must be provisioned by an administrator/database process.
        newUser.setRoles(Set.of("USER"));

        try {
            userRepository.saveAndFlush(newUser);
        } catch (DataIntegrityViolationException ex) {
            // Protect against two concurrent registrations racing on the unique username constraint.
            throw new IllegalArgumentException("Username already taken");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        String username = request.getUsername().trim();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        Set<String> roles = user.getRoles() == null ? Set.of("USER") : user.getRoles();
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roles);
        return new AuthResponseDTO(token, user.getId(), user.getUsername());
    }
}
