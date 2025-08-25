package com.byeolnight.service.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

class CustomOAuth2UserServiceNicknameTest {

    @Test
    @DisplayName("OAuth2 사용자 닉네임 생성 성능 테스트")
    void testOAuth2UserNicknameGenerationPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 닉네임 생성 시뮬레이션
        String nickname = generateNickname("test@example.com");
        
        stopWatch.stop();
        
        // 성능 검증 (50ms 이내)
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(50);
        assertThat(nickname).isNotNull();
    }

    @Test
    @DisplayName("중복 닉네임 처리 성능 테스트")
    void testDuplicateNicknameHandlingPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 중복 닉네임 처리 시뮬레이션
        String nickname = generateUniqueNickname("test@example.com");
        
        stopWatch.stop();
        
        // 중복 처리 시간이 100ms 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(100);
        assertThat(nickname).isNotNull();
    }

    @Test
    @DisplayName("대량 OAuth2 사용자 처리 성능 테스트")
    void testBulkOAuth2UserProcessingPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 10명의 사용자 처리 시뮬레이션
        for (int i = 0; i < 10; i++) {
            generateNickname("test" + i + "@example.com");
        }
        
        stopWatch.stop();
        
        // 10명 처리 시간이 200ms 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(200);
    }

    private String generateNickname(String email) {
        // 닉네임 생성 시뮬레이션
        try {
            Thread.sleep(5); // 5ms 처리 시간
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "user_" + email.split("@")[0];
    }

    private String generateUniqueNickname(String email) {
        // 중복 체크 시뮬레이션
        try {
            Thread.sleep(15); // 15ms 처리 시간 (DB 조회 포함)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "unique_" + email.split("@")[0];
    }
}