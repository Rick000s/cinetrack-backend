package cz.upce.fei.cinetrack.repository;

import cz.upce.fei.cinetrack.model.Movie;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Repository
public class MovieRepository {
    // Наш список-імітація таблиці в базі даних
    private final List<Movie> moviesTable = new ArrayList<>();
    private final Random random = new Random();

    // Конструктор, який відразу заповнить "базу" при запуску сервера
    public MovieRepository() {
        moviesTable.add(new Movie(1L, "Inception", "Sci-Fi", 2010, 8.8, "Сон всередині сну. Лео ДіКапріо крутить дзиґу."));
        moviesTable.add(new Movie(2L, "The Dark Knight", "Action", 2008, 9.0, "Джокер Хіта Леджера показує фокус з олівцем."));
        moviesTable.add(new Movie(3L, "Interstellar", "Sci-Fi", 2014, 8.7, "Меттью Макконахі плаче через відносність часу та кукурудзу."));
        moviesTable.add(new Movie(4L, "Pulp Fiction", "Crime", 1994, 8.9, "Джон Траволта та Семюел Л. Джексон обговорюють чізбургери."));
    }

    // Отримати всі фільми
    public List<Movie> findAll() {
        return moviesTable;
    }

    // Додати новий фільм
    public Movie save(Movie movie) {
        // Генеруємо новий ID, ніби це робить база даних
        movie.setId((long) (moviesTable.size() + 1));
        moviesTable.add(movie);
        return movie;
    }

    // Фіча: Рандомізатор фільмів!
    public Movie getRandom() {
        if (moviesTable.isEmpty()) return null;
        return moviesTable.get(random.nextInt(moviesTable.size()));
    }
}