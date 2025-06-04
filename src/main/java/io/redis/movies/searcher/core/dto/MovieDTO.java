package io.redis.movies.searcher.core.dto;

public record MovieDTO(
        String title,
        int year,
        String plot,
        double rating,
        String[] actors
) {}
