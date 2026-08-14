package com.shop.authservice.service;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
	
	public ResponseEntity<String> register(RegisterRequestDTO request) {
		if(userRepository.existsByUsername(request.getUsername())) {
			return ResponseEntity.badRequest().body("Username already taken!");
		}
		User newUser = new User();
		newUser.setUsername(request.getUsername());
		newUser.setEmail(request.getEmail());
		newUser.setName(request.getName());
		newUser.setPassword(passwordEncoder.encode(request.getPassword()));
		newUser.setRoles(Set.of("USER"));
		
		userRepository.save(newUser);
		return ResponseEntity.ok("User registered successfully!");
	}

	public ResponseEntity<AuthResponseDTO> login(LoginRequestDTO request) {
		User user = userRepository.findByUsername(request.getUsername())
								.orElseThrow(() -> new RuntimeException("Invalid username or password"));
		if(!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new RuntimeException("Invalid username or password");
        }
		
		String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoles());
		return ResponseEntity.ok(new AuthResponseDTO(token, user.getId(), user.getUsername()));
	}

}
