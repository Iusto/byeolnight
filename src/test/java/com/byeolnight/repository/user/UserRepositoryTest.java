package com.byeolnight.repository.user;

import com.byeolnight.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .phone("01012345678")
                .role(User.Role.USER)
                .build();
    }
    
    @Test
    @DisplayName("이메일로 사용자 조회 성공")
    void 이메일로_사용자_조회_성공() {
        // Given
        entityManager.persistAndFlush(testUser);
        
        // When
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");
        
        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.get().getNickname()).isEqualTo("테스트유저");
    }
    
    @Test
    @DisplayName("존재하지 않는 이메일로 조회 시 빈 결과")
    void 존재하지_않는_이메일_조회() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");
        
        // Then
        assertThat(foundUser).isEmpty();
    }
    
    @Test
    @DisplayName("이메일 중복 확인 - 존재하는 경우")
    void 이메일_중복_확인_존재() {
        // Given
        entityManager.persistAndFlush(testUser);
        
        // When
        boolean exists = userRepository.existsByEmail("test@example.com");
        
        // Then
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("이메일 중복 확인 - 존재하지 않는 경우")
    void 이메일_중복_확인_미존재() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");
        
        // Then
        assertThat(exists).isFalse();
    }
    
    @Test
    @DisplayName("닉네임 중복 확인 - 존재하는 경우")
    void 닉네임_중복_확인_존재() {
        // Given
        entityManager.persistAndFlush(testUser);
        
        // When
        boolean exists = userRepository.existsByNickname("테스트유저");
        
        // Then
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("닉네임 중복 확인 - 존재하지 않는 경우")
    void 닉네임_중복_확인_미존재() {
        // When
        boolean exists = userRepository.existsByNickname("존재하지않는닉네임");
        
        // Then
        assertThat(exists).isFalse();
    }
}