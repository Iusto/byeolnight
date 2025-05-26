package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.token.Token;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByRefreshToken(String token);

    void deleteByUser(User user);
}
