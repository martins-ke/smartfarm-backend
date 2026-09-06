package com.smartfarm.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService { 

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        log.info("Sending password reset email to {} (From: {})", to, fromEmail);
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromEmail != null && !fromEmail.trim().isEmpty()) {
            message.setFrom(fromEmail.trim());
        }
        message.setTo(to.trim());
        message.setSubject("SmartFarm Password Reset Request");
        message.setText("Hello,\n\n" +
                "You have requested to reset your password for your SmartFarm account.\n" +
                "Please click on the link below to set a new password:\n\n" +
                resetLink + "\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Regards,\n" +
                "SmartFarm Admin");
        mailSender.send(message);
        log.info("Password reset email sent successfully to {}", to);
    }
}
