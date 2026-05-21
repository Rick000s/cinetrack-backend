package cz.upce.fei.cinetrack.controller;

import cz.upce.fei.cinetrack.model.Movie;
import cz.upce.fei.cinetrack.repository.MovieRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@CrossOrigin(origins = "*") // Дозволяє фронтенду робити запити (вирішує проблему CORS)
public class MovieController {

    private final MovieRepository movieRepository;

    // Spring сам автоматично підтягне сюди наш репозиторій
    public MovieController(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    // Посилання в браузері: http://localhost:8080/api/movies
    @GetMapping
    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    // Посилання в браузері: http://localhost:8080/api/movies/random
    @GetMapping("/random")
    public Movie getRandomMovie() {
        return movieRepository.getRandom();
    }

    // Сюди фронтенд буде відправляти нові фільми через POST запит
    @PostMapping
    public Movie addMovie(@RequestBody Movie movie) {
        return movieRepository.save(movie);
    }
}