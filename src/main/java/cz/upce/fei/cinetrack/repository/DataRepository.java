package cz.upce.fei.cinetrack.repository;

import cz.upce.fei.cinetrack.model.Movie;
import cz.upce.fei.cinetrack.model.Comment;
import cz.upce.fei.cinetrack.model.User;
import cz.upce.fei.cinetrack.model.ChatMessage;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Repository
public class DataRepository {

    private final List<Movie> movies = new ArrayList<>();
    private final List<Comment> comments = new ArrayList<>();
    private final Set<Long> favorites = new HashSet<>();
    private final List<User> users = new ArrayList<>();
    private final List<ChatMessage> communityMessages = new ArrayList<>();

    private final RestTemplate restTemplate = new RestTemplate();

    private final String BEARER_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4NjZkMDAzMWU2ZGUwNWUwYzY0ZjcxNmUyMWVhYWIzNyIsIm5iZiI6MTc3OTU5MDk5Ny44MjYsInN1YiI6IjZhMTI2NzU1NTg1ZTI0ODM5ZGUzNzU2ZCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.LDvbynzJLeFJf2j84A_Cy02XGIuwpfY98HBm-kIZWAo";

    public DataRepository() {
        loadUsers();
        loadCommunityMessages();
        loadMoviesFromTMDB();
    }

    private void loadUsers() {
        users.clear();
        users.add(new User(1L, "_Rick_000_", "rik24096@gmail.com", "0502241023", "ADMIN"));
        users.add(new User(2L, "Moderátor", "moderator@cinetrack.cz", "mod123", "MODERATOR"));
        users.add(new User(3L, "Alexandr Ivanov", "alex.iv@gmail.com", "user123", "USER"));
        users.add(new User(4L, "Marie Kovalová", "maria.kov@ukr.net", "user123", "USER"));
        users.add(new User(5L, "Dmytro Shevchenko", "dmytro.shevchenko@gmail.com", "user123", "USER"));
        users.add(new User(6L, "Anna Petrenko", "anna.petrenko@outlook.com", "user123", "USER"));
        System.out.println("✅ Načteno " + users.size() + " uživatelů");
    }

    private void loadCommunityMessages() {
        communityMessages.clear();
        communityMessages.add(new ChatMessage(1L, 3L, 1L, "Alexandr Ivanov", "Ahoj, viděl jsi dnes nějaký dobrý film?", "2026-05-24 08:00"));
        communityMessages.add(new ChatMessage(2L, 1L, 3L, "_Rick_000_", "Ještě ne, ale v katalogu je dost novinek.", "2026-05-24 08:03"));
        communityMessages.add(new ChatMessage(3L, 6L, 1L, "Anna Petrenko", "Doporučíš mi něco na večer?", "2026-05-24 08:10"));
    }

    private void loadMoviesFromTMDB() {
        try {
            movies.clear();
            int totalLoaded = 0;

            for (int page = 1; page <= 4; page++) {
                String url = "https://api.themoviedb.org/3/movie/popular?language=cs-CZ&page=" + page;

                HttpHeaders headers = new HttpHeaders();
                headers.set("accept", "application/json");
                headers.set("Authorization", "Bearer " + BEARER_TOKEN);

                ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

                JsonNode results = response.getBody().get("results");

                for (JsonNode node : results) {
                    String posterPath = node.get("poster_path").asText(null);
                    if (posterPath == null || posterPath.isEmpty()) continue;

                    Long movieId = node.get("id").asLong();

                    Movie movie = new Movie();
                    movie.setId(movieId);
                    movie.setTitle(node.get("title").asText());
                    movie.setPosterUrl("https://image.tmdb.org/t/p/w500" + posterPath);
                    movie.setGenre("Film");
                    movie.setYear(node.hasNonNull("release_date") && node.get("release_date").asText().length() >= 4
                            ? Integer.parseInt(node.get("release_date").asText().substring(0, 4)) : 2023);
                    movie.setRating(node.get("vote_average").asDouble());
                    movie.setDescription(node.get("overview").asText("Popis není dostupný..."));
                    movie.setTrailerUrl(getTrailerUrl(movieId));

                    movies.add(movie);
                    totalLoaded++;
                }
            }
            System.out.println("✅ Načteno " + totalLoaded + " filmů z TMDB (cs-CZ)");
        } catch (Exception e) {
            System.err.println("❌ Chyba při načítání z TMDB: " + e.getMessage());
            addFallbackMovies();
        }
    }

