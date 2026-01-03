package com.byeolnight.service.auth;

import com.byeolnight.dto.auth.EmailJob;
import com.byeolnight.infrastructure.cache.RedissonCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * EmailAuthService 테스트 - 이메일 인증 비즈니스 로직 검증
 *
 * 테스트 범위:
 * 1. 인증 코드 생성 및 비동기 큐 추가
 * 2. SHA-256 해시로 인증 코드 저장
 * 3. 인증 코드 검증 (해시 비교)
 * 4. 원자적 카운터로 시도 횟수 제한
 * 5. 인증 상태 관리
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailAuthService 테스트")
class EmailAuthServiceTest {

    @InjectMocks
    private EmailAuthService emailAuthService;

    @Mock
    private RedissonCacheService cacheService;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_IP = "192.168.1.1";
    private final String VERIFICATION_SECRET = "test-secret";

    @BeforeEach
    void setUp() {
        // 기본적으로 인증되지 않은 상태로 설정
        given(cacheService.get("verified:email:" + TEST_EMAIL)).willReturn(null);
        given(cacheService.getCounter(anyString())).willReturn(0L);

        // verificationSecret 주입
        ReflectionTestUtils.setField(emailAuthService, "verificationSecret", VERIFICATION_SECRET);
    }

    @Nested
    @DisplayName("인증 코드 전송")
    class SendVerificationCode {

        @Test
        @DisplayName("정상 인증 코드 전송 - 해시 저장 및 큐 추가")
        void sendCode_Success_StoresHashAndEnqueuesJob() {
            // When
            emailAuthService.sendCode(TEST_EMAIL);

            // Then
            // 1. Redis에 해시 저장 확인 (5분 TTL)
            ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
            verify(cacheService).set(
                    eq("email:" + TEST_EMAIL),
                    hashCaptor.capture(),
                    eq(Duration.ofMinutes(5))
            );

            // 해시값은 64자리 hex 문자열 (SHA-256)
            assertThat(hashCaptor.getValue()).matches("[a-f0-9]{64}");

            // 2. 전송 횟수 원자적 증가 확인
            verify(cacheService).incrementAndGet(
                    eq("send_attempts:email:" + TEST_EMAIL),
                    eq(Duration.ofMinutes(10))
            );

            // 3. 이메일 작업 큐에 추가 확인
            ArgumentCaptor<EmailJob> jobCaptor = ArgumentCaptor.forClass(EmailJob.class);
            verify(cacheService).enqueue(eq("queue:mail"), jobCaptor.capture());

            EmailJob job = jobCaptor.getValue();
            assertThat(job.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(job.getSubject()).isEqualTo("[별 헤는 밤] 이메일 인증 코드");
            assertThat(job.getHtmlBody()).contains("인증 코드");
            assertThat(job.getAttempt()).isEqualTo(0);
        }

        @Test
        @DisplayName("이미 인증된 이메일 - 전송 차단")
        void sendCode_AlreadyVerified_ThrowsException() {
            // Given
            given(cacheService.get("verified:email:" + TEST_EMAIL)).willReturn("true");

            // When & Then
            assertThatThrownBy(() -> emailAuthService.sendCode(TEST_EMAIL))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("이미 인증이 완료된 이메일입니다.");

            // 큐에 추가되지 않음을 확인
            verify(cacheService, never()).enqueue(anyString(), any());
        }

        @Test
        @DisplayName("전송 횟수 5회 초과 - 차단")
        void sendCode_SendLimitExceeded_ThrowsException() {
            // Given
            given(cacheService.getCounter("send_attempts:email:" + TEST_EMAIL)).willReturn(5L);

            // When & Then
            assertThatThrownBy(() -> emailAuthService.sendCode(TEST_EMAIL))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("인증 코드 전송 횟수를 초과했습니다. 10분 후 다시 시도해주세요.");

            // 큐에 추가되지 않음을 확인
            verify(cacheService, never()).enqueue(anyString(), any());
        }
    }

    @Nested
    @DisplayName("인증 코드 검증 (해시 기반)")
    class VerifyCode {

        @Test
        @DisplayName("올바른 인증 코드 검증 - 해시 일치")
        void verifyCode_ValidCode_ReturnsTrue() {
            // Given
            String code = "ABC12345";
            String expectedHash = generateExpectedHash(TEST_EMAIL, code);

            given(cacheService.get("email:" + TEST_EMAIL)).willReturn(expectedHash);

            // When
            boolean result = emailAuthService.verifyCode(TEST_EMAIL, code, TEST_IP);

            // Then
            assertThat(result).isTrue();

            // 인증 코드 삭제 확인
            verify(cacheService).delete("email:" + TEST_EMAIL);

            // 인증 성공 상태 저장 확인
            verify(cacheService).set(
                    eq("verified:email:" + TEST_EMAIL),
                    eq("true"),
                    eq(Duration.ofMinutes(10))
            );

            // 시도 횟수 카운터 삭제 확인
            verify(cacheService).deleteCounter("verify_attempts:email:" + TEST_EMAIL);
            verify(cacheService).deleteCounter("verify_attempts:ip:" + TEST_IP);
        }

