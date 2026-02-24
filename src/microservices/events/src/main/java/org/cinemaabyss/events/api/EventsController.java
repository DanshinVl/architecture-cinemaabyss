package org.cinemaabyss.events.api;


import org.cinemaabyss.events.kafka.EventProducer;
import org.cinemaabyss.events.model.EventDto;
import org.cinemaabyss.events.model.EventResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventsController {

    private final EventProducer producer;

    public EventsController(EventProducer producer) {
        this.producer = producer;
    }

    @PostMapping("/movie")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDto createMovieEvent(@RequestBody Map<String, Object> payload) {
        return publish("movie", payload, null);
    }

    @PostMapping("/user")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDto createUserEvent(@RequestBody Map<String, Object> payload) {
        // timestamp обязателен по OpenAPI — но даже если не пришёл, поставим текущий
        return publish("user", payload, getTimestampFromPayload(payload));
    }

    @PostMapping("/payment")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDto createPaymentEvent(@RequestBody Map<String, Object> payload) {
        return publish("payment", payload, getTimestampFromPayload(payload));
    }

    private EventResponseDto publish(String type, Map<String, Object> payload, OffsetDateTime forcedTs) {

        OffsetDateTime ts = forcedTs != null ? forcedTs : OffsetDateTime.now();

        String id = buildId(type, payload);
        EventDto event = new EventDto(id, type, ts, payload);

        SendResult<String, EventDto> result = producer.sendBlocking(type, event);

        int partition = result.getRecordMetadata().partition();
        long offset = result.getRecordMetadata().offset();

        return new EventResponseDto("success", partition, offset, event);
    }

    private String buildId(String type, Map<String, Object> payload) {
        // ближе к примеру "movie-1-viewed"
        Object action = payload.get("action");
        Object movieId = payload.get("movie_id");
        Object userId = payload.get("user_id");
        Object paymentId = payload.get("payment_id");

        String a = action != null ? action.toString() : "event";

        return switch (type) {
            case "movie" -> "movie-" + (movieId != null ? movieId : "x") + "-" + a;
            case "user" -> "user-" + (userId != null ? userId : "x") + "-" + a;
            case "payment" -> "payment-" + (paymentId != null ? paymentId : "x") + "-" + a;
            default -> type + "-x-" + a;
        };
    }

    private OffsetDateTime getTimestampFromPayload(Map<String, Object> payload) {
        Object ts = payload.get("timestamp");
        if (ts == null) return OffsetDateTime.now();
        try {
            return OffsetDateTime.parse(ts.toString());
        } catch (Exception ignored) {
            return OffsetDateTime.now();
        }
    }
}
