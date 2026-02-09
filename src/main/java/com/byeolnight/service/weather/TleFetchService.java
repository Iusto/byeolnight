package com.byeolnight.service.weather;

import com.github.amsacode.predict4java.TLE;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CelesTrak에서 ISS TLE(Two-Line Element) 데이터를 가져와 메모리에 캐싱하는 서비스.
 * TLE는 ISS 궤도 정보를 담고 있으며, SGP4 계산의 입력 데이터로 사용됨.
 * 12시간마다 자동 갱신.
 */
@Service
@Slf4j
public class TleFetchService {

    private static final String CELESTRAK_ISS_TLE_URL =
            "https://celestrak.org/NORAD/elements/gp.php?CATNR=25544&FORMAT=TLE";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final AtomicReference<TLE> cachedTle = new AtomicReference<>();
    private volatile LocalDateTime lastFetchTime;

    @PostConstruct
    public void init() {
        refreshTle();
    }

    /**
     * 12시간마다 TLE 자동 갱신.
     * TLE가 없으면 5분마다 재시도, 있으면 12시간 간격 유지.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    public void scheduledRefresh() {
        if (cachedTle.get() != null && lastFetchTime != null
                && java.time.Duration.between(lastFetchTime, LocalDateTime.now()).toHours() < 12) {
            return; // TLE가 유효하면 12시간 전까지 갱신 안 함
        }
        refreshTle();
    }

    /**
     * 캐싱된 TLE를 반환. 없으면 즉시 가져옴.
     */
    public TLE getIssTle() {
        TLE tle = cachedTle.get();
        if (tle == null) {
            refreshTle();
            tle = cachedTle.get();
        }
        return tle;
    }

    public LocalDateTime getLastFetchTime() {
        return lastFetchTime;
    }

    private void refreshTle() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CELESTRAK_ISS_TLE_URL))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body().trim();
                String[] lines = body.split("\\r?\\n");

                if (lines.length >= 3) {
                    // CelesTrak 형식: 이름(line0), TLE line1, TLE line2
                    String[] tleLines = new String[]{
                            lines[0].trim(),
                            lines[1].trim(),
                            lines[2].trim()
                    };
                    TLE tle = new TLE(tleLines);
                    cachedTle.set(tle);
                    lastFetchTime = LocalDateTime.now();
                    log.info("ISS TLE 갱신 성공: {}", tleLines[0]);
                } else {
                    log.error("TLE 데이터 형식 오류: 라인 수 = {}", lines.length);
                }
            } else {
                log.error("CelesTrak TLE 조회 실패: HTTP {}", response.statusCode());
            }
        } catch (Exception e) {
            log.error("TLE 갱신 실패: {}", e.getMessage());
        }
    }
}
