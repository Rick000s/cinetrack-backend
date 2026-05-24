package cz.upce.fei.cinetrack.repository;

import cz.upce.fei.cinetrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {
}
