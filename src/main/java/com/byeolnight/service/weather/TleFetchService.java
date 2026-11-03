package com.byeolnight.service.weather;

import com.github.amsacode.predict4java.TLE;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CelesTrak 실패 시 AMSAT으로 전환한다. 화면 요청은 네트워크를 호출하지 않는다.
 * 마지막 정상 TLE는 디스크에 보존하되 측정 시점으로부터 72시간까지만 사용한다.
 */
@Service
@Slf4j
public class TleFetchService {
    static final String CELESTRAK = "https://celestrak.org/NORAD/elements/gp.php?CATNR=25544&FORMAT=TLE";
    static final String AMSAT = "https://www.amsat.org/tle/dailytle.txt";
    private final HttpClient httpClient;
    private final Clock clock;
    private final Path cacheFile;
    private final AtomicReference<Snapshot> cached = new AtomicReference<>();
    private volatile Instant nextAttempt = Instant.MIN;
    private volatile LocalDateTime lastFetchTime;
    // 비정상 HTTP 응답은 운영자 확인 전까지 해당 공급원의 자동 요청을 중단한다.
    private boolean celestrakDisabled;

    @Autowired
    public TleFetchService(@Value("${iss.tle.cache-file:./data/iss/last-normal.tle}") String cacheFile) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(),
                Clock.systemUTC(), Path.of(cacheFile));
    }

    TleFetchService(HttpClient httpClient, Clock clock, Path cacheFile) {
        this.httpClient = httpClient;
        this.clock = clock;
        this.cacheFile = cacheFile;
    }

    @PostConstruct
    public void init() {
        try {
            if (Files.exists(cacheFile)) {
                cached.set(parse(Files.readString(cacheFile)));
                log.info("ISS TLE 디스크 캐시 복원 완료");
            }
        } catch (Exception e) {
            log.warn("ISS TLE 캐시 복원 실패: {}", e.getMessage());
        }
    }

    // 초기 수집도 스케줄러에서 실행하여 외부 연결이 앱 시작을 지연시키지 않는다.
    @Scheduled(fixedDelay = 60_000, initialDelay = 1_000)
    public synchronized void scheduledRefresh() {
        if (clock.instant().isBefore(nextAttempt)) return;
        nextAttempt = clock.instant().plus(Duration.ofHours(2));
        Snapshot result = celestrakDisabled ? null : fetch(CELESTRAK);
        String source = "CelesTrak";
        if (result == null) {
            source = "AMSAT";
            result = fetch(AMSAT);
        }
        if (result == null) return;
        cached.set(result);
        lastFetchTime = LocalDateTime.now(clock);
        nextAttempt = clock.instant().plus(Duration.ofHours(12));
        log.info("ISS TLE 갱신 성공: 공급원={}, 측정시각={}", source, result.epoch());
        try {
            Path target = cacheFile.toAbsolutePath();
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), "iss-", ".tmp");
            try {
                Files.writeString(temporary, result.body());
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (Exception e) {
            log.warn("ISS TLE 디스크 저장 실패: {}", e.getMessage());
        }
    }

    public TLE getIssTle() {
        Snapshot snapshot = cached.get();
        return snapshot != null && usable(snapshot.epoch()) ? snapshot.tle() : null;
    }

    public LocalDateTime getLastFetchTime() {
        return lastFetchTime;
    }

    private Snapshot fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (url.equals(CELESTRAK)) celestrakDisabled = true;
                log.error("ISS TLE 공급원 HTTP 오류: url={}, status={}", url, response.statusCode());
                return null;
            }
            return parse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.warn("ISS TLE 수집 실패: url={}, 원인={}", url, e.getMessage());
            return null;
        }
    }

    private boolean usable(Instant epoch) {
        return !epoch.isAfter(clock.instant().plus(Duration.ofHours(1)))
                && clock.instant().isBefore(epoch.plus(Duration.ofHours(72)));
    }

    private Snapshot parse(String body) {
        String[] lines = body.strip().split("\\R");
        for (int i = 1; i + 1 < lines.length; i++) {
            if (!lines[i].startsWith("1 25544") || !lines[i + 1].startsWith("2 25544")) continue;
            String line1 = lines[i];
            int year = Integer.parseInt(line1.substring(18, 20));
            year += year >= 57 ? 1900 : 2000;
            double day = Double.parseDouble(line1.substring(20, 32));
            Instant epoch = LocalDate.of(year, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant()
                    .plusNanos((long) ((day - 1) * 86_400_000_000_000L));
            if (!usable(epoch)) throw new IllegalArgumentException("ISS TLE 측정 시각이 유효기간 밖입니다: " + epoch);
            String[] tleLines = {lines[i - 1].strip(), line1, lines[i + 1]};
            return new Snapshot(new TLE(tleLines), epoch, String.join("\n", tleLines));
        }
        throw new IllegalArgumentException("응답에 ISS(25544) TLE가 없습니다");
    }

    private record Snapshot(TLE tle, Instant epoch, String body) {}
}
