package cz.upce.fei.cinetrack.repository;

import cz.upce.fei.cinetrack.model.FavoriteMovie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteMovieJpaRepository extends JpaRepository<FavoriteMovie, Long> {
}
