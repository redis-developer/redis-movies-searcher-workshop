# Building Context-Aware Applications with Java, Vector Searches, and Redis

![redis-logo.png](images/redis-logo.png)

In this workshop, you will learn how to design and develop an application that makes use of vector databases to handle semantic search. You will learn how to prepare the dataset for storage, how to process JSON documents and generate embeddings, how to index them into Redis, and how to implement searches and aggregations on top of it. You must be comfortable with [Java](https://www.java.com/en) and have a basic understanding of [Redis](https://redis.io/open-source).

## Part 1: Getting everything ready

The goal of this section is to get the moving parts from this project ready to be used. You will take care of the steps necessary to get the application running. This means you will make sure the application dependencies are ready, build the code from scratch, and make sure the dataset the application will handle is in a good shape.

### Task 1: Installing dependencies

#### ⏰ Estimated time: **15 minutes**

This workshop requires the following dependencies to be installed on your machine:

- Mandatory
  - [JDK 21+](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html)
  - [Docker](https://www.docker.com/get-started)
  - [RIOT](https://redis.github.io/riot/#_install)
  - [Redis Insight](https://redis.io/docs/latest/operate/redisinsight/install/install-on-desktop/) (If you're a Mac user, you can install it using [Homebrew](https://formulae.brew.sh/cask/redis-insight))
- Recommended
  - [Maven 3.9+](https://maven.apache.org/download.cgi)

Please use IDEs like [VS Code](https://code.visualstudio.com), [IntelliJ IDEA](https://www.jetbrains.com/idea/) or [Eclipse](https://www.eclipse.org/downloads/packages/) to work with the code. As a Java developer, you can use any IDE you feel comfortable with. Please make sure to have one installed before proceeding.

### Task 2: Building and running the application

#### ⏰ Estimated time: **20 minutes**

Your first step is cloning the repository and building the application. Make sure to clone the repository with the branch `jnation-2025-workshop`:

```bash
git clone https://github.com/redis-developer/redis-movies-searcher.git -b jnation-2025-workshop
```

Once you have a copy of the repository locally, you can open it in your favorite IDE. The project is a standard Maven project, so you can import it as a Maven project. Go ahead and run a Maven build so the dependencies are downloaded and you can verify if everything is working as expected. Please note that depending of the internet connection, this may take a while to complete.

```bash
mvn clean package
```

Before running the application, you need to start the application dependencies. This include the frontend layer, the Redis database, and a browser version of Redis Insight. You can do this by running the following command:

```bash
docker compose up -d
```

Now execute the application with the following command:

```bash
mvn spring-boot:run
```

The application will start on port `8080`. You can access it by opening your browser and navigating to:

http://localhost:8080/redis-movies-searcher

You should see the following screen:

![app-mvp.png](images/app-mvp.png)

You can type anything you want in the search box. You should see the same result coming back. This is the MVP of the application, and the result is currently hard-coded.

Now open Redis Insight and connect to the Redis instance running on your machine. You can do this by clicking on the **Add Redis Database** button and entering the following information: `redis://default@127.0.0.1:6379`

![ri-start.png](images/ri-start.png)

If you don't want to use the desktop version of Redis Insight, you can access the one running on Docker. You can access it by opening your browser and navigating to:

http://localhost:5540

This browser version of Redis Insight is limited in capabilities. I would highly recommend you to use the desktop version of Redis Insight.

### Task 3: Importing the dataset into Redis

#### ⏰ Estimated time: **5 minutes**

The dataset you will be using is a collection of movies. It is a JSON file that contains the information we need about the movies, such as title, year, plot, release date, rating, and actors. The dataset is located in the `data` folder of the project.

```bash
cd ${project_dir}/data
```

To import the dataset into Redis, you can use the [RIOT](https://redis.github.io/riot/) command line tool. This tool is used to import and export data from Redis. You can use the following command to import the dataset into Redis:

```bash
riot file-import \
    --var counter="new java.lang.Integer(1)" \
    --proc id="#counter++" \
    --proc plot="info.plot" \
    --proc releaseDate="info.release_date" \
    --proc rating="info.rating" \
    --proc actors="remove('info').actors" \
    movies.json json.set --keyspace import:movie --key id
```

There are lots of important information going on with this simple command. Your instructor will explain the details of this command during the workshop. For now, just run it and make sure it works as expected. If you open Redis Insight right now, you should be able to see the movies stored at Redis.

![ri-ds-loaded.png](images/ri-ds-loaded.png)

### Task 4: Finding and removing duplicated movies

#### ⏰ Estimated time: **10 minutes**

The dataset you just imported into Redis contains some duplicated movies. You need to remove the duplicates from the dataset. You can do this easily by creating an index on the dataset and then use [Redis Query Engine](https://redis.io/docs/latest/develop/interact/search-and-query) commands to find the duplicates. The command below creates an index called `imported_movies_index` that will manage any keys that start with the prefix `import:movie:`.

```bash
FT.CREATE imported_movies_index ON JSON PREFIX 1 "import:movie:" SCHEMA
  $.title AS title TEXT WEIGHT 1
  $.year AS year NUMERIC SORTABLE UNF
  $.plot AS plot TEXT WEIGHT 1
  $.releaseDate AS releaseDate TAG SEPARATOR "|"
  $.rating AS rating NUMERIC SORTABLE UNF
  $.actors[*] AS actors TAG SEPARATOR "|"
  $.id AS id NUMERIC SORTABLE UNF
```

To create this index, go to Redis Insight, and access the **Workbench** tab. Then, paste the command into the query editor and click on the **Run** button. You should see the index being created.

![ri-create-idx.png](images/ri-create-idx.png)

At any time, you can verify the details of the index by running the command below. This will show you the details of the index, including the fields and their types.

```bash
FT.INFO 'imported_movies_index'
```

![ri-inspect-idx.png](images/ri-inspect-idx.png)

One cool aspect of working with indexed datasets with Redis Insight is the ability to search for keys using the UI. Go back to the **Browser** tab and select the option **Search by Values of Keys**. Then select the index you created in the drop down box near it. In the search box, try to search for all movies that contain the word `Matrix` for example.

![ri-search-matrix.png](images/ri-search-matrix.png)

Now that you know the index is working as expected, you can find the duplicates. Go to the Workbench tab and run the following command:

```bash
FT.AGGREGATE imported_movies_index "*"
  LOAD 2 @title @id
  GROUPBY 1 @title
  REDUCE COUNT 0 AS count
  REDUCE TOLIST 1 @id AS ids
  FILTER "@count > 1"
```

This is an aggregation command that will group the movies by title and count the number of movies with the same title. It will also return the IDs of the movies with the same title. You should see a result similar to this:

![ri-dupl-movies.png](images/ri-dupl-movies.png)

To remove the duplicates efficiently, you are going to leverage another powerful feature of Redis: [Lua scripting](https://redis.io/docs/latest/develop/interact/programmability/eval-intro/). You can use the following Lua script to remove the duplicates from the dataset. This script will iterate over the results of the aggregation command use before and delete the duplicates from Redis.

```bash
EVAL "local result = redis.call('FT.AGGREGATE', 'imported_movies_index', '*', 'LOAD', '2', '@title', '@id', 'GROUPBY', '1', '@title', 'REDUCE', 'COUNT', '0', 'AS', 'count', 'REDUCE', 'TOLIST', '1', '@id', 'AS', 'ids', 'FILTER', '@count > 1') local deletion_count = 0 local index = 2 while index <= #result do local group = result[index] local title_idx, ids_idx = nil, nil for i = 1, #group, 2 do if group[i] == 'title' then title_idx = i elseif group[i] == 'ids' then ids_idx = i end end if ids_idx then local ids_list = group[ids_idx + 1] for i = 2, #ids_list do local key_name = 'import:movie:' .. ids_list[i] redis.call('DEL', key_name) deletion_count = deletion_count + 1 end end index = index + 1 end return 'Deleted ' .. deletion_count .. ' duplicate movies'" 0
```

![ri-remove-dupl.png](images/ri-remove-dupl.png)

### Task 5: Fixing data glitches in the dataset

#### ⏰ Estimated time: **10 minutes**

The dataset you imported may have a few data glitches to fix. For example, some movies may have a `null` value for the `actors` field. One good example is the movie `Justice League`. To fix it, the first thing you need is to find which key this movie belong to. You can search for movies with this title, using the following query:

```bash
FT.SEARCH imported_movies_index "@title:\"Justice League\""
```

You may see more than one movie with this title. Select the one from `2017`. To include the actors into the movie, you can use the following command:

```bash
JSON.SET import:movie:1321 $.actors '["Ben Affleck", "Gal Gadot", "Jason Momoa", "Henry Cavill", "Ezra Miller"]'
```

Like this glitch, there may be others. Please spend some time finding fields with no values, or perhaps with wrong values. Your instructor will likely explain how to do this efficiently using [Redis Copilot](https://redis.io/docs/latest/develop/tools/insight/copilot-faq/), which is a feature from Redis Insight.

![ri-copilot.png](images/ri-copilot.png)

## Part 2: Preparing the dataset for searches

In this section, you will implement a functionality that will prepare the dataset for searches. You will write code in Java to process the existing dataset in such a way that creates the building blocks required for the searches, such as schemas, indexes, and embeddings.

### Task 1: Persistence layer for the existing data

#### ⏰ Estimated time: **10 minutes**

In the previous section, you imported the dataset into Redis. The dataset is stored in Redis as JSON documents. In this section, you will create a persistence layer for the existing data. This will allow you to access data from Java and perform operations on it.

You will start by creating a domain entity that will represent the current data in Redis. This class will leverage [Spring Data](https://spring.io/projects/spring-data) and [Redis OM for Java](https://redis.io/docs/latest/integrate/redisom-for-java/). Your instructor will explain what each annotation does. Create a new Java class called `MovieData` in the package `io.redis.movies.searcher.data.domain`. Note how you are mapping the domain entity to the existing index created earlier.

```java
package io.redis.movies.searcher.data.domain;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.Searchable;
import org.springframework.data.annotation.Id;

import java.util.List;

@Document(value = "import:movie", indexName = "imported_movies_index")
public class MovieData {

    @Id
    private int id;

    @Searchable
    private String title;

    @Indexed(sortable = true)
    private int year;

    @Searchable
    private String plot;

    @Indexed
    private String releaseDate;

    @Indexed(sortable = true)
    private double rating;

    @Indexed
    private List<String> actors;
    
    // Create getters and setters for all fields

}
```

Along with the domain entity, you will need a repository implementation to perform data operations. Create a new Java interface called `MovieDataRepository` in the package `io.redis.movies.searcher.data.repository`.

```java
package io.redis.movies.searcher.data.repository;

import com.redis.om.spring.repository.RedisDocumentRepository;
import io.redis.movies.searcher.data.domain.MovieData;

public interface MovieDataRepository extends RedisDocumentRepository<MovieData, Integer> {
}
```

### Task 2: Persistence layer for the generated data

#### ⏰ Estimated time: **10 minutes**

You will create a persistence layer for the generated data. This will allow you to access data from Java and perform operations on it. You will create a new domain entity that will represent the generated data in Redis. This class will leverage [Spring Data](https://spring.io/projects/spring-data) and [Redis OM for Java](https://redis.io/docs/latest/integrate/redisom-for-java/). Your instructor will explain what each annotation does. Create a new Java class called `Movie` in the package `io.redis.movies.searcher.core.domain`.

Note are mapping this domain entity to a index that doesn't exist yet. This index will be created when you run the application for the first time. Also, this implementation holds the logic about how embeddings will be created. Your instructor will explain how this works, and how you can customize this.

```java
package io.redis.movies.searcher.core.domain;

import com.redis.om.spring.annotations.*;
import com.redis.om.spring.indexing.DistanceMetric;
import com.redis.om.spring.indexing.VectorType;
import io.redis.movies.searcher.data.domain.MovieData;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import redis.clients.jedis.search.schemafields.VectorField;

import java.util.List;

@RedisHash("movie")
public class Movie {

    @Id
    private int id;

    @Searchable
    private String title;

    @Indexed(sortable = true)
    private int year;

    @Vectorize(
            destination = "plotEmbedding",
            embeddingType = EmbeddingType.SENTENCE
    )
    private String plot;

    @Indexed(
            schemaFieldType = SchemaFieldType.VECTOR,
            algorithm = VectorField.VectorAlgorithm.FLAT,
            type = VectorType.FLOAT32,
            dimension = 384,
            distanceMetric = DistanceMetric.COSINE,
            initialCapacity = 10
    )
    private byte[] plotEmbedding;

    @Indexed
    private String releaseDate;

    @Indexed(sortable = true)
    private double rating;

    @Indexed
    private List<String> actors;

    public Movie() {}

    public Movie(int id, String title, int year, String plot,
                 String releaseDate, double rating, List<String> actors) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.plot = plot;
        this.releaseDate = releaseDate;
        this.rating = rating;
        this.actors = actors;
    }

    public static Movie fromData(MovieData movieAsJSON) {
        return new Movie(movieAsJSON.getId(), movieAsJSON.getTitle(),
                movieAsJSON.getYear(), movieAsJSON.getPlot(),
                movieAsJSON.getReleaseDate(), movieAsJSON.getRating(),
                movieAsJSON.getActors());
    }

    // Create getters and setters for all fields

}
```

Along with the domain entity, you will need a repository implementation to perform data operations. Create a new Java interface called `MovieRepository` in the package `io.redis.movies.searcher.core.repository`.

```java
package io.redis.movies.searcher.core.repository;

import com.redis.om.spring.repository.RedisEnhancedRepository;
import io.redis.movies.searcher.core.domain.Movie;

public interface MovieRepository extends RedisEnhancedRepository<Movie, Integer> {
}
```

You need to enable the two repositories created with Spring Boot. In order to do this, you will need to use the annotations `@EnableRedisDocumentRepositories` and `@EnableRedisEnhancedRepositories` in the main class of the app. Edit the Java class `io.redis.movies.searcher.RedisMoviesSearcher.java` and add these annotations.

```java
package io.redis.movies.searcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories;

@SpringBootApplication
@EnableRedisDocumentRepositories(basePackages = {"io.redis.movies.searcher.data*"})
@EnableRedisEnhancedRepositories(basePackages = {"io.redis.movies.searcher.core*"})
public class RedisMoviesSearcher {

    private static final Logger log = LoggerFactory.getLogger(RedisMoviesSearcher.class);

    public static void main(String[] args) {
        SpringApplication.run(RedisMoviesSearcher.class, args);
    }

}
```

### Task 3: Implementing the dataset processing

#### ⏰ Estimated time: **40 minutes**

Now that you have the persistence layers properly implemented, you can start the implementation of the dataset processing. You are going to write code now to load all the movies currently stored at Redis as JSON documents in-memory. Then, you are going to save them with a new domain entity layer to generated the movies as Hashes and with their respective embeddings. Your instructor will explain the differences between [JSON](https://redis.io/docs/latest/develop/data-types/json/) and [Hashes](https://redis.io/docs/latest/develop/data-types/hashes/) data types at Redis.

Create a new Java class called `MovieService` in the package `io.redis.movies.searcher.core.service`. This class will be responsible for processing the movies.

```java
package io.redis.movies.searcher.core.service;

import io.redis.movies.searcher.core.domain.Movie;
import io.redis.movies.searcher.core.repository.MovieRepository;
import io.redis.movies.searcher.data.repository.MovieDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieService.class);

    private final MovieRepository movieRepository;
    private final MovieDataRepository movieDataRepository;
    private final RedisTemplate redisTemplate;

    public MovieService(MovieRepository movieRepository,
                        MovieDataRepository movieDataRepository,
                        RedisTemplate redisTemplate) {
        this.movieRepository = movieRepository;
        this.movieDataRepository = movieDataRepository;
        this.redisTemplate = redisTemplate;
    }

    public void importMovies() {
        log.info("Starting processing the movies available at Redis...");

        Set<String> allMovieKeys = new HashSet<>();
        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection()
                .scan(ScanOptions.scanOptions().match("import:movie:*").count(1000).build())) {
            while (cursor.hasNext()) {
                allMovieKeys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        }

        log.info("Found {} records with the key prefix 'import:movie'", allMovieKeys.size());
        if (allMovieKeys.isEmpty()) {
            return;
        }

        var startTime = Instant.now();
        List<Movie> movies = allMovieKeys.parallelStream()
                .map(key -> {
                    try {
                        Integer id = Integer.parseInt(key.split(":")[2]);
                        var movieData = movieDataRepository.findById(id).orElse(null);
                        if (movieData != null && !movieRepository.existsById(movieData.getId())) {
                            return Movie.fromData(movieData);
                        }
                    } catch (Exception e) {
                        log.warn("Error processing key {}: {}", key, e.getMessage());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!movies.isEmpty()) {
            log.info("Loaded {} records into memory. Saving them all...", movies.size());
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                final int batchSize = 500;
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                AtomicInteger savedCounter = new AtomicInteger(0);

                for (int i = 0; i < movies.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, movies.size());
                    List<Movie> batch = movies.subList(i, end);

                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            movieRepository.saveAll(batch);
                            int totalSaved = savedCounter.addAndGet(batch.size());
                            if (totalSaved % 500 == 0 || totalSaved == movies.size()) {
                                double percentComplete = (totalSaved * 100.0) / movies.size();
                                log.info("Saved {}/{} movies ({}%)",
                                        totalSaved, movies.size(),
                                        String.format("%.1f", percentComplete));
                            }
                        } catch (Exception ex) {
                            log.error("Error saving batch: {}", ex.getMessage(), ex);
                        }
                    }, executor);

                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }
        }

        var duration = Duration.between(startTime, Instant.now());
        double seconds = duration.toMillis() / 1000.0;
        log.info("Processing complete: {} source keys loaded, saved {} records in {} seconds",
                allMovieKeys.size(),
                movies.size(),
                String.format("%.2f", seconds));
    }

    public boolean isDataLoaded() {
        return movieRepository.count() > 1;
    }
}
```

As you can see, the implementation is using a parallel stream to load all the movies keys from Redis. This is done to optimize the loading process. The movies are then saved in batches of 500 records. The implementation also uses an executor service to manage the threads used for saving the movies. To use this code properly, you need to call it from the main class. Edit the Java class `io.redis.movies.searcher.RedisMoviesSearcher.java` and update it as follows:

```java
package io.redis.movies.searcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.redis.movies.searcher.core.service.MovieService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories;

@SpringBootApplication
@EnableRedisDocumentRepositories(basePackages = {"io.redis.movies.searcher.data*"})
@EnableRedisEnhancedRepositories(basePackages = {"io.redis.movies.searcher.core*"})
public class RedisMoviesSearcher {

    private static final Logger log = LoggerFactory.getLogger(RedisMoviesSearcher.class);

    public static void main(String[] args) {
        SpringApplication.run(RedisMoviesSearcher.class, args);
    }

    @Bean
    CommandLineRunner loadData(MovieService movieService) {
        return args -> {
            if (movieService.isDataLoaded()) {
                log.info("Movies already loaded. Skipping data load.");
                return;
            }
            movieService.importMovies();
        };
    }

}
```

If you have the application currently running, please stop its execution and run it again. Once the application finish processing the movies, you should see an output similar to this:

```bash
Starting processing the movies available at Redis...
Found 4527 records with the key prefix 'import:movie'
Loaded 4527 records into memory. Saving them all...
Saved 4527/4527 movies (100.0%)
Processing complete: 4527 source keys loaded, saved 4527 records in 13.40 seconds
```

From this point on, we have the movies saved as Hashes into Redis. Therefore, we don't need anymore the copies of the movies stored as JSON. You can remove them by running the command below. Use Redis Insight to execute this command. It will delete the index `imported_movies_index` and all the JSON documents that were created with the prefix `import:movie:`.

```bash
FT.DROPINDEX 'imported_movies_index' DD
```

## Part 3: Implementing the Searches

In this section, you will implement the search functionality in the application. It will allow users to search for movies using details of the movies like title and actors, as well as search based on the plot. You will implement the code to optimize the searches for maximum efficiency, combining different search strategies and caching computationally expensive operations when necessary.

### Task 1: Implementing the Full-Text Search

#### ⏰ Estimated time: **15 minutes**

With the movies properly stored at Redis, you can now implement the search functionality. You will edit the Java class `io.redis.movies.searcher.core.service.SearchService` and change the implementation as shown below.

```java
package io.redis.movies.searcher.core.service;

import ai.djl.util.Pair;
import com.redis.om.spring.search.stream.EntityStream;
import io.redis.movies.searcher.core.domain.*;
import io.redis.movies.searcher.core.dto.MovieDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final EntityStream entityStream;

    public SearchService(EntityStream entityStream) {
        this.entityStream = entityStream;
    }

    public Pair<List<MovieDTO>, ResultType> searchMovies(String query, Integer limit) {
        logger.info("Received query: {}", query);
        logger.info("-------------------------");
        final int resultLimit = (limit == null) ? 3 : limit;

        // Execute FTS search
        var ftsSearchStartTime = System.currentTimeMillis();
        List<Movie> ftsMovies = entityStream.of(Movie.class)
                .filter(
                        Movie$.TITLE.eq(query).or(Movie$.TITLE.containing(query)).or(
                                ((Predicate<? super String>) Movie$.ACTORS.containsAll(query)))

                )
                .limit(resultLimit)
                .sorted(Comparator.comparing(Movie::getTitle))
                .collect(Collectors.toList());

        var ftsSearchEndTime = System.currentTimeMillis();
        logger.info("FTS search took {} ms", ftsSearchEndTime - ftsSearchStartTime);

        return new Pair<>(convertToDTOs(ftsMovies), ResultType.FTS);
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
```

With this implementation, you are going to be able to search for movies by their title or by their actors. The search will return a list of movies that match the search criteria. The search is done using the `EntityStream` class, which is part of the Redis OM for Java library. This class allows you to perform searches on Redis data using a fluent API. Your instructor will explain more details about this powerful API. Go ahead and execute the application. You can search for movies by their title or by their actors. For example, you can search for movies with the title `Matrix` or with the actor `Keanu Reeves`. You should see the results being displayed on the screen.

![matrix-movies.png](images/matrix-movies.png)

### Task 2: Implementing the Vector Similarity Search

#### ⏰ Estimated time: **30 minutes**

The current implementation of the `searchMovies()` method uses full-text search (FTS) to search for movies. This is a good start, but you can improve the search by using vector similarity search (VSS) as well. VSS allows you to search for movies based on their plot, using embeddings to represent the plot in a vector space. This will allow you to find movies using the plot field even if the words you use are not exactly the same as the words in the plot. Edit the Java class `io.redis.movies.searcher.core.service.SearchService` and change the implementation as shown below.

```java
package io.redis.movies.searcher.core.service;

import ai.djl.util.Pair;
import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.vectorize.Embedder;
import io.redis.movies.searcher.core.domain.*;
import io.redis.movies.searcher.core.dto.MovieDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final EntityStream entityStream;
    private final Embedder embedder;

    public SearchService(EntityStream entityStream, Embedder embedder) {
        this.entityStream = entityStream;
        this.embedder = embedder;
    }

    public Pair<List<MovieDTO>, ResultType> searchMovies(String query, Integer limit) {
        logger.info("Received query: {}", query);
        logger.info("-------------------------");
        final int resultLimit = (limit == null) ? 3 : limit;

        // Execute FTS search
        var ftsSearchStartTime = System.currentTimeMillis();
        List<Movie> ftsMovies = entityStream.of(Movie.class)
                .filter(
                        Movie$.TITLE.eq(query).or(Movie$.TITLE.containing(query)).or(
                                ((Predicate<? super String>) Movie$.ACTORS.containsAll(query)))

                )
                .limit(resultLimit)
                .sorted(Comparator.comparing(Movie::getTitle))
                .collect(Collectors.toList());

        var ftsSearchEndTime = System.currentTimeMillis();
        logger.info("FTS search took {} ms", ftsSearchEndTime - ftsSearchStartTime);

        // Convert FTS results to DTOs
        List<MovieDTO> ftsMovieDTOs = convertToDTOs(ftsMovies);

        // If FTS results are sufficient, return them immediately
        if (ftsMovies.size() >= resultLimit) {
            return new Pair<>(ftsMovieDTOs, ResultType.FTS);
        }

        // Create the embedding
        var embeddingStartTime = System.currentTimeMillis();
        byte[] embeddedQuery = getQueryEmbeddingAsByteArray(query);
        var embeddingEndTime = System.currentTimeMillis();
        logger.info("Embedding took {} ms", embeddingEndTime - embeddingStartTime);

        // Execute VSS search
        var vssSearchStartTime = System.currentTimeMillis();
        List<Movie> vssMovies = entityStream.of(Movie.class)
                .filter(Movie$.PLOT_EMBEDDING.knn(resultLimit, embeddedQuery))
                .limit(resultLimit)
                .sorted(Movie$._PLOT_EMBEDDING_SCORE)
                .collect(Collectors.toList());
        var vssSearchEndTime = System.currentTimeMillis();
        logger.info("VSS search took {} ms", vssSearchEndTime - vssSearchStartTime);

        // Combine results
        LinkedHashMap<Integer, Movie> uniqueMoviesMap = new LinkedHashMap<>();
        ftsMovies.forEach(movie -> uniqueMoviesMap.put(movie.getId(), movie));
        vssMovies.forEach(movie -> uniqueMoviesMap.putIfAbsent(movie.getId(), movie));

        // Limit and convert combined results to DTOs
        List<Movie> uniqueMovies = uniqueMoviesMap.values().stream()
                .limit(resultLimit)
                .collect(Collectors.toList());

        return new Pair<>(convertToDTOs(uniqueMovies), ftsMovies.isEmpty() ? ResultType.VSS : ResultType.HYBRID);
    }

    private byte[] getQueryEmbeddingAsByteArray(String query) {
        return embedder.getTextEmbeddingsAsBytes(List.of(query), Movie$.PLOT).getFirst();
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
```

Note that we only execute the VSS search whenever the FTS search does not return enough results. This is done to optimize the search process as FTS searches are faster than VSS ones. The VSS search is implemented with the `knn()` operation, which is part of the Redis Query Engine implementation. This operation allows you to perform a [K-Nearest Neighbors](https://redis.io/docs/latest/develop/interact/search-and-query/query/vector-search/) search on the vector field. For example, you can search for a movie using the query `Dude who teaches rock` and there should be an entry pointing to the movie `The School of Rock`, along with other movies about the same topic. 

![vss-search.png](images/vss-search.png)

### Task 3: Optimizing the Vector Similarity Search

#### ⏰ Estimated time: **15 minutes**

The current implementation of the `searchMovies()` method uses the `getQueryEmbeddingAsByteArray()` method to create the embedding for the query. It uses an `Embedder` instance to create the embedding. However, this is not efficient because it creates a new embedding every time the method is called. You can further optimize this by caching the embeddings for the keywords used in the search. To support this caching process, create a new Java class called `Keyword` in the package `io.redis.movies.searcher.core.domain` and implement as shown below. 

```java
package io.redis.movies.searcher.core.domain;

import com.redis.om.spring.annotations.*;
import com.redis.om.spring.indexing.DistanceMetric;
import com.redis.om.spring.indexing.VectorType;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import redis.clients.jedis.search.schemafields.VectorField;

@RedisHash(value = "keyword")
public class Keyword {

    @Id
    private String id;

    @Searchable
    @Vectorize(
            destination = "embedding",
            embeddingType = EmbeddingType.SENTENCE
    )
    private String value;

    @Indexed(
            schemaFieldType = SchemaFieldType.VECTOR,
            algorithm = VectorField.VectorAlgorithm.FLAT,
            type = VectorType.FLOAT32,
            dimension = 384,
            distanceMetric = DistanceMetric.COSINE,
            initialCapacity = 10
    )
    private byte[] embedding;

    public Keyword() {}

    public Keyword(String value) {
        this.value = value;
    }
    
    // Create getters and setters for all fields

}
```

Along with the domain entity, you will need a repository implementation to perform data operations. Create a new Java interface called `KeywordRepository` in the package `io.redis.movies.searcher.core.repository`.

```java
package io.redis.movies.searcher.core.repository;

import com.redis.om.spring.repository.RedisEnhancedRepository;
import io.redis.movies.searcher.core.domain.Keyword;

public interface KeywordRepository extends RedisEnhancedRepository<Keyword, String> {
}
```

Finally, you need to update the implementation of the SearchService to cache keywords appropriately. Edit the Java class `io.redis.movies.searcher.core.service.SearchService` and change the implementation as shown below.

```java
package io.redis.movies.searcher.core.service;

import ai.djl.util.Pair;
import com.redis.om.spring.search.stream.EntityStream;
import io.redis.movies.searcher.core.domain.*;
import io.redis.movies.searcher.core.dto.MovieDTO;
import io.redis.movies.searcher.core.repository.KeywordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final EntityStream entityStream;
    private final KeywordRepository keywordRepository;

    public SearchService(EntityStream entityStream, KeywordRepository keywordRepository) {
        this.entityStream = entityStream;
        this.keywordRepository = keywordRepository;
    }

    public Pair<List<MovieDTO>, ResultType> searchMovies(String query, Integer limit) {
        logger.info("Received query: {}", query);
        logger.info("-------------------------");
        final int resultLimit = (limit == null) ? 3 : limit;

        // Execute FTS search
        var ftsSearchStartTime = System.currentTimeMillis();
        List<Movie> ftsMovies = entityStream.of(Movie.class)
                .filter(
                        Movie$.TITLE.eq(query).or(Movie$.TITLE.containing(query)).or(
                                ((Predicate<? super String>) Movie$.ACTORS.containsAll(query)))

                )
                .limit(resultLimit)
                .sorted(Comparator.comparing(Movie::getTitle))
                .collect(Collectors.toList());

        var ftsSearchEndTime = System.currentTimeMillis();
        logger.info("FTS search took {} ms", ftsSearchEndTime - ftsSearchStartTime);

        // Convert FTS results to DTOs
        List<MovieDTO> ftsMovieDTOs = convertToDTOs(ftsMovies);

        // If FTS results are sufficient, return them immediately
        if (ftsMovies.size() >= resultLimit) {
            return new Pair<>(ftsMovieDTOs, ResultType.FTS);
        }

        // Create the embedding
        var embeddingStartTime = System.currentTimeMillis();
        byte[] embeddedQuery = getQueryEmbeddingAsByteArray(query);
        var embeddingEndTime = System.currentTimeMillis();
        logger.info("Embedding took {} ms", embeddingEndTime - embeddingStartTime);

        // Execute VSS search
        var vssSearchStartTime = System.currentTimeMillis();
        List<Movie> vssMovies = entityStream.of(Movie.class)
                .filter(Movie$.PLOT_EMBEDDING.knn(resultLimit, embeddedQuery))
                .limit(resultLimit)
                .sorted(Movie$._PLOT_EMBEDDING_SCORE)
                .collect(Collectors.toList());
        var vssSearchEndTime = System.currentTimeMillis();
        logger.info("VSS search took {} ms", vssSearchEndTime - vssSearchStartTime);

        // Combine results
        LinkedHashMap<Integer, Movie> uniqueMoviesMap = new LinkedHashMap<>();
        ftsMovies.forEach(movie -> uniqueMoviesMap.put(movie.getId(), movie));
        vssMovies.forEach(movie -> uniqueMoviesMap.putIfAbsent(movie.getId(), movie));

        // Limit and convert combined results to DTOs
        List<Movie> uniqueMovies = uniqueMoviesMap.values().stream()
                .limit(resultLimit)
                .collect(Collectors.toList());

        return new Pair<>(convertToDTOs(uniqueMovies), ftsMovies.isEmpty() ? ResultType.VSS : ResultType.HYBRID);
    }

    private byte[] getQueryEmbeddingAsByteArray(String query) {
        return entityStream.of(Keyword.class)
                .filter(Keyword$.VALUE.containing(query))
                .findFirst()
                .map(Keyword::getEmbedding)
                .orElseGet(() -> keywordRepository.save(new Keyword(query)).getEmbedding());
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
```

## Part 4: Moving the dataset to the cloud

In this section, you will replace your local database with one running in the cloud. You will replicate the entire data from one database to another and change the application to point to the new database. By the end, you should be able to use the application just like before with no breaking changes.

### Task 1: Create an account on Redis Cloud

#### ⏰ Estimated time: **5 minutes**

If you don't have one already, you must create a new account with Redis Cloud. Redis Cloud is a service offering from Redis that allows you to run Redis as a fully managed service. It is compatible with different cloud providers, such as AWS, Google Cloud, and Azure. Use the following link to create your account. 

https://redis.io/try-free

You don't need to register a credit card to create your account. You can enroll using your GitHub and Google accounts. Once you create your account, you will be redirected to the Redis Cloud console. 

![rc-welcome.png](images/rc-welcome.png)

To get things started, click in the **New Database** button. This will take you to the database creation page.

### Task 2: Create an 30MB free database

#### ⏰ Estimated time: **5 minutes**

For this workshop, you will create a 30MB free database. This is enough to store the movies dataset. Moroever, this option is free of charge. This option is ideal for developers trying Redis in the cloud. The secret to leverage this free option is the selection of the subscription. Make sure to select the **Essentials** subscription.

![rc-subscription.png](images/rc-subscription.png)

Once you select the subscription, you will be able to select the cloud provider and the region. You can select any of the available options. The database name is not important, but make sure to select a name that is easy to remember.

![rc-db-details.png](images/rc-db-details.png)

Make sure the database size selected is 30MB. This is the maximum size for the free plan. Once you select the database size, click on the **Create Database** button. This will take you to the database details page.

![rc-confirm.png](images/rc-confirm.png)

It usually takes about 30 seconds for your database to be created. Once this process finishes, you can start using your database. In the **General** section, you will find a button named **Connect** that will bring you to the connectivity options. Select the Redis CLI option.

![rc-connect-1.png](images/rc-connect-1.png)

With the **Redis CLI** option, you can copy the command to connect with your database using the Redis CLI. This is not what you are going to do though. What you need from this option is the connection details. Copy the Redis URL from the prompt. The Redis URL has the following sintax:

```bash
redis://username:password@host:port
```

Make sure to click in the eyes icon before copying the Redis URL. It will allow you to copy the password without the masking.

![rc-connect-2.png](images/rc-connect-2.png)

### Task 3: Replicate the entire dataset

#### ⏰ Estimated time: **10 minutes**

Now that you have your database created, you can replicate the entire dataset from your local database to the cloud. You will use the `riot` utility to do this. The command is similar to the one you used to load the JSON file into your local database. The `replicate` option from RIOT allows you to replicate data from one Redis database to another. In this case, the source database will be your local Redis, and the target database will be the one running with Redis Cloud.

```bash
riot replicate \
  redis://localhost:6379 \
  redis://default:password@public-endpoint-with-port
```

### Task 4: Point the application to the cloud

#### ⏰ Estimated time: **10 minutes**

You need to make some changes in your application so that it points to the new database. The connection details will come from the Redis URL you copied earlier. Please make sure to export the following environment variables with the connection details from your Redis Cloud database. You can do this by running the commands below in your terminal.

```bash
export REDIS_HOST=public-endpoint-without-port
```

```bash
export REDIS_PORT=public-endpoint-only-port
```

```bash
export REDIS_USERNAME=default
```

```bash
export REDIS_PASSWORD=password
```

Edit the `src/main/resources/application.properties` file and update it with the new connection details. The file should look like this:

```properties

```java
server.port=8081

spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}

spring.data.redis.username=${REDIS_USERNAME:default}
spring.data.redis.password=${REDIS_PASSWORD}

redis.om.spring.ai.enabled=true
redis.om.spring.ai.embedding-batch-size=2048
redis.om.spring.repository.query.limit=50000
```

You are ready to execute the application. You should be able to use the application just like you did before. The only difference is that now the data is stored in the cloud.

```bash
mvn spring-boot:run
```
