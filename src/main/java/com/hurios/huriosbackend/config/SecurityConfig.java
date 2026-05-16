package com.hurios.huriosbackend.config; // ajusta el package si tu estructura es diferente

// Importaciones de Spring
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Importaciones de Spring Security
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Importa tu utilitario y filtro JWT (asegúrate que el package sea el correcto)

/*
 * SecurityConfig: configuración mínima de Spring Security
 * - expone BCryptPasswordEncoder como bean
 * - configura las reglas: permite /auth/** y exige auth en otras rutas
 * - registra JwtAuthFilter para validar JWT en cada request
 */
@Configuration
public class SecurityConfig {

    // Bean para encriptación de contraseñas usando BCrypt
    // Puedes inyectar este bean donde necesites (ej. AuthService)
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Configuración del filtro de seguridad
    // Recibe HttpSecurity para configurar reglas y JwtUtil (se pasa al filtro)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtUtil jwtUtil) throws Exception {
        // crear e insertar nuestro filtro JWT, pasándole la utilidad JwtUtil
        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtUtil);

        http
            .csrf(csrf -> csrf.disable()) // deshabilitar CSRF para APIs (en producción revisa esto)
            .cors(cors -> cors.configure(http)) // habilitar CORS
            .authorizeHttpRequests(auth -> auth
                // rutas públicas (auth)
                .requestMatchers("/auth/**").permitAll()
                // rutas públicas (products - para que usuarios puedan ver productos sin login)
                .requestMatchers("/products/**").permitAll()
                // rutas públicas (export - para exportar reportes)
                .requestMatchers("/export/**").permitAll()
                // rutas públicas (uploads - para servir imágenes)
                .requestMatchers("/uploads/**").permitAll()
                // rutas públicas (api/images - para subir imágenes)
                .requestMatchers("/api/images/**").permitAll()
                // cualquier otra ruta requiere autenticación
                .anyRequest().authenticated()
            )
            // insertar nuestro filtro antes del filtro de autenticación por defecto
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // construir y devolver la SecurityFilterChain
        return http.build();
    }
}
