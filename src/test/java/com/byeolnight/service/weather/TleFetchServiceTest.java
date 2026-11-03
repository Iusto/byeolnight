package com.byeolnight.service.weather;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TleFetchServiceTest {
    @TempDir Path directory;
    private HttpClient client;
    private Clock clock;
    private TleFetchService service;
    private static final String TLE = "ISS (ZARYA)\n"
            + "1 25544U 98067A   08264.51782528 -.00002182  00000-0 -11606-4 0  2927\n"
            + "2 25544  51.6416 247.4627 0006703 130.5360 325.0288 15.72125391563537";

    @BeforeEach
    void 준비() {
        client = mock(HttpClient.class);
        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.parse("2008-09-21T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        service = new TleFetchService(client, clock, directory.resolve("iss.tle"));
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> 응답(int status, String body) {
        return mock(HttpResponse.class, invocation -> switch (invocation.getMethod().getName()) {
            case "statusCode" -> status;
            case "body" -> body;
            default -> null;
        });
    }

    @Test
    void 화면조회는_외부요청하지_않는다() {
        assertThat(service.getIssTle()).isNull();
        verifyNoInteractions(client);
    }

    @Test
    void 초기화는_파일만_확인하고_외부요청하지_않는다() {
        service.init();
        assertThat(service.getIssTle()).isNull();
        verifyNoInteractions(client);
    }

    @Test
    void CelesTrak이_정상이면_예비공급원을_호출하지_않는다() throws Exception {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(응답(200, TLE.replace("\n", "\r\n")));
        service.scheduledRefresh();
        assertThat(service.getIssTle()).isNotNull();
        assertThat(service.getLastFetchTime()).isNotNull();
        verify(client, times(1)).send(argThat(r -> r.uri().toString().equals(TleFetchService.CELESTRAK)), any());
        verify(client, never()).send(argThat(r -> r.uri().toString().equals(TleFetchService.AMSAT)), any());
    }

    @Test
    void 오래된_응답은_예비공급원으로_전환한다() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2008-09-24T00:00:00Z"));
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(응답(200, TLE), 응답(200, TLE.replace("08264.51782528", "08267.51782528")));
        // 예비 데이터의 측정 시각보다 늦고 72시간 이내인 현재 시각이다.
        when(clock.instant()).thenReturn(Instant.parse("2008-09-24T13:00:00Z"));
        service.scheduledRefresh();
        assertThat(service.getIssTle()).isNotNull();
        verify(client).send(argThat(r -> r.uri().toString().equals(TleFetchService.AMSAT)), any());
    }

    @Test
    void 연결실패시_AMSAT에서_ISS만_추출하고_재시작시_복원한다() throws Exception {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("연결 시간 초과"))
                .thenReturn(응답(200, "OTHER\n1 12345\n2 12345\n" + TLE));
        service.scheduledRefresh();
        assertThat(service.getIssTle()).isNotNull();
        verify(client).send(argThat(r -> r.uri().toString().equals(TleFetchService.AMSAT)), any());
        TleFetchService restored = new TleFetchService(client, clock, directory.resolve("iss.tle"));
        restored.init();
        assertThat(restored.getIssTle()).isNotNull();
        clearInvocations(client);
        service.scheduledRefresh();
        service.getIssTle();
        verifyNoInteractions(client);
    }

    @Test
    void 모두실패하면_두시간동안_재요청하지_않는다() throws Exception {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("연결 실패"));
        service.scheduledRefresh();
        service.scheduledRefresh();
        service.getIssTle();
        assertThat(service.getIssTle()).isNull();
        verify(client, times(2)).send(any(HttpRequest.class), any());
    }

    @Test
    void HTTP오류_공급원은_자동재요청을_중단한다() throws Exception {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(응답(403, "차단"), 응답(200, TLE));
        service.scheduledRefresh();
        when(clock.instant()).thenReturn(Instant.parse("2008-09-21T13:00:00Z"));
        service.scheduledRefresh();
        verify(client, times(1)).send(argThat(r -> r.uri().toString().equals(TleFetchService.CELESTRAK)), any());
        verify(client, times(2)).send(argThat(r -> r.uri().toString().equals(TleFetchService.AMSAT)), any());
    }

    @Test
    void 측정후_72시간이_지나면_메모리와_디스크_데이터를_사용하지_않는다() throws Exception {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(응답(200, TLE));
        service.scheduledRefresh();
        assertThat(service.getIssTle()).isNotNull();
        when(clock.instant()).thenReturn(Instant.parse("2008-09-24T00:00:00Z"));
        assertThat(service.getIssTle()).isNull();
        TleFetchService restored = new TleFetchService(client, clock, directory.resolve("iss.tle"));
        restored.init();
        assertThat(restored.getIssTle()).isNull();
    }

    @Test
    void 잘못된_응답은_기존_정상파일을_덮어쓰지_않는다() throws Exception {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(응답(200, TLE));
        service.scheduledRefresh();
        String saved = Files.readString(directory.resolve("iss.tle"));
        when(clock.instant()).thenReturn(Instant.parse("2008-09-21T13:00:00Z"));
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(응답(200, "<html>오류</html>"));
        service.scheduledRefresh();
        assertThat(service.getIssTle()).isNotNull();
        assertThat(Files.readString(directory.resolve("iss.tle"))).isEqualTo(saved);
    }
}
