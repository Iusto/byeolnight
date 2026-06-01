package com.byeolnight.integration;

import com.byeolnight.controller.admin.AdminUserController;
import com.byeolnight.dto.admin.IpBlockRequestDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.repository.log.AuditSignupLogRepository;
import com.byeolnight.service.auth.SocialAccountCleanupService;
import com.byeolnight.service.user.PointService;
import com.byeolnight.service.user.UserAccountService;
import com.byeolnight.service.user.UserAdminService;
import com.byeolnight.service.user.UserSecurityService;
import com.byeolnight.service.user.WithdrawnUserCleanupService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * IP 차단 회귀 방지 통합 테스트.
 *
 * <p>관리자 수동 차단({@link AdminUserController})과 로그인 자동 차단 검사
 * ({@link UserSecurityService})가 <b>동일한 Redis 키 네임스페이스를 공유</b>하는지
 * 실제 embedded Redis로 검증한다.
 *
 * <p>과거 두 모듈이 서로 다른 접두사(<code>blocked-ip:</code> vs <code>blocked:ip:</code>)를
 * 사용해, 관리자 수동 차단이 실제 로그인을 막지 못하고 자동 차단이 관리자 목록에
 * 노출되지 않던 버그가 있었다. 이 테스트는 그 회귀를 직접 포착한다.
 *
 * <p>두 컴포넌트에 <b>같은</b> {@link StringRedisTemplate} 인스턴스를 주입하므로,
 * 한쪽이 키를 쓰고 다른 쪽이 읽는 실제 상호작용을 검증한다(접두사가 어긋나면 실패).
 */
@DisplayName("IP 차단 Redis 통합 테스트")
class IpBlockRedisIntegrationTest {

    private static final int REDIS_PORT = 16379;

    private static RedisServer redisServer;
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    private AdminUserController adminUserController;
    private UserSecurityService userSecurityService;

    @BeforeAll
    static void startRedis() throws IOException {
        redisServer = new RedisServer(REDIS_PORT);
        redisServer.start();

        connectionFactory = new LettuceConnectionFactory("localhost", REDIS_PORT);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void stopRedis() throws IOException {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        // 매 테스트 격리: 이전 테스트가 남긴 차단 키 제거
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        // 관리자 컨트롤러와 보안 서비스에 '동일한' 실제 RedisTemplate 주입,
        // 나머지 협력자는 이 시나리오에서 쓰이지 않으므로 mock 처리.
        adminUserController = new AdminUserController(
                mock(UserAdminService.class),
                mock(UserAccountService.class),
                redisTemplate,
                mock(PointService.class),
                mock(WithdrawnUserCleanupService.class),
                mock(SocialAccountCleanupService.class)
        );
        userSecurityService = new UserSecurityService(
                mock(AuditSignupLogRepository.class),
                redisTemplate,
                mock(ApplicationContext.class)
        );
    }

    private void blockManually(String ip, long durationMinutes) {
        IpBlockRequestDto dto = new IpBlockRequestDto();
        dto.setIp(ip);
        dto.setDurationMinutes(durationMinutes);
        adminUserController.blockIpManually(dto);
    }

    @SuppressWarnings("ConstantConditions")
    private List<String> getBlockedIps() {
        ResponseEntity<CommonResponse<List<String>>> response = adminUserController.getBlockedIps();
        return response.getBody().getData();
    }

    @Nested
    @DisplayName("관리자 수동 차단 ↔ 로그인 차단 검사 연동")
    class ManualBlockSharesNamespaceWithLoginCheck {

        @Test
        @DisplayName("관리자가 수동 차단한 IP는 로그인 차단 검사(isIpBlocked)에서 차단으로 인식된다")
        void manuallyBlockedIp_isRecognizedByLoginCheck() {
            // given
            String ip = "203.0.113.10";
            assertThat(userSecurityService.isIpBlocked(ip)).isFalse();

            // when - 관리자 수동 차단
            blockManually(ip, 30);

            // then - 로그인 차단 검사가 동일 키를 읽어 차단으로 인식 (접두사 불일치 시 실패)
            assertThat(userSecurityService.isIpBlocked(ip)).isTrue();
        }

        @Test
        @DisplayName("관리자가 차단 해제(unblockIp)하면 로그인 차단 검사에서도 해제된다")
        void unblockedIp_isReleasedFromLoginCheck() {
            // given
            String ip = "203.0.113.11";
            blockManually(ip, 30);
            assertThat(userSecurityService.isIpBlocked(ip)).isTrue();

            // when
            adminUserController.unblockIp(ip);

            // then
            assertThat(userSecurityService.isIpBlocked(ip)).isFalse();
        }

        @Test
        @DisplayName("관리자가 수동 차단한 IP는 차단 목록 조회(getBlockedIps)에 노출된다")
        void manuallyBlockedIp_appearsInBlockedList() {
            // given
            String ip = "203.0.113.12";

            // when
            blockManually(ip, 30);

            // then
            assertThat(getBlockedIps()).contains(ip);
        }
    }

    @Nested
    @DisplayName("로그인 자동 차단 ↔ 관리자 목록/해제 연동")
    class AutoBlockVisibleToAdmin {

        @Test
        @DisplayName("로그인 실패 자동 차단으로 생긴 키는 관리자 목록 조회에 노출된다")
        void autoBlockedIp_appearsInAdminList() {
            // given - 자동 차단(UserSecurityService.handleLoginFailure)이 사용하는 키 형식 재현
            String ip = "198.51.100.20";
            redisTemplate.opsForValue().set("blocked:ip:" + ip, "true");

            // when & then - 관리자 목록 조회가 동일 네임스페이스를 검색해 노출 (접두사 불일치 시 실패)
            assertThat(getBlockedIps()).contains(ip);
        }

        @Test
        @DisplayName("자동 차단된 IP를 관리자가 수동 해제할 수 있다")
        void autoBlockedIp_canBeUnblockedByAdmin() {
            // given
            String ip = "198.51.100.21";
            redisTemplate.opsForValue().set("blocked:ip:" + ip, "true");
            assertThat(userSecurityService.isIpBlocked(ip)).isTrue();

            // when
            adminUserController.unblockIp(ip);

            // then
            assertThat(userSecurityService.isIpBlocked(ip)).isFalse();
            assertThat(getBlockedIps()).doesNotContain(ip);
        }
    }

    @Test
    @DisplayName("잘못된 IP 형식은 차단되지 않고 400 응답을 반환한다")
    void invalidIpFormat_isRejected() {
        // given
        IpBlockRequestDto dto = new IpBlockRequestDto();
        dto.setIp("not-an-ip");
        dto.setDurationMinutes(30);

        // when
        ResponseEntity<CommonResponse<String>> response = adminUserController.blockIpManually(dto);

        // then
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(getBlockedIps()).isEmpty();
    }
}
