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

    @PostMapping("/auth/login")
    public ResponseEntity<User> login(@RequestBody Map<String, String> request) {
        return repo.authenticate(request.get("login"), request.get("password"))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/auth/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        return repo.registerUser(user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping("/movies")
    public List<Movie> getMovies(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        return repo.searchMovies(query, genre, sort);
    }

    @GetMapping("/movies/genres")
    public List<String> getMovieGenres() {
        return repo.findAllGenres();
    }

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
    public List<User> getUsers(@RequestParam(value = "query", required = false) String query) {
        return repo.searchUsers(query);
    }

    @PostMapping("/users")
    public ResponseEntity<User> addUser(@RequestBody User user) {
        return ResponseEntity.ok(repo.addUser(user));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return repo.updateUser(id, user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        return repo.deleteUser(id)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/community/friends/{userId}")
    public List<User> getFriends(@PathVariable Long userId) {
        return repo.findFriends(userId);
    }

    @GetMapping("/community/messages")
    public List<ChatMessage> getCommunityMessages(@RequestParam Long userId, @RequestParam Long friendId) {
        return repo.findCommunityMessages(userId, friendId);
    }

    @PostMapping("/community/messages")
    public ResponseEntity<ChatMessage> addCommunityMessage(@RequestBody ChatMessage message) {
        return repo.addCommunityMessage(message)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }
}
