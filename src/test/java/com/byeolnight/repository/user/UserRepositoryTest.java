package com.byeolnight.repository.user;

import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.config.QueryDslConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {

    @Autowired UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private User saveActiveUser(String email, String nickname) {
        return userRepository.save(User.builder()
                .email(email)
                .nickname(nickname)
                .password("encoded")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build());
    }

    // ──────────────────────────────────────────────
    // 이메일 조회
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("이메일로 사용자 조회 (findByEmail)")
    class FindByEmail {

        @Test
        @DisplayName("존재하는 이메일로 조회 시 사용자 반환")
        void shouldReturnUserForExistingEmail() {
            saveActiveUser("user@test.com", "유저");

            Optional<User> result = userRepository.findByEmail("user@test.com");

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 조회 시 빈 Optional 반환")
        void shouldReturnEmptyForNonExistingEmail() {
            Optional<User> result = userRepository.findByEmail("none@test.com");

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // 닉네임 중복 확인
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("닉네임 중복 확인 (existsByNickname)")
    class ExistsByNickname {

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 true 반환")
        void shouldReturnTrueForDuplicateNickname() {
            saveActiveUser("a@test.com", "별하나");

            boolean exists = userRepository.existsByNickname("별하나");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("사용 가능한 닉네임이면 false 반환")
        void shouldReturnFalseForAvailableNickname() {
            boolean exists = userRepository.existsByNickname("없는닉네임");

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("탈퇴 상태 제외 닉네임 중복 확인 — ACTIVE 사용자 있으면 true")
        void shouldReturnTrueWhenActiveUserHasNickname() {
            saveActiveUser("a@test.com", "별하나");

            boolean exists = userRepository.existsByNicknameAndStatusNotIn(
                    "별하나", List.of(User.UserStatus.WITHDRAWN));

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("탈퇴 상태 제외 닉네임 중복 확인 — WITHDRAWN 사용자만 있으면 false")
        void shouldReturnFalseWhenOnlyWithdrawnUserHasNickname() {
            User withdrawn = User.builder()
                    .email("withdrawn@test.com")
                    .nickname("탈퇴닉네임")
                    .password("encoded")
                    .role(User.Role.USER)
                    .status(User.UserStatus.WITHDRAWN)
                    .build();
            userRepository.save(withdrawn);

            boolean exists = userRepository.existsByNicknameAndStatusNotIn(
                    "탈퇴닉네임", List.of(User.UserStatus.WITHDRAWN));

            assertThat(exists).isFalse();
        }
    }

    // ──────────────────────────────────────────────
    // 탈퇴 사용자 조회
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("탈퇴 후 경과 사용자 조회 (findByStatusAndWithdrawnAtBefore)")
    class FindWithdrawnUsers {

        @Test
        @DisplayName("탈퇴 후 30일 경과 사용자가 조회됨")
        void shouldFindUsersWithdrawnBefore30Days() {
            User oldWithdrawn = User.builder()
                    .email("old@test.com")
                    .nickname("오래된탈퇴")
                    .password("encoded")
                    .role(User.Role.USER)
                    .status(User.UserStatus.WITHDRAWN)
                    .build();
            userRepository.save(oldWithdrawn);
            // withdrawnAt을 31일 전으로 직접 설정
            oldWithdrawn.withdraw("탈퇴 테스트");
            // withdraw()는 withdrawnAt = now()로 설정하므로, 경과 판단은 threshold 기준
            userRepository.save(oldWithdrawn);

            User recentWithdrawn = User.builder()
                    .email("recent@test.com")
                    .nickname("최근탈퇴")
                    .password("encoded")
                    .role(User.Role.USER)
                    .status(User.UserStatus.WITHDRAWN)
                    .build();
            userRepository.save(recentWithdrawn);
            recentWithdrawn.withdraw("최근 탈퇴");
            userRepository.save(recentWithdrawn);

            // threshold를 미래로 설정하면 withdrawnAt < threshold 조건 성립
            LocalDateTime futureThreshold = LocalDateTime.now().plusSeconds(10);
            List<User> result = userRepository.findByStatusAndWithdrawnAtBefore(
                    User.UserStatus.WITHDRAWN, futureThreshold);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(u -> u.getStatus() == User.UserStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("ACTIVE 상태 사용자는 탈퇴 조회에서 제외됨")
        void shouldNotIncludeActiveUsersInWithdrawnQuery() {
            saveActiveUser("active@test.com", "활성유저");

            LocalDateTime futureThreshold = LocalDateTime.now().plusSeconds(10);
            List<User> result = userRepository.findByStatusAndWithdrawnAtBefore(
                    User.UserStatus.WITHDRAWN, futureThreshold);

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // 소셜 사용자 30일 경과 조회
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("소셜 사용자 탈퇴 후 경과 조회 (findBySocialProviderIsNotNullAndWithdrawnAtBeforeAndStatus)")
    class FindExpiredSocialUsers {

        @Test
        @DisplayName("소셜 + WITHDRAWN + withdrawnAt 경과 조건 모두 만족 시 반환")
        void shouldFindExpiredSocialWithdrawnUsers() {
            User socialUser = User.builder()
                    .email("social@test.com")
                    .nickname("소셜유저")
                    .password(null)
                    .role(User.Role.USER)
                    .status(User.UserStatus.ACTIVE)
                    .build();
            userRepository.save(socialUser);
            socialUser.setSocialProvider("google");
            socialUser.withdraw("소셜 탈퇴");
            userRepository.save(socialUser);

            LocalDateTime futureThreshold = LocalDateTime.now().plusSeconds(10);
            List<User> result = userRepository
                    .findBySocialProviderIsNotNullAndWithdrawnAtBeforeAndStatus(
                            futureThreshold, User.UserStatus.WITHDRAWN);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo("social@test.com");
        }

        @Test
        @DisplayName("소셜 아닌 일반 WITHDRAWN 사용자는 제외됨")
        void shouldExcludeNonSocialWithdrawnUsers() {
            User normalUser = User.builder()
                    .email("normal@test.com")
                    .nickname("일반유저")
                    .password("encoded")
                    .role(User.Role.USER)
                    .status(User.UserStatus.ACTIVE)
                    .build();
            userRepository.save(normalUser);
            normalUser.withdraw("일반 탈퇴");
            userRepository.save(normalUser);

            LocalDateTime futureThreshold = LocalDateTime.now().plusSeconds(10);
            List<User> result = userRepository
                    .findBySocialProviderIsNotNullAndWithdrawnAtBeforeAndStatus(
                            futureThreshold, User.UserStatus.WITHDRAWN);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("소셜 사용자여도 ACTIVE 상태이면 제외됨")
        void shouldExcludeActiveSocialUsers() {
            User activeSocial = User.builder()
                    .email("activesocial@test.com")
                    .nickname("활성소셜")
                    .password(null)
                    .role(User.Role.USER)
                    .status(User.UserStatus.ACTIVE)
                    .build();
            userRepository.save(activeSocial);
            activeSocial.setSocialProvider("naver");
            userRepository.save(activeSocial);

            LocalDateTime futureThreshold = LocalDateTime.now().plusSeconds(10);
            List<User> result = userRepository
                    .findBySocialProviderIsNotNullAndWithdrawnAtBeforeAndStatus(
                            futureThreshold, User.UserStatus.WITHDRAWN);

            assertThat(result).isEmpty();
        }
    }
}
