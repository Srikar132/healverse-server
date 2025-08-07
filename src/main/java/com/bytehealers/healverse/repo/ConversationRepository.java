package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.id = :conversationId AND c.user.id = :userId")
    Optional<Conversation> findByIdAndUserId(@Param("conversationId") String conversationId,
                                             @Param("userId") Long userId);

    boolean existsByIdAndUserId(String conversationId, Long userId);
}