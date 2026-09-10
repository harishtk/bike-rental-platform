package com.bikerental.reservation.infrastructure.persistence.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataOutboxEventRepository
    extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findByPublishedAtIsNullOrderByOccurredAtAsc(
            Pageable pageable
    );

    @Modifying
    @Query("""
        UPDATE OutboxEventEntity e
            SET e.publishedAt = :publishedAt
        WHERE e.id = :eventId
    """)
    void markPublished(
            @Param("eventId") UUID eventId,
            @Param("publishedAt") Instant publishedAt
    );
}