        @Test
        @DisplayName("잘못된 인증 코드 검증 - 해시 불일치")
        void verifyCode_InvalidCode_ReturnsFalse() {
            // Given
            String correctCode = "ABC12345";
            String wrongCode = "WRONG123";
            String correctHash = generateExpectedHash(TEST_EMAIL, correctCode);

            given(cacheService.get("email:" + TEST_EMAIL)).willReturn(correctHash);

            // When
            boolean result = emailAuthService.verifyCode(TEST_EMAIL, wrongCode, TEST_IP);

            // Then
            assertThat(result).isFalse();

            // 인증 코드가 삭제되지 않음
            verify(cacheService, never()).delete("email:" + TEST_EMAIL);

            // 시도 횟수 원자적 증가 확인
            verify(cacheService).incrementAndGet(
                    eq("verify_attempts:email:" + TEST_EMAIL),
                    eq(Duration.ofMinutes(10))
            );
            verify(cacheService).incrementAndGet(
                    eq("verify_attempts:ip:" + TEST_IP),
                    eq(Duration.ofMinutes(10))
            );
        }

        @Test
        @DisplayName("만료된 인증 코드 검증 - 실패")
        void verifyCode_ExpiredCode_ReturnsFalse() {
            // Given
            given(cacheService.get("email:" + TEST_EMAIL)).willReturn(null);

            // When
            boolean result = emailAuthService.verifyCode(TEST_EMAIL, "ABC12345", TEST_IP);

            // Then
            assertThat(result).isFalse();

            // 시도 횟수 증가 확인
            verify(cacheService).incrementAndGet(
                    eq("verify_attempts:email:" + TEST_EMAIL),
                    eq(Duration.ofMinutes(10))
            );
        }

        /**
         * 테스트용 해시 생성 헬퍼
         */
        private String generateExpectedHash(String email, String code) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                String combined = email + ":" + code + ":" + VERIFICATION_SECRET;
                byte[] hashBytes = digest.digest(combined.getBytes(java.nio.charset.StandardCharsets.UTF_8));

                StringBuilder hexString = new StringBuilder();
                for (byte b : hashBytes) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                return hexString.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    @DisplayName("시도 횟수 제한 (원자적 카운터)")
    class VerificationAttemptLimits {

        @Test
        @DisplayName("이메일별 5회 시도 후 차단")
        void verifyCode_EmailFiveAttempts_BlocksVerification() {
            // Given
            given(cacheService.getCounter("verify_attempts:email:" + TEST_EMAIL)).willReturn(5L);

            // When & Then
            assertThatThrownBy(() -> emailAuthService.verifyCode(TEST_EMAIL, "ABC12345", TEST_IP))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("인증 시도 횟수를 초과했습니다. 10분 후 다시 시도해주세요.");

            // 인증 코드 조회가 실행되지 않음
            verify(cacheService, never()).get("email:" + TEST_EMAIL);
        }

        @Test
        @DisplayName("IP별 10회 시도 후 차단")
        void verifyCode_IpTenAttempts_BlocksVerification() {
            // Given
            given(cacheService.getCounter("verify_attempts:ip:" + TEST_IP)).willReturn(10L);

            // When & Then
            assertThatThrownBy(() -> emailAuthService.verifyCode(TEST_EMAIL, "ABC12345", TEST_IP))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("인증 시도 횟수를 초과했습니다. 10분 후 다시 시도해주세요.");
        }
    }

    @Nested
    @DisplayName("인증 상태 관리")
    class VerificationStatusManagement {

        @Test
        @DisplayName("인증 상태 확인 - 인증됨")
        void isAlreadyVerified_VerifiedEmail_ReturnsTrue() {
            // Given
            given(cacheService.get("verified:email:" + TEST_EMAIL)).willReturn("true");

            // When
            boolean isVerified = emailAuthService.isAlreadyVerified(TEST_EMAIL);

            // Then
            assertThat(isVerified).isTrue();
        }

        @Test
        @DisplayName("인증 상태 확인 - 인증되지 않음")
        void isAlreadyVerified_NotVerifiedEmail_ReturnsFalse() {
            // Given
            given(cacheService.get("verified:email:" + TEST_EMAIL)).willReturn(null);

            // When
            boolean isVerified = emailAuthService.isAlreadyVerified(TEST_EMAIL);

            // Then
            assertThat(isVerified).isFalse();
        }

        @Test
        @DisplayName("모든 이메일 데이터 삭제")
        void clearAllEmailData_RemovesAllData() {
            // When
            emailAuthService.clearAllEmailData(TEST_EMAIL);

            // Then
            verify(cacheService).delete("email:" + TEST_EMAIL);
            verify(cacheService).delete("verified:email:" + TEST_EMAIL);
        }

        @Test
        @DisplayName("인증 상태만 삭제")
        void clearVerificationStatus_RemovesOnlyStatus() {
            // When
            emailAuthService.clearVerificationStatus(TEST_EMAIL);

            // Then
            verify(cacheService).delete("verified:email:" + TEST_EMAIL);
            verify(cacheService, never()).delete("email:" + TEST_EMAIL);
        }
    }
}