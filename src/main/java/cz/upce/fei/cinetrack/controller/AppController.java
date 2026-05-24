package cz.upce.fei.cinetrack.controller;

import cz.upce.fei.cinetrack.model.*;
import cz.upce.fei.cinetrack.repository.DataRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class AppController {

    private final DataRepository repo;

    public AppController(DataRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/movies")
    public List<Movie> getMovies() { return repo.findAllMovies(); }

    @GetMapping("/movies/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        return repo.findMovieById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/movies/random")
    public ResponseEntity<Movie> getRandomMovie() {
        return repo.getRandomMovie()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/movies/{id}/comments")
    public List<Comment> getComments(@PathVariable Long id) {
        return repo.findComments(id);
    }

    @PostMapping("/movies/comments")
    public ResponseEntity<Comment> addComment(@RequestBody Comment comment) {
        Comment saved = repo.addComment(comment);
        return ResponseEntity.ok(saved);
    }

    // === НОВЕ: Видалення коментаря ===
    @DeleteMapping("/movies/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        repo.deleteComment(commentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/movies/{id}/favorite")
    public ResponseEntity<Void> toggleFavorite(@PathVariable Long id) {
        repo.toggleFav(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/movies/{id}/is-favorite")
    public Map<String, Boolean> isFavorite(@PathVariable Long id) {
        return Map.of("favorite", repo.isFav(id));
    }

    @GetMapping("/movies/favorites")
    public List<Movie> getFavorites() {
        return repo.getFavs();
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return repo.findAllUsers();
    }
}