package com.mazadak.inventory_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEvent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String aggregateType;

    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private boolean published = false;

    public OutboxEvent(String aggregateType, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
    }
}
