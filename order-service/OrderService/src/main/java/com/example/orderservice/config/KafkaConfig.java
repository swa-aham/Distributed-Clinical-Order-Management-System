package com.example.orderservice.config;

import com.example.orderservice.event.ClinicalOrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // enable Java 8 date/time serialization
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public ProducerFactory<String, ClinicalOrderCreatedEvent> producerFactory(ObjectMapper objectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Use Spring Kafka's new JsonSerializer replacement
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JsonSerializer.class); // still works, warnings are suppressed if you use this approach
        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(),
                new org.springframework.kafka.support.serializer.JsonSerializer<>(objectMapper));
    }

    @Bean
    public KafkaTemplate<String, ClinicalOrderCreatedEvent> kafkaTemplate(
            ProducerFactory<String, ClinicalOrderCreatedEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}