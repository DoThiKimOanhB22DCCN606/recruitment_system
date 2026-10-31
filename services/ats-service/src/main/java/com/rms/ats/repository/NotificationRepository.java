package com.rms.ats.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.rms.ats.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientUserIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID recipientUserId, Instant after);
    long countByRecipientUserIdAndReadFalseAndCreatedAtAfter(UUID recipientUserId, Instant after);
    Optional<Notification> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);
    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = :readAt "
            + "where n.recipientUserId = :recipientUserId and n.read = false")
    int markAllRead(UUID recipientUserId, Instant readAt);
}
