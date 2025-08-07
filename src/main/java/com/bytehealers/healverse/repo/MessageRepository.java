package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Fetch messages by conversation ID (String UUID)
    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    // Optional: If you want to delete messages by conversation
    void deleteByConversationId(String conversationId);
}
