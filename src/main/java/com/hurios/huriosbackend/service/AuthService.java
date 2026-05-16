package com.hurios.huriosbackend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

import com.hurios.huriosbackend.config.JwtUtil;
import com.hurios.huriosbackend.entity.*;
import com.hurios.huriosbackend.repository.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

/*
 * AuthService: encapsula la lógica de registro, login, verificación por email
 * y reset de contraseña. Ahora genera un JWT tras login exitoso.
 */
@Service
public class AuthService {

    private final UserRepository userRepo;
    private final EmailVerificationCodeRepository codeRepo;
    private final PasswordResetRepository resetRepo;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public AuthService(UserRepository userRepo,
                       EmailVerificationCodeRepository codeRepo,
                       PasswordResetRepository resetRepo,
                       EmailService emailService,
                       JwtUtil jwtUtil,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.codeRepo = codeRepo;
        this.resetRepo = resetRepo;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    // Registro: crea usuario, guarda hash de password y envía código de verificación
    @Transactional
    public String register(String email, String password, String fullName, String phone) throws Exception {
        Optional<User> existing = userRepo.findByEmail(email);
        if (existing.isPresent()) {
            return "Correo ya registrado";
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password)); // hashear contraseña
        user.setFullName(fullName); // guardar nombre completo
        user.setPhone(phone); // guardar teléfono
        user.setRole("CLIENTE"); // todos los registros nuevos son CLIENTE
        user.setVerified(false);
        userRepo.save(user);

        // crear código 6 dígitos con expiración 15 minutos
        String code = String.valueOf((int)(100000 + Math.random() * 900000));
        EmailVerificationCode ev = new EmailVerificationCode();
        ev.setUser(user);
        ev.setCode(code);
        ev.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        codeRepo.save(ev);

        // enviar correo con el código
        String html = buildVerificationEmail(fullName, code);
        emailService.sendHtml(email, "Código de verificación - Hurios Rally", html, true);

        return "Usuario creado. Revisa tu email para verificar.";
    }

    // Login: valida credenciales; si isVerified==false devuelve "not_verified";
    // si todo OK, genera JWT y devuelve mapa con message y token.
    // Ahora también valida que el rol coincida
    public Map<String, Object> login(String email, String password, String expectedRole) {
        Optional<User> op = userRepo.findByEmail(email);
        if (op.isEmpty()) return Map.of("ok", false, "message", "Usuario no encontrado");
        User user = op.get();

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return Map.of("ok", false, "message", "Contraseña incorrecta");
        }

        // Validar que el rol del usuario coincida con el esperado
        if (expectedRole != null && !expectedRole.equals(user.getRole())) {
            String actualRole = user.getRole();
            return Map.of("ok", false, "message", "Su correo es de perfil " + actualRole);
        }

        if (!user.isVerified()) {
            return Map.of("ok", false, "message", "not_verified");
        }

        // Generar JWT con subject = email del usuario
        String subject = user.getEmail();
        String token = jwtUtil.generateToken(subject);

