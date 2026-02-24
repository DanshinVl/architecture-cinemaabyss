package org.cinemaabyss.events.model;

public record EventResponseDto (
        String status,
        int partition,
        long offset,
        EventDto event
) {}
