package io.redis.movies.searcher.core.domain;

import java.util.List;

public class Movie {

    private int id;
    private String title;
    private int year;
    private String plot;
    private String releaseDate;
    private double rating;
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public List<String> getActors() {
        return actors;
    }

    public void setActors(List<String> actors) {
        this.actors = actors;
    }

}