        // devolver token, mensaje OK y rol
        return Map.of("ok", true, "message", "Login exitoso", "token", token, "role", user.getRole());
    }

    // Enviar nuevo código de verificación al email
    public String sendVerificationCode(String email) throws Exception {
        Optional<User> op = userRepo.findByEmail(email);
        if (op.isEmpty()) return "Usuario no encontrado";
        User user = op.get();

        String code = String.valueOf((int)(100000 + Math.random() * 900000));
        EmailVerificationCode ev = new EmailVerificationCode();
        ev.setUser(user);
        ev.setCode(code);
        ev.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        codeRepo.save(ev);

        String html = buildVerificationEmail(user.getFullName(), code);
        emailService.sendHtml(email, "Código de verificación - Hurios Rally", html, true);
        return "Código enviado";
    }

    // Verificar código: marca el usuario como verificado si el código existe y no expiró
    @Transactional
    public String verifyCode(String email, String code) {
        Optional<User> op = userRepo.findByEmail(email);
        if (op.isEmpty()) return "Usuario no encontrado";
        User user = op.get();

        Optional<EmailVerificationCode> rec = codeRepo.findTopByUserAndCodeAndExpiresAtAfterOrderByCreatedAtDesc(user, code, LocalDateTime.now());
        if (rec.isEmpty()) return "Código inválido o expirado";

        user.setVerified(true);
        userRepo.save(user);

        // eliminar códigos antiguos
        codeRepo.deleteByUser(user);
        return "Email verificado";
    }

    // Solicitar reset: crear token (UUID), guardar y enviar link al frontend
    public String requestPasswordReset(String email) throws Exception {
        Optional<User> op = userRepo.findByEmail(email);
        if (op.isEmpty()) {
            // por seguridad devolvemos mensaje genérico
            return "Si el email existe, se envió un correo";
        }
        User user = op.get();
        String token = UUID.randomUUID().toString();
        PasswordReset pr = new PasswordReset();
        pr.setUser(user);
        pr.setToken(token);
        pr.setExpiresAt(LocalDateTime.now().plusHours(1));
        resetRepo.save(pr);

        String link = frontendUrl() + "/new-password?token=" + token + "&email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
        String html = "<p>Haz clic para restablecer tu contraseña: <a href=\"" + link + "\">Restablecer contraseña</a></p>";
        try {
            emailService.sendHtml(email, "Reiniciar contraseña", html);
        } catch (Exception e) {
            // Log del error pero continuar el flujo para desarrollo
            System.err.println("Error sending email: " + e.getMessage());
            System.out.println("Reset link (for development): " + link);
        }
        return "Si el email existe, se envió un correo";
    }

    // Resetear contraseña usando token
    @Transactional
    public String resetPassword(String email, String token, String newPassword) {
        Optional<User> op = userRepo.findByEmail(email);
        if (op.isEmpty()) return "Usuario no encontrado";
        User user = op.get();

        Optional<PasswordReset> rec = resetRepo.findTopByUserAndTokenAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(user, token, LocalDateTime.now());
        if (rec.isEmpty()) return "Token inválido o expirado";

        // actualizar contraseña (hashear)
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // marcar token como usado
        PasswordReset pr = rec.get();
        pr.setUsed(true);
        resetRepo.save(pr);

        return "Contraseña actualizada";
    }

    // helper para construir frontend url (inyectada desde properties)
    private String frontendUrl() {
        // devuelve frontendUrl si existe; evita NPE si no se inyectó (use default)
        if (this.frontendUrl == null || this.frontendUrl.isBlank()) {
            return "http://localhost:5173";
        }
        return this.frontendUrl;
    }

    // Template HTML para email de verificación
    private String buildVerificationEmail(String userName, String code) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<style>" +
            "body { margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #1a1a1a; }" +
            ".container { max-width: 600px; margin: 0 auto; background-color: #1a1a1a; padding: 40px 20px; }" +
            ".content { color: #ffffff; text-align: center; }" +
            "h1 { color: #4a9eff; font-size: 32px; margin-bottom: 20px; }" +
            "p { color: #cccccc; font-size: 16px; line-height: 1.6; margin: 15px 0; }" +
            ".code-box { background-color: transparent; border: 3px dashed #ff4444; border-radius: 10px; padding: 30px; margin: 30px 0; display: inline-block; }" +
            ".code { color: #ff4444; font-size: 48px; font-weight: bold; letter-spacing: 8px; font-family: monospace; }" +
            ".warning { color: #cccccc; font-size: 14px; margin-top: 20px; }" +
            ".logo { margin: 30px 0; }" +
            ".logo img { width: 200px; height: auto; }" +
            ".footer { color: #888888; font-size: 14px; margin-top: 40px; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class='container'>" +
            "<div class='content'>" +
            "<h1>¡Bienvenido, " + userName + "!</h1>" +
            "<p>Gracias por registrarte en nuestra aplicación. Para activar tu cuenta, utiliza el siguiente código OTP:</p>" +
            "<div class='code-box'>" +
            "<div class='code'>" + code + "</div>" +
            "</div>" +
            "<p class='warning'>Este código expira en 15 minutos. Por favor no lo compartas con nadie.</p>" +
            "<div class='logo'>" +
            "<img src='cid:logo' alt='Hurios Rally' />" +
            "</div>" +
            "<div class='footer'>" +
            "<p>© 2025 Hurios Rally. Todos los derechos reservados.</p>" +
            "</div>" +
            "</div>" +
            "</div>" +
            "</body>" +
            "</html>";
    }
}
