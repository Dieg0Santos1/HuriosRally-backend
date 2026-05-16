package com.hurios.huriosbackend.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;

/**
 * EmailService - Servicio completo para envío de emails
 * Responsabilidades:
 * - Enviar correos HTML y texto plano
 * - Notificaciones de pedidos
 * - Alertas de stock
 * - Emails de recuperación de contraseña
 * - Modo mock para desarrollo
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final ValidationService validationService;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.email.mock:false}")
    private boolean mockEmail;

    public EmailService(JavaMailSender mailSender, ValidationService validationService) {
        this.mailSender = mailSender;
        this.validationService = validationService;
    }

    // Método para enviar HTML con fallback en caso de error
    public void sendHtml(String to, String subject, String html) throws Exception {
        sendHtml(to, subject, html, false);
    }

    // Método para enviar HTML con o sin logo incrustado
    public void sendHtml(String to, String subject, String html, boolean includeLogo) throws Exception {
        if (mockEmail) {
            // Modo desarrollo - simular envío
            logger.info("\n" + "=".repeat(50));
            logger.info("[MOCK EMAIL] To: {}", to);
            logger.info("[MOCK EMAIL] Subject: {}", subject);
            logger.info("[MOCK EMAIL] Content: {}", html);
            logger.info("=".repeat(50) + "\n");
            return;
        }

        try {
            logger.info("Attempting to send email to: {}", to);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // true => HTML
            
            // Adjuntar logo si se solicita
            if (includeLogo) {
                try {
                    // Intentar cargar el logo desde el proyecto frontend
                    File logoFile = new File("../huriosfrontend/public/assets/imgs/logo.webp");
                    if (logoFile.exists()) {
                        FileSystemResource res = new FileSystemResource(logoFile);
                        helper.addInline("logo", res);
                        logger.info("✓ Logo attached to email");
                    } else {
                        logger.warn("⚠ Logo file not found at: {}", logoFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    logger.warn("⚠ Could not attach logo: {}", e.getMessage());
                }
            }
            
            mailSender.send(message);
            logger.info("✓ Email sent successfully to: {}", to);
            
        } catch (Exception e) {
            logger.error("❌ Failed to send email to {}: {}", to, e.getMessage());
            logger.error("Full error: ", e);
            
            // En desarrollo, mostrar el contenido del email
            logger.info("\n" + "=".repeat(50));
            logger.info("[EMAIL FALLBACK] To: {}", to);
            logger.info("[EMAIL FALLBACK] Subject: {}", subject);
            logger.info("[EMAIL FALLBACK] Content: {}", html);
            logger.info("=".repeat(50) + "\n");
            
            // Re-throw para que el AuthService pueda manejar el error
            throw new Exception("Error al enviar email: " + e.getMessage());
        }
    }

    /**
     * Enviar email de confirmación de compra
     */
    public void sendPurchaseConfirmation(String to, Long orderId, Double total) throws Exception {
        validationService.validateEmail(to);
        validationService.validateId(orderId);
        validationService.validatePrice(total);

        String subject = "Confirmación de Compra - Orden #" + orderId;
        String html = String.format(
            "<h2>Estimado cliente,</h2>" +
            "<p>Tu compra ha sido confirmada.</p>" +
            "<p><strong>Número de orden:</strong> #%d</p>" +
            "<p><strong>Total:</strong> S/ %.2f</p>" +
            "<p>Gracias por tu compra.</p>" +
            "<p>Saludos,<br>Equipo Hurios Rally</p>",
            orderId, total
        );
        
        sendHtml(to, subject, html);
    }

    /**
     * Enviar email de cambio de estado de orden
     */
    public void sendOrderStatusUpdate(String to, Long orderId, String newStatus) throws Exception {
        validationService.validateEmail(to);
        validationService.validateId(orderId);
        
        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("El nuevo estado no puede estar vacío");
        }

        String subject = "Actualización de Estado - Orden #" + orderId;
        String html = String.format(
            "<h2>Estimado cliente,</h2>" +
            "<p>El estado de tu orden #%d ha cambiado.</p>" +
            "<p><strong>Nuevo estado:</strong> %s</p>" +
            "<p>Saludos,<br>Equipo Hurios Rally</p>",
            orderId, newStatus
        );
        
        sendHtml(to, subject, html);
    }

    /**
     * Enviar alerta de stock bajo (para administradores)
     */
    public void sendLowStockAlert(String to, String productName, Integer currentStock) throws Exception {
        validationService.validateEmail(to);
        
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de producto inválido");
        }
        
        if (currentStock == null || currentStock < 0) {
            throw new IllegalArgumentException("Stock inválido");
        }

        String subject = "Alerta: Stock Bajo - " + productName;
        String html = String.format(
            "<h2>Alerta de Stock</h2>" +
            "<p>El producto <strong>'%s'</strong> tiene stock bajo.</p>" +
            "<p><strong>Stock actual:</strong> %d unidades</p>" +
            "<p>Se recomienda realizar un nuevo pedido.</p>" +
            "<p>Sistema Hurios Rally</p>",
            productName, currentStock
        );
        
        sendHtml(to, subject, html);
    }

    /**
     * Enviar email de producto sin stock
     */
    public void sendOutOfStockAlert(String to, String productName) throws Exception {
        validationService.validateEmail(to);
        
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de producto inválido");
        }

        String subject = "Alerta: Producto Sin Stock - " + productName;
        String html = String.format(
            "<h2>Alerta de Stock</h2>" +
            "<p>El producto <strong>'%s'</strong> se ha quedado sin stock.</p>" +
            "<p>Se requiere reabastecimiento urgente.</p>" +
            "<p>Sistema Hurios Rally</p>",
            productName
        );
        
        sendHtml(to, subject, html);
    }

    /**
     * Validar formato de email sin enviar
     */
    public boolean isValidEmail(String email) {
        try {
            validationService.validateEmail(email);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Verificar si está en modo mock
     */
    public boolean isMockMode() {
        return mockEmail;
    }
}
