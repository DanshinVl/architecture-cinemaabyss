package org.cinemaabyss.events.kafka;

import org.cinemaabyss.events.model.EventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    @KafkaListener(topics = "${events.topics.user}", containerFactory = "kafkaListenerContainerFactory")
    public void onUser(EventDto event) {
        log.info("Consumed USER event: {}", event);
    }

    @KafkaListener(topics = "${events.topics.payment}", containerFactory = "kafkaListenerContainerFactory")
    public void onPayment(EventDto event) {
        log.info("Consumed PAYMENT event: {}", event);
    }

    @KafkaListener(topics = "${events.topics.movie}", containerFactory = "kafkaListenerContainerFactory")
    public void onMovie(EventDto event) {
        log.info("Consumed MOVIE event: {}", event);
    }
}
