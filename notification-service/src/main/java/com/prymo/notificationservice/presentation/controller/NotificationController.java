package com.prymo.notificationservice.presentation.controller;

import com.prymo.notificationservice.application.usecase.SendNotificationUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final SendNotificationUseCase useCase;

    public NotificationController(SendNotificationUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/trigger-mock")
    public ResponseEntity<?> triggerMockNotification(@RequestBody Map<String, String> body) {
        String event = body.get("event");
        if (event == null || event.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Event body is required"));
        }
        useCase.processEvent(event);
        return ResponseEntity.ok(Map.of("message", "Mock notification dispatched locally"));
    }
}
