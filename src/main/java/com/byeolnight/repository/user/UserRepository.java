package com.byeolnight.repository.user;

import com.byeolnight.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);

    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    
    List<User> findByRole(User.Role role);
    
    List<User> findByStatusAndWithdrawnAtBefore(User.UserStatus status, LocalDateTime withdrawnAt);
    
    List<User> findByWithdrawnAtBeforeAndStatusIn(LocalDateTime withdrawnAt, List<User.UserStatus> statuses);
    
    @Query("SELECT u FROM User u WHERE u.password IS NULL AND u.status = :status AND (u.lastLoginAt IS NULL OR u.lastLoginAt < :cutoffDate)")
    List<User> findSocialUsersForCleanup(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("status") User.UserStatus status);
    
    List<User> findByPasswordIsNullAndStatus(User.UserStatus status);
}
