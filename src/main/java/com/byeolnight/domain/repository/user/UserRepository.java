package com.byeolnight.domain.repository.user;

import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
    boolean existsByPhoneHash(String phoneHash);

    boolean existsByNickname(String nickname);
}
