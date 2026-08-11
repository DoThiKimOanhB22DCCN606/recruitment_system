package com.recruitment.ats.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recruitment.ats.domain.Notification;
import com.recruitment.ats.service.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    List<Notification> list() {
        return service.list();
    }

    @PatchMapping("/{id}/read")
    Notification markRead(@PathVariable UUID id) {
        return service.markRead(id);
    }

    @PatchMapping("/read-all")
    Map<String, Integer> markAllRead() {
        return Map.of("updated", service.markAllRead());
    }
}
