package cz.upce.fei.cinetrack.repository;

import cz.upce.fei.cinetrack.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, Long> {
}
