package com.byeolnight.domain.repository.chat;

import com.byeolnight.domain.entity.chat.ChatParticipation;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ChatParticipationRepository extends JpaRepository<ChatParticipation, Long> {
    
    Optional<ChatParticipation> findByUserAndParticipationDate(User user, LocalDate date);
    
    @Query("SELECT SUM(cp.messageCount) FROM ChatParticipation cp WHERE cp.user = :user")
    Long getTotalMessageCountByUser(@Param("user") User user);
}