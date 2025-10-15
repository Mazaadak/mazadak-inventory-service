package com.mazadak.inventory_service.scheduler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduler.enabled", matchIfMissing = true)
@Slf4j
public class SchedulingConfiguration {
}
