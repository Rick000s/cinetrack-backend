package cz.upce.fei.cinetrack.repository;

import cz.upce.fei.cinetrack.model.Movie;
import cz.upce.fei.cinetrack.model.Comment;
import cz.upce.fei.cinetrack.model.User;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

@Repository
public class DataRepository {

    private final List<Movie> movies = new ArrayList<>();
    private final List<Comment> comments = new ArrayList<>();
    private final Set<Long> favorites = new HashSet<>();
    private final List<User> users = new ArrayList<>();

    private final RestTemplate restTemplate = new RestTemplate();

    private final String BEARER_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4NjZkMDAzMWU2ZGUwNWUwYzY0ZjcxNmUyMWVhYWIzNyIsIm5iZiI6MTc3OTU5MDk5Ny44MjYsInN1YiI6IjZhMTI2NzU1NTg1ZTI0ODM5ZGUzNzU2ZCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.LDvbynzJLeFJf2j84A_Cy02XGIuwpfY98HBm-kIZWAo";

    public DataRepository() {
        loadUsers();
        loadMoviesFromTMDB();
    }

    private void loadUsers() {
        users.clear();
        users.add(new User(1L, "Administrátor", "admin@cinetrack.cz", "ADMIN"));
        users.add(new User(2L, "Moderátor", "moderator@cinetrack.cz", "MODERATOR"));
        users.add(new User(3L, "Alexandr Ivanov", "alex.iv@gmail.com", "USER"));
        users.add(new User(4L, "Marie Kovalová", "maria.kov@ukr.net", "USER"));
        users.add(new User(5L, "Dmytro Shevchenko", "dmytro.shevchenko@gmail.com", "USER"));
        users.add(new User(6L, "Anna Petrenko", "anna.petrenko@outlook.com", "USER"));
        System.out.println("✅ Načteno " + users.size() + " uživatelů");
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

    public void addMovie(Movie movie) {
        movie.setId((long) (movies.size() + 1));
        movies.add(movie);
    }
}