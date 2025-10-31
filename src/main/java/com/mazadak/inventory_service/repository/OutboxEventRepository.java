package com.mazadak.inventory_service.repository;

import com.mazadak.inventory_service.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
        SELECT obe from OutboxEvent obe
        WHERE obe.published = false
    """)
    List<OutboxEvent> findByPublishedFalse();
}
