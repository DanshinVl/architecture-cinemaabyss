package org.cinemaabyss.events.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class EventsHealthController {

    @GetMapping("/api/events/health")
    public Map<String, Boolean> health() {
        return Map.of("status", true);
    }
}
