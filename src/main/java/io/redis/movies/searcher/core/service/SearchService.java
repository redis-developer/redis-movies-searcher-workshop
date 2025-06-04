package io.redis.movies.searcher.core.service;

import ai.djl.util.Pair;
import io.redis.movies.searcher.core.domain.*;
import io.redis.movies.searcher.core.dto.MovieDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    public Pair<List<MovieDTO>, ResultType> searchMovies(String query, Integer limit) {
        logger.info("Received query: {}", query);
        logger.info("-------------------------");
        final int resultLimit = (limit == null) ? 3 : limit;

        List<Movie> movies = List.of(new Movie(
                1, "Welcome to the workshop", 2025,
                "Building Context-Aware Applications with Java, Vector Searches, and Redis",
                "2025-05-28", 10.0, List.of("Ricardo Ferreira")));

        return new Pair<>(convertToDTOs(movies), ResultType.NONE);
    }

    private List<MovieDTO> convertToDTOs(List<Movie> movies) {
        return movies.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private MovieDTO convertToDTO(Movie movie) {
        return new MovieDTO(
                movie.getTitle(),
                movie.getYear(),
                movie.getPlot(),
                movie.getRating(),
                movie.getActors().toArray(new String[0])
        );
    }

}
