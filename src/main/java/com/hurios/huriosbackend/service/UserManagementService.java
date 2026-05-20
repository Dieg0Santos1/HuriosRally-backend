package com.hurios.huriosbackend.service;

import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.UserRepository;
import com.hurios.huriosbackend.repository.SaleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UserManagementService - Servicio completo para gestión de usuarios
 * Responsabilidades:
 * - CRUD de usuarios
 * - Gestión de roles y permisos
 * - Activación/Desactivación de cuentas
 * - Búsqueda y filtrado avanzado
 * - Estadísticas de usuarios
 * - Validación y seguridad
 */
@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final SaleRepository saleRepository;
    private final ValidationService validationService;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepository userRepository,
                                SaleRepository saleRepository,
                                ValidationService validationService,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.saleRepository = saleRepository;
        this.validationService = validationService;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== OPERACIONES CRUD ====================

    /**
     * Obtener todos los usuarios
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Obtener usuario por ID
     */
    public Optional<User> getUserById(Long id) {
        validationService.validateId(id);
        return userRepository.findById(id);
    }

    /**
     * Obtener usuario por email
     */
    public Optional<User> getUserByEmail(String email) {
        validationService.validateEmail(email);
        String normalizedEmail = validationService.normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail);
    }

    /**
     * Crear nuevo usuario
     */
    @Transactional
    public User createUser(String email, String password, String fullName, String role) {
        // Validaciones
        validationService.validateEmail(email);
        validationService.validatePassword(password);
        
        String normalizedEmail = validationService.normalizeEmail(email);
        
        // Verificar que el email no exista
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado: " + normalizedEmail);
        }
        
        // Validar rol
        String validRole = validateAndNormalizeRole(role);
        
        // Crear usuario
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName != null ? validationService.sanitizeString(fullName) : null);
        user.setRole(validRole);
        user.setVerified(true); // Por defecto verificado
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    /**
     * Actualizar información de usuario
     */
    @Transactional
    public User updateUser(Long id, String fullName, String phone, String address) {
        validationService.validateId(id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        
        // Actualizar campos
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(validationService.sanitizeString(fullName));
        }
        
        if (phone != null && !phone.trim().isEmpty()) {
            validationService.validatePhone(phone);
            user.setPhone(phone.trim());
        }
        
        if (address != null) {
            user.setAddress(address.trim().isEmpty() ? null : validationService.sanitizeString(address));
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    /**
     * Cambiar contraseña de usuario
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        validationService.validateId(userId);
        validationService.validatePassword(newPassword);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Verificar contraseña anterior
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
        
        // Actualizar contraseña
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }

    /**
     * Restablecer contraseña (admin)
     */
    @Transactional
    public String resetPassword(Long userId) {
        validationService.validateId(userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Generar contraseña temporal
        String tempPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        return tempPassword; // Se debe enviar al usuario por email
    }

    /**
     * Eliminar usuario
     */
    @Transactional
    public void deleteUser(Long id) {
        validationService.validateId(id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Verificar si tiene compras
        long purchaseCount = saleRepository.findAll().stream()
                .filter(sale -> sale.getUser() != null && sale.getUser().getId().equals(id))
                .count();
        
        if (purchaseCount > 0) {
            throw new IllegalStateException(
                "No se puede eliminar el usuario porque tiene " + purchaseCount + " compras registradas"
            );
        }
        
        userRepository.deleteById(id);
    }

    // ==================== GESTIÓN DE ROLES ====================

    /**
     * Cambiar rol de usuario
     */
    @Transactional
    public User changeUserRole(Long userId, String newRole) {
        validationService.validateId(userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        String validRole = validateAndNormalizeRole(newRole);
        user.setRole(validRole);
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    /**
     * Validar y normalizar rol
     */
    private String validateAndNormalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return "CLIENTE"; // Rol por defecto
        }
        
        String upperRole = role.trim().toUpperCase();
        
        if (!upperRole.equals("CLIENTE") && !upperRole.equals("ADMINISTRADOR")) {
            throw new IllegalArgumentException(
                "Rol inválido. Los roles permitidos son: CLIENTE, ADMINISTRADOR"
            );
        }
        
        return upperRole;
    }

    /**
     * Obtener usuarios por rol
     */
    public List<User> getUsersByRole(String role) {
        String validRole = validateAndNormalizeRole(role);
        
        return userRepository.findAll().stream()
                .filter(user -> validRole.equals(user.getRole()))
                .collect(Collectors.toList());
    }

    /**
     * Contar usuarios por rol
     */
    public Map<String, Long> countUsersByRole() {
        List<User> allUsers = userRepository.findAll();
        
        return allUsers.stream()
                .collect(Collectors.groupingBy(
                    user -> user.getRole() != null ? user.getRole() : "SIN_ROL",
                    Collectors.counting()
                ));
    }

    // ==================== VERIFICACIÓN Y ACTIVACIÓN ====================

    /**
     * Verificar cuenta de usuario
     */
    @Transactional
    public void verifyUser(Long userId) {
        validationService.validateId(userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }

    /**
     * Desactivar cuenta de usuario
     */
    @Transactional
    public void deactivateUser(Long userId) {
        validationService.validateId(userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setVerified(false);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }

    /**
     * Verificar si un usuario está activo
     */
    public boolean isUserActive(Long userId) {
        validationService.validateId(userId);
        
        return userRepository.findById(userId)
                .map(User::isVerified)
                .orElse(false);
    }

    // ==================== BÚSQUEDA Y FILTRADO ====================

    /**
     * Buscar usuarios por término de búsqueda
     */
    public List<User> searchUsers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllUsers();
        }
        
        String lowerSearch = searchTerm.toLowerCase().trim();
        
        return userRepository.findAll().stream()
                .filter(user -> 
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerSearch)) ||
                    (user.getFullName() != null && user.getFullName().toLowerCase().contains(lowerSearch)) ||
                    (user.getPhone() != null && user.getPhone().contains(lowerSearch))
                )
                .collect(Collectors.toList());
    }

    /**
     * Filtrar usuarios por rango de fechas de registro
     */
    public List<User> getUsersRegisteredBetween(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser nulas");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
        
        return userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt() != null)
                .filter(user -> !user.getCreatedAt().isBefore(startDate) && 
                               !user.getCreatedAt().isAfter(endDate))
                .collect(Collectors.toList());
    }

    /**
     * Obtener usuarios nuevos (registrados en los últimos N días)
     */
    public List<User> getRecentUsers(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("El número de días debe ser mayor a 0");
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        
        return userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt() != null)
                .filter(user -> user.getCreatedAt().isAfter(cutoffDate))
                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Obtener usuarios inactivos (sin compras)
     */
    public List<User> getInactiveUsers() {
        List<Long> activeUserIds = saleRepository.findAll().stream()
                .filter(sale -> sale.getUser() != null)
                .map(sale -> sale.getUser().getId())
                .distinct()
                .collect(Collectors.toList());
        
        return userRepository.findAll().stream()
                .filter(user -> !activeUserIds.contains(user.getId()))
                .collect(Collectors.toList());
    }