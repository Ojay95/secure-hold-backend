package com.prymo.notificationservice.application.usecase;

import com.prymo.notificationservice.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SendNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendNotificationUseCase.class);

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
                    dispatchTxDisputed(parts[1], parts[2]);
                    break;
                case "EXPIRED":
                    dispatchTxExpired(parts[1], parts[2]);
                    break;
                default:
                    log.warn("Unknown notification event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process event details: {}", e.getMessage());
        }
    }

    private void dispatchTxCreated(String sender, String recipient, String amount) {
        logNotification(Notification.builder()
                .gateway("SMS/WhatsApp ALERT (Twilio Mock)")
                .recipient("+2348031234567 (Sender: " + sender + ")")
                .message("Prymo SecureHold Active: NGN " + amount + " has been locked in escrow for transaction to " + recipient + ". It will automatically release or expire in 24 hours.")
                .build());
    }

    private void dispatchTxCompleted(String sender, String recipient, String amount) {
        logNotification(Notification.builder()
                .gateway("Email Receipt (Mailgun Mock)")
                .recipient(recipient + "@prymo.com")
                .message("Instant Transfer Received: You have received NGN " + amount + " from " + sender + ". Ref: COMPLETED.")
                .build());
    }

    private void dispatchTxReleased(String sender, String recipient, String amount) {
        logNotification(Notification.builder()
                .gateway("SMS/WhatsApp ALERT (Twilio Mock)")
                .recipient("+2348031234567 (Recipient: " + recipient + ")")
                .message("SecureHold Released: NGN " + amount + " has been released by " + sender + " and is now available in your balance!")
                .build());
    }

    private void dispatchTxDisputed(String user, String reason) {
        logNotification(Notification.builder()
                .gateway("Support Ticket Alert (Internal Mock)")
                .recipient("disputes-desk@prymo.com")
                .message("Dispute Filed: User " + user + " has opened a dispute ticket. Reason: " + reason)
                .build());
    }

    private void dispatchTxExpired(String sender, String amount) {
        logNotification(Notification.builder()
                .gateway("SMS/WhatsApp ALERT (Twilio Mock)")
                .recipient("+2348031234567")
                .message("SecureHold Expired: Your held funds of NGN " + amount + " have expired and have been fully refunded back to your available balance.")
                .build());
    }

    private void logNotification(Notification notif) {
        log.info("======================================================================");
        log.info("DISPATCHING VIA GATEWAY: {}", notif.getGateway());
        log.info("RECIPIENT: {}", notif.getRecipient());
        log.info("MESSAGE CONTENT:");
        log.info("   \"{}\"", notif.getMessage());
        log.info("======================================================================");
    }
}
