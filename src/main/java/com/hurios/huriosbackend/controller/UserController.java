package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.config.JwtUtil;
import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public UserController(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * GET /user/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {

            String token = extractToken(authHeader);
            String email = jwtUtil.validateAndGetSubject(token);

            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            User user = userOpt.get();

            Map<String, Object> profile = new HashMap<>();
            profile.put("email", user.getEmail());
            profile.put("fullName", user.getFullName());
            profile.put("phone", user.getPhone());
            profile.put("address", user.getAddress());
            profile.put("role", user.getRole());
            profile.put("createdAt", user.getCreatedAt());
            profile.put("profileImage", user.getProfileImage());

            return ResponseEntity.ok(profile);

        } catch (Exception e) {

            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token inválido"));
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        throw new IllegalArgumentException("Token inválido");
    }
}