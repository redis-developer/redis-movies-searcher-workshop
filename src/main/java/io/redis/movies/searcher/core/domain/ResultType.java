package io.redis.movies.searcher.core.domain;

public enum ResultType {
    NONE("None"),
    FTS("Full-Text Search"),
    VSS("Vector Similarity Search"),
    HYBRID("Hybrid Search");

    private final String description;

    ResultType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
