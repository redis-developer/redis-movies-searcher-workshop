package io.redis.movies.searcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RedisMoviesSearcher {

    private static final Logger log = LoggerFactory.getLogger(RedisMoviesSearcher.class);

    public static void main(String[] args) {
        SpringApplication.run(RedisMoviesSearcher.class, args);
    }

}
