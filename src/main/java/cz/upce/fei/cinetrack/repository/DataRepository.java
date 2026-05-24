package cz.upce.fei.cinetrack.repository;

import com.fasterxml.jackson.databind.JsonNode;
import cz.upce.fei.cinetrack.model.ChatMessage;
import cz.upce.fei.cinetrack.model.Comment;
import cz.upce.fei.cinetrack.model.FavoriteMovie;
import cz.upce.fei.cinetrack.model.Movie;
import cz.upce.fei.cinetrack.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Repository
public class DataRepository {

    private static final String FALLBACK_GENRE = "Film";

    private static final Map<Integer, String> TMDB_GENRES = Map.ofEntries(
            Map.entry(28, "Action"),
            Map.entry(12, "Adventure"),
            Map.entry(16, "Animation"),
            Map.entry(35, "Comedy"),
            Map.entry(80, "Crime"),
            Map.entry(99, "Documentary"),
            Map.entry(18, "Drama"),
            Map.entry(10751, "Family"),
            Map.entry(14, "Fantasy"),
            Map.entry(36, "History"),
            Map.entry(27, "Horror"),
            Map.entry(10402, "Music"),
            Map.entry(9648, "Mystery"),
            Map.entry(10749, "Romance"),
            Map.entry(878, "Science Fiction"),
            Map.entry(10770, "TV Movie"),
            Map.entry(53, "Thriller"),
            Map.entry(10752, "War"),
            Map.entry(37, "Western")
    );

    private final MovieJpaRepository movieRepository;
    private final CommentJpaRepository commentRepository;
    private final FavoriteMovieJpaRepository favoriteMovieRepository;
    private final UserJpaRepository userRepository;
    private final ChatMessageJpaRepository chatMessageRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String bearerToken;

