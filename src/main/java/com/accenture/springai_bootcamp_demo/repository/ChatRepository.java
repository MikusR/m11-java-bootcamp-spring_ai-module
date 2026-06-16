package com.accenture.springai_bootcamp_demo.repository;

import com.accenture.springai_bootcamp_demo.entity.Chat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, String> {

    List<Chat> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = "chatMessages")
    Optional<Chat> findWithMessagesById(String id);
}
