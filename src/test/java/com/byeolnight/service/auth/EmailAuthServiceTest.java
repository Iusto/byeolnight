package com.byeolnight.service.auth;

import com.byeolnight.infrastructure.cache.RedissonCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * 1. 인증 코드 생성 및 전송
 * 2. 인증 코드 검증 (성공/실패)
 * 3. 시도 횟수 제한 (이메일별 5회, IP별 10회)
 * 4. 인증 상태 관리 (Redis 캐시)
 * 5. 재시도 로직 및 예외 처리
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailAuthService 테스트")
class EmailAuthServiceTest {

    @InjectMocks
    private EmailAuthService emailAuthService;

    @Mock
    private RedissonCacheService cacheService;

    @Mock
    private GmailEmailService gmailEmailService;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_IP = "192.168.1.1";
    private final String VALID_CODE = "ABC12345";

    @BeforeEach
    void setUp() {
        // 기본적으로 인증되지 않은 상태로 설정
        given(cacheService.get("verified:email:" + TEST_EMAIL)).willReturn(null);
    }

    @Nested
    @DisplayName("인증 코드 전송")
    class SendVerificationCode {

        @Test
        @DisplayName("정상 인증 코드 전송 - Redis 저장 및 이메일 발송")
        void sendCode_Success_StoresCodeAndSendsEmail() throws Exception {
            // Given
            doNothing().when(gmailEmailService).sendHtml(eq(TEST_EMAIL), anyString(), anyString());

            // When
            emailAuthService.sendCode(TEST_EMAIL);

            // Then
            // Redis에 인증 코드 저장 확인 (5분 TTL)
            verify(cacheService).set(
                eq("email:" + TEST_EMAIL),
                matches("[A-Z0-9]{8}"), // 8자리 영숫자 패턴
                eq(Duration.ofMinutes(5))
            );

            // HTML 이메일 전송 확인
            verify(gmailEmailService).sendHtml(
                eq(TEST_EMAIL),
                eq("[별 헤는 밤] 이메일 인증 코드"),
                contains("인증 코드")
            );
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

            // 이메일 전송이 호출되지 않음을 확인
            verify(gmailEmailService, never()).sendHtml(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("이메일 전송 실패 후 재시도 성공")
        void sendCode_EmailFailsThenSucceeds_RetriesAndSucceeds() throws Exception {
            // Given
            doThrow(new RuntimeException("SMTP 연결 실패"))
                    .doNothing()
                    .when(gmailEmailService).sendHtml(eq(TEST_EMAIL), anyString(), anyString());

            // When
            emailAuthService.sendCode(TEST_EMAIL);

            // Then
            // 2번 시도 확인 (첫 번째 실패, 두 번째 성공)
            verify(gmailEmailService, times(2)).sendHtml(eq(TEST_EMAIL), anyString(), anyString());
        }

        @Test
        @DisplayName("이메일 전송 3회 모두 실패 - 예외 발생")
        void sendCode_EmailFailsAllRetries_ThrowsException() throws Exception {
            // Given
            doThrow(new RuntimeException("SMTP 서버 오류"))
                    .when(gmailEmailService).sendHtml(eq(TEST_EMAIL), anyString(), anyString());

            // When & Then
            assertThatThrownBy(() -> emailAuthService.sendCode(TEST_EMAIL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("이메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요.");

            // 3번 재시도 확인
            verify(gmailEmailService, times(3)).sendHtml(eq(TEST_EMAIL), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("인증 코드 검증")
    class VerifyCode {

        @Test
        @DisplayName("올바른 인증 코드 검증 - 성공 및 상태 저장")
        void verifyCode_ValidCode_ReturnsTrue() {
            // Given
            given(cacheService.get("email:" + TEST_EMAIL)).willReturn(VALID_CODE);
            given(cacheService.get("verify_attempts:email:" + TEST_EMAIL)).willReturn(null);
            given(cacheService.get("verify_attempts:ip:" + TEST_IP)).willReturn(null);

            // When
            boolean result = emailAuthService.verifyCode(TEST_EMAIL, VALID_CODE, TEST_IP);

            // Then
            assertThat(result).isTrue();

            // 인증 코드 삭제 확인
            verify(cacheService).delete("email:" + TEST_EMAIL);

            // 인증 성공 상태 저장 확인 (10분 TTL)
            verify(cacheService).set(
                eq("verified:email:" + TEST_EMAIL),
                eq("true"),
                eq(Duration.ofMinutes(10))
            );

            // 시도 횟수 초기화 확인
            verify(cacheService).delete("verify_attempts:email:" + TEST_EMAIL);
            verify(cacheService).delete("verify_attempts:ip:" + TEST_IP);
        }

        @Test
        @DisplayName("잘못된 인증 코드 검증 - 실패 및 시도 횟수 증가")
        void verifyCode_InvalidCode_ReturnsFalse() {
            // Given
            given(cacheService.get("email:" + TEST_EMAIL)).willReturn(VALID_CODE);
            given(cacheService.get("verify_attempts:email:" + TEST_EMAIL)).willReturn(null);
            given(cacheService.get("verify_attempts:ip:" + TEST_IP)).willReturn(null);

            // When
            boolean result = emailAuthService.verifyCode(TEST_EMAIL, "WRONG123", TEST_IP);

            // Then
            assertThat(result).isFalse();

            // 인증 코드가 삭제되지 않음을 확인
            verify(cacheService, never()).delete("email:" + TEST_EMAIL);

            // 시도 횟수 증가 확인
            verify(cacheService).set(
                eq("verify_attempts:email:" + TEST_EMAIL),
                eq(1),
                eq(Duration.ofMinutes(10))
            );
            verify(cacheService).set(
                eq("verify_attempts:ip:" + TEST_IP),
                eq(1),
                eq(Duration.ofMinutes(10))
            );
        }

        @Test
        @DisplayName("만료된 인증 코드 검증 - 실패")
        void verifyCode_ExpiredCode_ReturnsFalse() {
            // Given
            given(cacheService.get("email:" + TEST_EMAIL)).willReturn(null); // 만료로 인한 null
            given(cacheService.get("verify_attempts:email:" + TEST_EMAIL)).willReturn(null);
            given(cacheService.get("verify_attempts:ip:" + TEST_IP)).willReturn(null);

            // When
            boolean result = emailAuthService.verifyCode(TEST_EMAIL, VALID_CODE, TEST_IP);

            // Then
            assertThat(result).isFalse();

            // 시도 횟수 증가 확인
            verify(cacheService).set(
                eq("verify_attempts:email:" + TEST_EMAIL),
                eq(1),
                eq(Duration.ofMinutes(10))
            );
        }
    }

    @Nested
    @DisplayName("시도 횟수 제한")
    class VerificationAttemptLimits {

        @Test
        @DisplayName("이메일별 5회 시도 후 차단")
        void verifyCode_EmailFiveAttempts_BlocksVerification() {
            // Given
            given(cacheService.get("verify_attempts:email:" + TEST_EMAIL)).willReturn(5);
            given(cacheService.get("verify_attempts:ip:" + TEST_IP)).willReturn(null);

            // When & Then
            assertThatThrownBy(() -> emailAuthService.verifyCode(TEST_EMAIL, VALID_CODE, TEST_IP))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("인증 시도 횟수를 초과했습니다. 10분 후 다시 시도해주세요.");

            // 인증 코드 조회가 실행되지 않음을 확인
            verify(cacheService, never()).get("email:" + TEST_EMAIL);
        }

        @Test
        @DisplayName("IP별 10회 시도 후 차단")
        void verifyCode_IpTenAttempts_BlocksVerification() {
            // Given
            given(cacheService.get("verify_attempts:email:" + TEST_EMAIL)).willReturn(null);
            given(cacheService.get("verify_attempts:ip:" + TEST_IP)).willReturn(10);

            // When & Then
            assertThatThrownBy(() -> emailAuthService.verifyCode(TEST_EMAIL, VALID_CODE, TEST_IP))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("인증 시도 횟수를 초과했습니다. 10분 후 다시 시도해주세요.");
        }

        @Test
        @DisplayName("시도 횟수 누적 증가 - 2번째 실패")
        void verifyCode_SecondFailure_IncrementsAttempts() {
            // Given
            given(cacheService.get("email:" + TEST_EMAIL)).willReturn(VALID_CODE);
            given(cacheService.get("verify_attempts:email:" + TEST_EMAIL)).willReturn(1); // 이미 1회 실패
            given(cacheService.get("verify_attempts:ip:" + TEST_IP)).willReturn(3); // 이미 3회 실패

            // When
            boolean result = emailAuthService.verifyCode(TEST_EMAIL, "WRONG123", TEST_IP);

            // Then
            assertThat(result).isFalse();

            // 시도 횟수 증가 확인 (1 -> 2, 3 -> 4)
            verify(cacheService).set(
                eq("verify_attempts:email:" + TEST_EMAIL),
                eq(2),
                eq(Duration.ofMinutes(10))
            );
            verify(cacheService).set(
                eq("verify_attempts:ip:" + TEST_IP),
                eq(4),
                eq(Duration.ofMinutes(10))
            );
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

    @Nested
    @DisplayName("엣지 케이스 처리")
    class EdgeCaseHandling {

        @Test
        @DisplayName("빈 문자열 인증 코드 검증")
        void verifyCode_EmptyCode_ReturnsFalse() {
            // Given
            given(cacheService.get("email:" + TEST_EMAIL)).willReturn(VALID_CODE);
            given(cacheService.get("verify_attempts:email:" + TEST_EMAIL)).willReturn(null);
            given(cacheService.get("verify_attempts:ip:" + TEST_IP)).willReturn(null);

            // When
            boolean result = emailAuthService.verifyCode(TEST_EMAIL, "", TEST_IP);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null 인증 코드 검증")
        void verifyCode_NullCode_ReturnsFalse() {
            // Given
            given(cacheService.get("email:" + TEST_EMAIL)).willReturn(VALID_CODE);
            given(cacheService.get("verify_attempts:email:" + TEST_EMAIL)).willReturn(null);
            given(cacheService.get("verify_attempts:ip:" + TEST_IP)).willReturn(null);

            // When
            boolean result = emailAuthService.verifyCode(TEST_EMAIL, null, TEST_IP);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("대소문자 구분 검증")
        void verifyCode_CaseSensitive_ReturnsFalse() {
            // Given
            given(cacheService.get("email:" + TEST_EMAIL)).willReturn("ABC12345");
            given(cacheService.get("verify_attempts:email:" + TEST_EMAIL)).willReturn(null);
            given(cacheService.get("verify_attempts:ip:" + TEST_IP)).willReturn(null);

            // When
            boolean result = emailAuthService.verifyCode(TEST_EMAIL, "abc12345", TEST_IP);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("공백 포함 인증 코드 검증")
        void verifyCode_CodeWithSpaces_ReturnsFalse() {
            // Given
            given(cacheService.get("email:" + TEST_EMAIL)).willReturn(VALID_CODE);
            given(cacheService.get("verify_attempts:email:" + TEST_EMAIL)).willReturn(null);
            given(cacheService.get("verify_attempts:ip:" + TEST_IP)).willReturn(null);

            // When
            boolean result = emailAuthService.verifyCode(TEST_EMAIL, " " + VALID_CODE + " ", TEST_IP);

            // Then
            assertThat(result).isFalse();
        }
    }
}