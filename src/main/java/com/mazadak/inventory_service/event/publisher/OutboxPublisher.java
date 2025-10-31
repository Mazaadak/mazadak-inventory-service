package com.mazadak.inventory_service.event.publisher;

import com.mazadak.inventory_service.constant.InventoryMessagingConstants;
import com.mazadak.inventory_service.model.OutboxEvent;
import com.mazadak.inventory_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final StreamBridge streamBridge;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByPublishedFalse();

        for (var event : events) {
            var bindingName = resolveBindingForEventType(event.getEventType());

            if (bindingName == null) {
                log.warn("No binding found for event type {}", event.getEventType());
                continue;
            }

            try {
                log.info("Sending outbox event: {}", event);
                streamBridge.send(bindingName, event.getPayload());
                event.setPublished(true);
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Couldn't publish event {}.", event, e);
            }
        }
    }

    private String resolveBindingForEventType(String eventType) {
        return switch (eventType) {
            case "InventoryDeleted" -> InventoryMessagingConstants.INVENTORY_DELETED_BINDING;
            default -> null;
        };
    }

}
