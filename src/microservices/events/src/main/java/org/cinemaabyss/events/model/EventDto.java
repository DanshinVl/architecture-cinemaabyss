package org.cinemaabyss.events.model;

import java.time.OffsetDateTime;
import java.util.Map;

public record EventDto (
        String id,
        String type,
        OffsetDateTime timestamp,
        Map<String, Object> payload
) {}