    private String getTrailerUrl(Long movieId) {
        try {
            String url = "https://api.themoviedb.org/3/movie/" + movieId + "/videos?language=cs-CZ";
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.set("Authorization", "Bearer " + BEARER_TOKEN);

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

            JsonNode results = response.getBody().get("results");
            for (JsonNode video : results) {
                if ("Trailer".equals(video.get("type").asText()) && "YouTube".equals(video.get("site").asText())) {
                    return video.get("key").asText();
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private void addFallbackMovies() {
        // резерв, якщо TMDB не працює
        System.out.println("⚠️ Používám fallback filmy");
    }

    // ==================== ОСНОВНІ МЕТОДИ ====================

    public List<Movie> findAllMovies() { return new ArrayList<>(movies); }

    public List<Movie> searchMovies(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) return findAllMovies();

        return movies.stream()
                .filter(movie ->
                        normalize(movie.getTitle()).contains(normalizedQuery)
                                || normalize(movie.getGenre()).contains(normalizedQuery)
                                || normalize(movie.getDescription()).contains(normalizedQuery)
                                || String.valueOf(movie.getYear()).contains(normalizedQuery))
                .toList();
    }

    public Optional<Movie> findMovieById(Long id) {
        return movies.stream().filter(m -> m.getId().equals(id)).findFirst();
    }

    public Optional<Movie> getRandomMovie() {
        if (movies.isEmpty()) return Optional.empty();
        return Optional.of(movies.get(new Random().nextInt(movies.size())));
    }

    public List<Comment> findComments(Long movieId) {
        return comments.stream().filter(c -> c.getMovieId().equals(movieId)).toList();
    }

    public Comment addComment(Comment c) {
        c.setId((long) (comments.size() + 1));
        comments.add(c);
        return c;
    }

    public void deleteComment(Long commentId) {
        comments.removeIf(c -> c.getId().equals(commentId));
    }

    public void toggleFav(Long id) {
        if (favorites.contains(id)) {
            favorites.remove(id);
        } else {
            favorites.add(id);
        }
    }

    public boolean isFav(Long id) {
        return favorites.contains(id);
    }

    public List<Movie> getFavs() {
        return movies.stream()
                .filter(m -> favorites.contains(m.getId()))
                .toList();
    }

    public List<User> findAllUsers() {
        return new ArrayList<>(users);
    }

    public Optional<User> authenticate(String login, String password) {
        String normalizedLogin = normalize(login);
        String safePassword = safeTrim(password);

        return users.stream()
                .filter(user -> (normalize(user.getUsername()).equals(normalizedLogin)
                        || normalize(user.getEmail()).equals(normalizedLogin))
                        && safeTrim(user.getPassword()).equals(safePassword))
                .findFirst();
    }

    public Optional<User> registerUser(User user) {
        String username = safeTrim(user.getUsername());
        String email = safeTrim(user.getEmail());
        String password = safeTrim(user.getPassword());

        if (username.isBlank() || email.isBlank() || password.isBlank() || userExists(username, email)) {
            return Optional.empty();
        }

        user.setId(nextUserId());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole("USER");
        users.add(user);
        return Optional.of(user);
    }

    public Optional<User> findUserById(Long id) {
        return users.stream().filter(user -> user.getId().equals(id)).findFirst();
    }

    public List<User> searchUsers(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) return findAllUsers();

        return users.stream()
                .filter(user ->
                        normalize(user.getUsername()).contains(normalizedQuery)
                                || normalize(user.getEmail()).contains(normalizedQuery)
                                || normalize(user.getRole()).contains(normalizedQuery))
                .toList();
    }

    public User addUser(User user) {
        user.setId(nextUserId());
        user.setUsername(safeTrim(user.getUsername()));
        user.setEmail(safeTrim(user.getEmail()));
        user.setPassword(defaultPassword(user.getPassword()));
        user.setRole(normalizeRole(user.getRole()));
        users.add(user);
        return user;
    }

    public Optional<User> updateUser(Long id, User updatedUser) {
        return users.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst()
                .map(user -> {
                    if (updatedUser.getUsername() != null) user.setUsername(safeTrim(updatedUser.getUsername()));
                    if (updatedUser.getEmail() != null) user.setEmail(safeTrim(updatedUser.getEmail()));
                    if (updatedUser.getPassword() != null && !safeTrim(updatedUser.getPassword()).isBlank()) {
                        user.setPassword(safeTrim(updatedUser.getPassword()));
                    }
                    if (updatedUser.getRole() != null) user.setRole(normalizeRole(updatedUser.getRole()));
                    return user;
                });
    }

    public boolean deleteUser(Long id) {
        return users.removeIf(user -> user.getId().equals(id));
    }

    public void addMovie(Movie movie) {
        movie.setId((long) (movies.size() + 1));
        movies.add(movie);
    }

    public List<User> findFriends(Long userId) {
        return users.stream()
                .filter(user -> !user.getId().equals(userId))
                .toList();
    }

    public List<ChatMessage> findCommunityMessages(Long userId, Long friendId) {
        return communityMessages.stream()
                .filter(message ->
                        (message.getSenderId().equals(userId) && message.getReceiverId().equals(friendId))
                                || (message.getSenderId().equals(friendId) && message.getReceiverId().equals(userId)))
                .sorted(Comparator.comparing(ChatMessage::getId))
                .toList();
    }

    public Optional<ChatMessage> addCommunityMessage(ChatMessage message) {
        String text = safeTrim(message.getText());
        if (message.getSenderId() == null || message.getReceiverId() == null || text.isBlank()) {
            return Optional.empty();
        }

        return findUserById(message.getSenderId()).map(sender -> {
            message.setId(nextMessageId());
            message.setSenderName(sender.getUsername());
            message.setText(text);
            message.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            communityMessages.add(message);
            return message;
        });
    }

    private Long nextUserId() {
        return users.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }

    private Long nextMessageId() {
        return communityMessages.stream()
                .map(ChatMessage::getId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }

    private boolean userExists(String username, String email) {
        String normalizedUsername = normalize(username);
        String normalizedEmail = normalize(email);

        return users.stream()
                .anyMatch(user -> normalize(user.getUsername()).equals(normalizedUsername)
                        || normalize(user.getEmail()).equals(normalizedEmail));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeRole(String role) {
        String normalizedRole = safeTrim(role).toUpperCase(Locale.ROOT);
        return switch (normalizedRole) {
            case "ADMIN", "MODERATOR" -> normalizedRole;
            default -> "USER";
        };
    }

    private String defaultPassword(String password) {
        String safePassword = safeTrim(password);
        return safePassword.isBlank() ? "user123" : safePassword;
    }
}
