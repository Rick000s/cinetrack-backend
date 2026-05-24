package cz.upce.fei.cinetrack.repository;

import cz.upce.fei.cinetrack.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieJpaRepository extends JpaRepository<Movie, Long> {
}
