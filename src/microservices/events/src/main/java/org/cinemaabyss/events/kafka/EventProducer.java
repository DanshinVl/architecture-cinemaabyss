package org.cinemaabyss.events.kafka;

import org.cinemaabyss.events.model.EventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class EventProducer {

    private static final Logger log = LoggerFactory.getLogger(EventProducer.class);

    private final KafkaTemplate<String, EventDto> kafkaTemplate;

    @Value("${events.topics.user}") private String userTopic;
    @Value("${events.topics.payment}") private String paymentTopic;
    @Value("${events.topics.movie}") private String movieTopic;

    public EventProducer(KafkaTemplate<String, EventDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public String topicFor(String type) {
        return switch (type) {
            case "user" -> userTopic;
            case "payment" -> paymentTopic;
            case "movie" -> movieTopic;
            default -> movieTopic;
        };
    }

    public SendResult<String, EventDto> sendBlocking(String type, EventDto event) {
        String topic = topicFor(type);
        log.info("Producing event: type={} id={} -> topic={}", type, event.id(), topic);

        try {
            // CompletableFuture in spring-kafka 3.x
            return kafkaTemplate.send(topic, event.id(), event).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send event to Kafka", e);
        }
    }
}
