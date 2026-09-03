package com.smartfarm.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService { 

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:smartfarm.alerts@gmail.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("SmartFarm Password Reset Request");
        message.setText("Hello,\n\n" +
                "You have requested to reset your password for your SmartFarm account.\n" +
                "Please click on the link below to set a new password:\n\n" +
                resetLink + "\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Regards,\n" +
                "SmartFarm Admin");
        mailSender.send(message);
    }
}
