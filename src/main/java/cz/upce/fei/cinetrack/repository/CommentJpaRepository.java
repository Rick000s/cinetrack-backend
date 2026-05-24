package cz.upce.fei.cinetrack.repository;

import cz.upce.fei.cinetrack.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentJpaRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByMovieIdOrderByIdAsc(Long movieId);
}
