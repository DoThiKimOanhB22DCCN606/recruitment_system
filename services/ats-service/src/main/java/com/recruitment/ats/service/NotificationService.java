package com.recruitment.ats.service;

import static com.recruitment.ats.api.ApiExceptionHandler.ApiException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recruitment.ats.config.RequestContext;
import com.recruitment.ats.domain.Notification;
import com.recruitment.ats.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Notification> list() {
        return repository.findByRecipientUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
                RequestContext.userId(), Instant.now().minus(90, ChronoUnit.DAYS));
    }

    @Transactional
    public Notification markRead(UUID id) {
        Notification notification = repository.findByIdAndRecipientUserId(id, RequestContext.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ERR_NOTIFICATION_NOT_FOUND",
                        "Notification was not found"));
        notification.setRead(true);
        notification.setReadAt(Instant.now());
        return repository.save(notification);
    }

    @Transactional
    public int markAllRead() {
        return repository.markAllRead(RequestContext.userId(), Instant.now());
    }
}
