package com.example.claims.web;

import com.example.claims.notify.Notification;
import com.example.claims.notify.NotificationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NotificationController {

    private final NotificationRepository notifications;

    public NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/api/notifications")
    public List<Notification> mine(@RequestHeader("X-User-Id") String userId) {
        return notifications.findByRecipientIdOrderByCreatedAtDesc(userId);
    }
}
