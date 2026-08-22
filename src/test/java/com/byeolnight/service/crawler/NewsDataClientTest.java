package com.byeolnight.service.crawler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.byeolnight.dto.ai.NewsApiResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewsDataClientTest {

    private RestTemplate restTemplate;
    private NewsDataClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new NewsDataClient(restTemplate);
        ReflectionTestUtils.setField(client, "primaryApiKey", "newsdata-key");
        ReflectionTestUtils.setField(client, "backupApiKey", "");
    }

    @Test
    @DisplayName("예외 로그에 API 키와 요청 URL을 노출하지 않는다")
    void doesNotLogSecretApiKey() {
        String secret = "TOP_SECRET_API_KEY";
        ReflectionTestUtils.setField(client, "primaryApiKey", secret);
        when(restTemplate.getForObject(anyString(), eq(NewsApiResponseDto.class)))
                .thenThrow(new RuntimeException("https://newsdata.io/api/1/news?apikey=" + secret));

        Logger logger = (Logger) LoggerFactory.getLogger(NewsDataClient.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            client.fetchSpaceNews();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
        assertThat(logs).doesNotContain(secret, "apikey=", "https://newsdata.io");
    }

    @Test
    @DisplayName("429 응답이면 백업 키로 현재 요청을 한 번 재시도한다")
    void retriesWithBackupKeyOnRateLimit() {
        ReflectionTestUtils.setField(client, "backupApiKey", "backup-key");
        HttpClientErrorException rateLimit = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "quota", HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8);
        NewsApiResponseDto empty = new NewsApiResponseDto();
        empty.setStatus("success");
        empty.setResults(List.of());
        when(restTemplate.getForObject(anyString(), eq(NewsApiResponseDto.class)))
                .thenThrow(rateLimit).thenReturn(empty);

        client.fetchSpaceNews();

        verify(restTemplate, times(5)).getForObject(anyString(), eq(NewsApiResponseDto.class));
    }
}
