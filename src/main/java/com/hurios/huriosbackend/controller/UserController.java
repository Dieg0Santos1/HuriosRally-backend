package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.config.JwtUtil;
import com.hurios.huriosbackend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

/**
 * UserController - endpoints para gestión de perfil de usuario
 */
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
}