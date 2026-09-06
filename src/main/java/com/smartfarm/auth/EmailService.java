package com.smartfarm.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService { 

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${RESEND_API_KEY:${resend.api-key:}}")
    private String resendApiKey;

    @Value("${RESEND_FROM:${resend.from:SmartFarm <onboarding@resend.dev>}}")
    private String resendFrom;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        String trimmedTo = to.trim();
        log.info("Preparing password reset email for {}", trimmedTo);

        if (resendApiKey != null && !resendApiKey.trim().isEmpty()) {
            sendViaResend(trimmedTo, resetLink);
            return;
        }

        // Fallback to SMTP if Resend key is not configured
        sendViaSmtp(trimmedTo, resetLink);
    }

    private void sendViaResend(String to, String resetLink) {
        try {
            log.info("Sending email to {} via Resend HTTP API (Port 443 HTTPS)", to);
            
            String fromSender = (resendFrom != null && !resendFrom.trim().isEmpty()) 
                    ? resendFrom.trim() 
                    : "SmartFarm <onboarding@resend.dev>";

            String htmlBody = "<div style=\\\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;\\\">"
                    + "<h2 style=\\\"color: #16a34a;\\\">SmartFarm Password Reset</h2>"
                    + "<p>Hello,</p>"
                    + "<p>You requested to reset your password for your SmartFarm account.</p>"
                    + "<p>Click the button below to set a new password (valid for 15 minutes):</p>"
                    + "<p><a href=\\\"" + resetLink + "\\\" style=\\\"background-color: #16a34a; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;\\\">Reset Password</a></p>"
                    + "<p style=\\\"color: #64748b; font-size: 0.85em; word-break: break-all;\\\">" + resetLink + "</p>"
                    + "<hr style=\\\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\\\" />"
                    + "<p style=\\\"color: #94a3b8; font-size: 0.8em;\\\">If you did not request this, please ignore this email.</p>"
                    + "</div>";

            String jsonPayload = "{"
                    + "\"from\":\"" + escapeJson(fromSender) + "\","
                    + "\"to\":[\"" + escapeJson(to) + "\"],"
                    + "\"subject\":\"SmartFarm Password Reset Request\","
                    + "\"html\":\"" + htmlBody + "\""
                    + "}";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Password reset email delivered to {} via Resend! Response: {}", to, response.body());
            } else {
                log.error("Resend API returned status {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Resend API error (" + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send email via Resend API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email via Resend API: " + e.getMessage(), e);
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void sendViaSmtp(String to, String resetLink) {
        if (mailSender == null) {
            throw new IllegalStateException("Neither RESEND_API_KEY nor JavaMailSender is configured.");
        }
        log.info("Sending password reset email to {} via SMTP", to);
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromEmail != null && !fromEmail.trim().isEmpty()) {
            message.setFrom(fromEmail.trim());
        }
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
        log.info("Password reset email sent successfully to {} via SMTP", to);
    }
}
