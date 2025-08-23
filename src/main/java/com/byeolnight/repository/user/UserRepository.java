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
    boolean existsByNicknameAndStatusNotIn(String nickname, List<User.UserStatus> statuses);
    
    List<User> findByRole(User.Role role);
    
    List<User> findByStatusAndWithdrawnAtBefore(User.UserStatus status, LocalDateTime withdrawnAt);
    
    List<User> findByWithdrawnAtBeforeAndStatusIn(LocalDateTime withdrawnAt, List<User.UserStatus> statuses);
    
    // 소셜 사용자 중 탈퇴 신청 후 30일 경과한 사용자 조회
    List<User> findBySocialProviderIsNotNullAndWithdrawnAtBeforeAndStatus(
        LocalDateTime withdrawnAt, User.UserStatus status);
}
