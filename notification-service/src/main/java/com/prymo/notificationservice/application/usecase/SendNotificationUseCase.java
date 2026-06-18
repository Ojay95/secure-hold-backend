package com.prymo.notificationservice.application.usecase;

import com.prymo.notificationservice.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SendNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendNotificationUseCase.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public SendNotificationUseCase(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void processEvent(String event) {
        try {
            String[] parts = event.split(":");
            if (parts.length < 2) return;
            
            String eventType = parts[0];
            
            switch (eventType) {
                case "CREATED":
                    dispatchTxCreated(parts[1], parts[2], parts[3]);
                    break;
                case "COMPLETED":
                    dispatchTxCompleted(parts[1], parts[2], parts[3]);
                    break;
                case "RELEASED":
                    dispatchTxReleased(parts[1], parts[2], parts[3]);
                    break;
                case "DISPUTED":
                    dispatchTxDisputed(parts[2], parts[3]);
                    break;
                case "EXPIRED":
                    dispatchTxExpired(parts[1], parts[2]);
                    break;
                case "REFUNDED":
                    dispatchTxRefunded(parts[1], parts[2]);
                    break;
                default:
                    log.warn("Unknown notification event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process event details: {}", e.getMessage());
        }
    }

    private void dispatchTxCreated(String sender, String recipient, String amount) {
        String msg = "Prymo SecureHold Active: NGN " + amount + " has been locked in escrow for transaction to " + recipient + ". It will automatically release or expire in 24 hours.";
        logNotification(Notification.builder()
                .gateway("SMS/WhatsApp ALERT (Twilio Mock)")
                .recipient("+2348031234567 (Sender: " + sender + ")")
                .message(msg)
                .build());
        
        sendEmail(sender + "@prymo.com", "SecureHold Escrow Hold Created", msg);
    }

    private void dispatchTxCompleted(String sender, String recipient, String amount) {
        String msg = "Instant Transfer Received: You have received NGN " + amount + " from " + sender + ". Ref: COMPLETED.";
        logNotification(Notification.builder()
                .gateway("Email Receipt (Mailgun Mock)")
                .recipient(recipient + "@prymo.com")
                .message(msg)
                .build());
        
        sendEmail(recipient + "@prymo.com", "Instant Transfer Received", msg);
    }

    private void dispatchTxReleased(String sender, String recipient, String amount) {
        String msg = "SecureHold Released: NGN " + amount + " has been released by " + sender + " and is now available in your balance!";
        logNotification(Notification.builder()
                .gateway("SMS/WhatsApp ALERT (Twilio Mock)")
                .recipient("+2348031234567 (Recipient: " + recipient + ")")
                .message(msg)
                .build());
        
        sendEmail(recipient + "@prymo.com", "SecureHold Escrow Funds Released", msg);
    }

    private void dispatchTxDisputed(String user, String reason) {
        String msg = "Dispute Filed: User " + user + " has opened a dispute ticket. Reason: " + reason;
        logNotification(Notification.builder()
                .gateway("Support Ticket Alert (Internal Mock)")
                .recipient("disputes-desk@prymo.com")
                .message(msg)
                .build());
        
        sendEmail("disputes-desk@prymo.com", "New Dispute Filed - Action Required", msg);
    }

    private void dispatchTxExpired(String sender, String amount) {
        String msg = "SecureHold Expired: Your held funds of NGN " + amount + " have expired and have been fully refunded back to your available balance.";
        logNotification(Notification.builder()
                .gateway("SMS/WhatsApp ALERT (Twilio Mock)")
                .recipient("+2348031234567")
                .message(msg)
                .build());
        
        sendEmail(sender + "@prymo.com", "SecureHold Escrow Expired", msg);
    }

    private void dispatchTxRefunded(String sender, String amount) {
        String msg = "SecureHold Refunded: Your disputed funds of NGN " + amount + " have been refunded back to your available balance.";
        logNotification(Notification.builder()
                .gateway("SMS/WhatsApp ALERT (Twilio Mock)")
                .recipient("+2348031234567 (Sender: " + sender + ")")
                .message(msg)
                .build());
        
        sendEmail(sender + "@prymo.com", "SecureHold Escrow Refunded", msg);
    }

    private void logNotification(Notification notif) {
        log.info("======================================================================");
        log.info("DISPATCHING VIA GATEWAY: {}", notif.getGateway());
        log.info("RECIPIENT: {}", notif.getRecipient());
        log.info("MESSAGE CONTENT:");
        log.info("   \"{}\"", notif.getMessage());
        log.info("======================================================================");
    }

    private void sendEmail(String toEmailAddress, String subject, String messageText) {
        if (smtpHost == null || smtpHost.isBlank()) {
            log.info("SMTP mail host not configured. Skipping real email dispatch.");
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(toEmailAddress);
            mailMessage.setSubject(subject);
            mailMessage.setText(messageText);

            String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "noreply@prymo.com";
            mailMessage.setFrom(sender);

            log.info("Sending real Email via SMTP to: {}", toEmailAddress);
            mailSender.send(mailMessage);
            log.info("Email dispatched successfully.");
        } catch (Exception e) {
            log.error("Failed to send Email via SMTP: {}", e.getMessage());
        }
    }
}