    public DataRepository(
            MovieJpaRepository movieRepository,
            CommentJpaRepository commentRepository,
            FavoriteMovieJpaRepository favoriteMovieRepository,
            UserJpaRepository userRepository,
            ChatMessageJpaRepository chatMessageRepository,
            @Value("${tmdb.api.key:}") String bearerToken
    ) {
        this.movieRepository = movieRepository;
        this.commentRepository = commentRepository;
        this.favoriteMovieRepository = favoriteMovieRepository;
        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.bearerToken = bearerToken;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeData() {
        loadUsers();
        loadCommunityMessages();
        loadMoviesFromTMDB();
    }

    private void loadUsers() {
        if (userRepository.count() > 0) {
            System.out.println("Loaded " + userRepository.count() + " users from database");
            return;
        }

        userRepository.saveAll(List.of(
                new User(null, "_Rick_000_", "rik24096@gmail.com", "0502241023", "ADMIN"),
                new User(null, "Moderator", "moderator@cinetrack.cz", "mod123", "MODERATOR"),
                new User(null, "Alexandr Ivanov", "alex.iv@gmail.com", "user123", "USER"),
                new User(null, "Marie Kovalova", "maria.kov@ukr.net", "user123", "USER"),
                new User(null, "Dmytro Shevchenko", "dmytro.shevchenko@gmail.com", "user123", "USER"),
                new User(null, "Anna Petrenko", "anna.petrenko@outlook.com", "user123", "USER")
        ));
        System.out.println("Seeded default users");
    }

    private void loadCommunityMessages() {
        if (chatMessageRepository.count() > 0) {
            System.out.println("Loaded " + chatMessageRepository.count() + " chat messages from database");
            return;
        }

        chatMessageRepository.saveAll(List.of(
                new ChatMessage(null, 3L, 1L, "Alexandr Ivanov", "Ahoj, videl jsi dnes nejaky dobry film?", "2026-05-24 08:00"),
                new ChatMessage(null, 1L, 3L, "_Rick_000_", "Jeste ne, ale v katalogu je dost novinek.", "2026-05-24 08:03"),
                new ChatMessage(null, 6L, 1L, "Anna Petrenko", "Doporucis mi neco na vecer?", "2026-05-24 08:10")
        ));
        System.out.println("Seeded default chat messages");
    }

    private void loadMoviesFromTMDB() {
        if (movieRepository.count() > 0) {
            System.out.println("Loaded " + movieRepository.count() + " movies from database");
            return;
        }

        if (safeTrim(bearerToken).isBlank()) {
            System.err.println("TMDB token is empty. Using fallback movies.");
            addFallbackMovies();
            return;
        }

        try {
            List<Movie> loadedMovies = new ArrayList<>();

            for (int page = 1; page <= 4; page++) {
                String url = "https://api.themoviedb.org/3/movie/popular?language=cs-CZ&page=" + page;

                HttpHeaders headers = new HttpHeaders();
                headers.set("accept", "application/json");
                headers.set("Authorization", "Bearer " + bearerToken);

                ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
                JsonNode body = response.getBody();
                if (body == null || !body.has("results")) {
                    continue;
                }

                for (JsonNode node : body.get("results")) {
                    String posterPath = node.path("poster_path").asText("");
                    if (posterPath.isBlank()) {
                        continue;
                    }

                    Long movieId = node.path("id").asLong();
                    List<String> genres = resolveGenres(node.path("genre_ids"));

                    Movie movie = new Movie();
                    movie.setId(movieId);
                    movie.setTitle(node.path("title").asText());
                    movie.setPosterUrl("https://image.tmdb.org/t/p/w500" + posterPath);
                    movie.setGenre(String.join(", ", genres));
                    movie.setGenres(genres);
                    movie.setYear(extractYear(node.path("release_date").asText("")));
                    movie.setRating(node.path("vote_average").asDouble());
                    movie.setDescription(node.path("overview").asText("Popis neni dostupny..."));
                    movie.setTrailerUrl(getTrailerUrl(movieId));

                    loadedMovies.add(movie);
                }
            }

            if (loadedMovies.isEmpty()) {
                addFallbackMovies();
                return;
            }

            movieRepository.saveAll(loadedMovies);
            System.out.println("Loaded " + loadedMovies.size() + " movies from TMDB");
        } catch (Exception e) {
            System.err.println("TMDB loading failed: " + e.getMessage());
            addFallbackMovies();
        }
    }

    private String getTrailerUrl(Long movieId) {
        try {
            String url = "https://api.themoviedb.org/3/movie/" + movieId + "/videos?language=cs-CZ";
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.set("Authorization", "Bearer " + bearerToken);

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
            JsonNode body = response.getBody();
            if (body == null || !body.has("results")) {
                return "";
            }

            for (JsonNode video : body.get("results")) {
                if ("Trailer".equals(video.path("type").asText()) && "YouTube".equals(video.path("site").asText())) {
                    return video.path("key").asText();
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private void addFallbackMovies() {
        if (movieRepository.count() > 0) {
            return;
        }

        movieRepository.saveAll(List.of(
                fallbackMovie(1001L, "Inception", "Science Fiction", "Action", "Thriller"),
                fallbackMovie(1002L, "The Grand Budapest Hotel", "Comedy", "Drama"),
                fallbackMovie(1003L, "Spider-Man: Into the Spider-Verse", "Animation", "Action", "Adventure"),
                fallbackMovie(1004L, "Interstellar", "Science Fiction", "Drama", "Adventure")
        ));
        System.out.println("Seeded fallback movies");
    }

    private Movie fallbackMovie(Long id, String title, String... genres) {
        List<String> genreList = List.of(genres);
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle(title);
        movie.setPosterUrl("");
        movie.setGenre(String.join(", ", genreList));
        movie.setGenres(genreList);
        movie.setYear(2024);
        movie.setRating(8.0);
        movie.setDescription("Fallback movie record.");
        movie.setTrailerUrl("");
        return movie;
    }

    public List<Movie> findAllMovies() {
        return movieRepository.findAll();
    }

    public List<Movie> searchMovies(String query) {
        return searchMovies(query, null, null);
    }

    public List<Movie> searchMovies(String query, String genre, String sort) {
        String normalizedQuery = normalize(query);
        String normalizedGenre = normalize(genre);

        List<Movie> result = movieRepository.findAll().stream()
                .filter(movie -> matchesGenre(movie, normalizedGenre))
                .filter(movie -> matchesQuery(movie, normalizedQuery))
                .toList();

        if ("genre".equals(normalize(sort))) {
            return result.stream()
                    .sorted(Comparator
                            .comparing(this::primaryGenre, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(Movie::getTitle, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        return result;
    }

    public List<String> findAllGenres() {
        return movieRepository.findAll().stream()
                .flatMap(movie -> movieGenres(movie).stream())
                .filter(genre -> !safeTrim(genre).isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public Optional<Movie> findMovieById(Long id) {
        return movieRepository.findById(id);
    }

    public Optional<Movie> getRandomMovie() {
        List<Movie> movies = movieRepository.findAll();
        if (movies.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(movies.get(new java.util.Random().nextInt(movies.size())));
    }

    public List<Comment> findComments(Long movieId) {
        return commentRepository.findByMovieIdOrderByIdAsc(movieId);
    }

    public Comment addComment(Comment comment) {
        comment.setId(null);
        return commentRepository.save(comment);
    }

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    public void toggleFav(Long id) {
        if (favoriteMovieRepository.existsById(id)) {
            favoriteMovieRepository.deleteById(id);
        } else {
            favoriteMovieRepository.save(new FavoriteMovie(id));
        }
    }

    public boolean isFav(Long id) {
        return favoriteMovieRepository.existsById(id);
    }

    public List<Movie> getFavs() {
        Set<Long> favoriteIds = new HashSet<>(favoriteMovieRepository.findAll().stream()
                .map(FavoriteMovie::getMovieId)
                .toList());

        return movieRepository.findAll().stream()
                .filter(movie -> favoriteIds.contains(movie.getId()))
                .toList();
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> authenticate(String login, String password) {
        String normalizedLogin = normalize(login);
        String safePassword = safeTrim(password);

        return userRepository.findAll().stream()
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

        user.setId(null);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole("USER");
        return Optional.of(userRepository.save(user));
    }

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> searchUsers(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return findAllUsers();
        }

        return userRepository.findAll().stream()
                .filter(user ->
                        normalize(user.getUsername()).contains(normalizedQuery)
                                || normalize(user.getEmail()).contains(normalizedQuery)
                                || normalize(user.getRole()).contains(normalizedQuery))
                .toList();
    }

    public User addUser(User user) {
        user.setId(null);
        user.setUsername(safeTrim(user.getUsername()));
        user.setEmail(safeTrim(user.getEmail()));
        user.setPassword(defaultPassword(user.getPassword()));
        user.setRole(normalizeRole(user.getRole()));
        return userRepository.save(user);
    }

    public Optional<User> updateUser(Long id, User updatedUser) {
        return userRepository.findById(id)
                .map(user -> {
                    if (updatedUser.getUsername() != null) {
                        user.setUsername(safeTrim(updatedUser.getUsername()));
                    }
                    if (updatedUser.getEmail() != null) {
                        user.setEmail(safeTrim(updatedUser.getEmail()));
                    }
                    if (updatedUser.getPassword() != null && !safeTrim(updatedUser.getPassword()).isBlank()) {
                        user.setPassword(safeTrim(updatedUser.getPassword()));
                    }
                    if (updatedUser.getRole() != null) {
                        user.setRole(normalizeRole(updatedUser.getRole()));
                    }
                    return userRepository.save(user);
                });
    }

    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }

    public void addMovie(Movie movie) {
        if (movie.getId() == null) {
            movie.setId(nextMovieId());
        }
        normalizeMovieGenres(movie);
        movieRepository.save(movie);
    }

    public List<User> findFriends(Long userId) {
        return userRepository.findAll().stream()
                .filter(user -> !user.getId().equals(userId))
                .toList();
    }

    public List<ChatMessage> findCommunityMessages(Long userId, Long friendId) {
        return chatMessageRepository.findAll().stream()
                .filter(message ->
                        (Objects.equals(message.getSenderId(), userId) && Objects.equals(message.getReceiverId(), friendId))
                                || (Objects.equals(message.getSenderId(), friendId) && Objects.equals(message.getReceiverId(), userId)))
                .sorted(Comparator.comparing(ChatMessage::getId))
                .toList();
    }

    public Optional<ChatMessage> addCommunityMessage(ChatMessage message) {
        String text = safeTrim(message.getText());
        if (message.getSenderId() == null || message.getReceiverId() == null || text.isBlank()) {
            return Optional.empty();
        }

        return findUserById(message.getSenderId()).map(sender -> {
            message.setId(null);
            message.setSenderName(sender.getUsername());
            message.setText(text);
            message.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            return chatMessageRepository.save(message);
        });
    }

    private List<String> resolveGenres(JsonNode genreIds) {
        List<String> genres = new ArrayList<>();
        if (genreIds != null && genreIds.isArray()) {
            for (JsonNode genreId : genreIds) {
                String genre = TMDB_GENRES.get(genreId.asInt());
                if (genre != null && !genres.contains(genre)) {
                    genres.add(genre);
                }
            }
        }

        if (genres.isEmpty()) {
            genres.add(FALLBACK_GENRE);
        }
        return genres;
    }

    private int extractYear(String releaseDate) {
        if (releaseDate != null && releaseDate.length() >= 4) {
            try {
                return Integer.parseInt(releaseDate.substring(0, 4));
            } catch (NumberFormatException ignored) {
            }
        }
        return 2023;
    }

    private boolean matchesGenre(Movie movie, String normalizedGenre) {
        if (normalizedGenre.isBlank() || "all".equals(normalizedGenre)) {
            return true;
        }

        return movieGenres(movie).stream()
                .map(this::normalize)
                .anyMatch(genre -> genre.equals(normalizedGenre));
    }

    private boolean matchesQuery(Movie movie, String normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            return true;
        }

        return normalize(movie.getTitle()).contains(normalizedQuery)
                || normalize(movie.getGenre()).contains(normalizedQuery)
                || movieGenres(movie).stream().map(this::normalize).anyMatch(genre -> genre.contains(normalizedQuery))
                || normalize(movie.getDescription()).contains(normalizedQuery)
                || String.valueOf(movie.getYear()).contains(normalizedQuery);
    }

    private String primaryGenre(Movie movie) {
        return movieGenres(movie).stream().findFirst().orElse(FALLBACK_GENRE);
    }

    private List<String> movieGenres(Movie movie) {
        if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
            return movie.getGenres();
        }

        String genre = safeTrim(movie.getGenre());
        if (!genre.isBlank()) {
            return List.of(genre.split(",")).stream()
                    .map(this::safeTrim)
                    .filter(value -> !value.isBlank())
                    .toList();
        }

        return List.of(FALLBACK_GENRE);
    }

    private void normalizeMovieGenres(Movie movie) {
        List<String> genres = new ArrayList<>(movieGenres(movie));
        movie.setGenres(genres);
        movie.setGenre(String.join(", ", genres));
    }

    private Long nextMovieId() {
        return movieRepository.findAll().stream()
                .map(Movie::getId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(1000L) + 1;
    }

    private boolean userExists(String username, String email) {
        String normalizedUsername = normalize(username);
        String normalizedEmail = normalize(email);

        return userRepository.findAll().stream()
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
