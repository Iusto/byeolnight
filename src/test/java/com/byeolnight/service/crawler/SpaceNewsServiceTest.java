package com.byeolnight.service.crawler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.byeolnight.dto.ai.NewsAiContentDto;
import com.byeolnight.dto.ai.NewsApiResponseDto;
import com.byeolnight.entity.News;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.config.BaseCollectionProperties;
import com.byeolnight.infrastructure.config.NewsCollectionProperties;
import com.byeolnight.repository.NewsRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpaceNewsServiceTest {

    @Mock private NewsRepository newsRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private RestTemplate restTemplate;
    @Mock private NewsCollectionProperties newsConfig;
    @Mock private NewsContentValidator validator;
    @Mock private NewsTranslationService translationService;
    @Mock private NewsContentFormatter formatter;

    @InjectMocks
    private SpaceNewsService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "primaryApiKey", "newsdata-key");
        ReflectionTestUtils.setField(service, "backupApiKey", "");
    }

    @Test
    @DisplayName("AI 생성이 실패하면 News와 Post를 모두 저장하지 않는다")
    void AI_실패_시_게시하지_않음() {
        NewsApiResponseDto.Result article = 기사();
        NewsApiResponseDto response = 응답(article);

        BaseCollectionProperties.Collection collection = new BaseCollectionProperties.Collection();
        collection.setMaxPosts(1);
        when(newsConfig.getCollection()).thenReturn(collection);
        when(restTemplate.getForObject(anyString(), eq(NewsApiResponseDto.class))).thenReturn(response);
        when(userRepository.findByEmail("newsbot@byeolnight.com")).thenReturn(Optional.of(mock(User.class)));
        when(validator.isHighQualityNews(any())).thenReturn(true);
        when(translationService.generateNewsContent(any())).thenReturn(Optional.empty());

        service.collectAndSaveSpaceNews();

        verify(newsRepository, never()).save(any(News.class));
        verify(postRepository, never()).save(any(Post.class));
        verify(formatter, never()).formatNewsContent(any(), any());
    }

    @Test
    @DisplayName("성공한 기사 한 건은 AI를 한 번만 호출하고 News와 Post를 한 번씩 저장한다")
    void 성공_경로는_AI와_저장을_각_한번_수행() {
        NewsApiResponseDto.Result article = 기사();
        BaseCollectionProperties.Collection collection = new BaseCollectionProperties.Collection();
        collection.setMaxPosts(1);
        NewsAiContentDto generated = 생성콘텐츠();
        Post savedPost = Post.builder().title(generated.getKoreanTitle()).build();

        when(newsConfig.getCollection()).thenReturn(collection);
        when(restTemplate.getForObject(anyString(), eq(NewsApiResponseDto.class))).thenReturn(응답(article));
        when(userRepository.findByEmail("newsbot@byeolnight.com")).thenReturn(Optional.of(mock(User.class)));
        when(validator.isHighQualityNews(any())).thenReturn(true);
        when(translationService.generateNewsContent(any())).thenReturn(Optional.of(generated));
        when(formatter.formatHashtags(generated)).thenReturn("#NASA #외계행성");
        when(formatter.formatNewsContent(article, generated)).thenReturn("## 한눈에 보기\n\n요약");
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        service.collectAndSaveSpaceNews();

        verify(translationService, times(1)).generateNewsContent(article);
        verify(newsRepository, times(1)).save(any(News.class));
        verify(postRepository, times(1)).save(any(Post.class));
        verify(formatter, times(1)).formatNewsContent(article, generated);
    }

    @Test
    @DisplayName("동일 수집 배치의 유사 기사는 AI를 중복 호출하지 않는다")
    void 배치_중복은_AI_호출_전에_제외() {
        NewsApiResponseDto.Result article = 기사();
        BaseCollectionProperties.Collection collection = new BaseCollectionProperties.Collection();
        collection.setMaxPosts(2);
        NewsAiContentDto generated = 생성콘텐츠();
        Post savedPost = Post.builder().title(generated.getKoreanTitle()).build();

        when(newsConfig.getCollection()).thenReturn(collection);
        when(restTemplate.getForObject(anyString(), eq(NewsApiResponseDto.class))).thenReturn(응답(article));
        when(userRepository.findByEmail("newsbot@byeolnight.com")).thenReturn(Optional.of(mock(User.class)));
        when(validator.isHighQualityNews(any())).thenReturn(true);
        when(validator.isSimilarToBatch(eq(article), anyList()))
                .thenAnswer(invocation -> !((List<?>) invocation.getArgument(1)).isEmpty());
        when(translationService.generateNewsContent(article)).thenReturn(Optional.of(generated));
        when(formatter.formatHashtags(generated)).thenReturn("#NASA #외계행성");
        when(formatter.formatNewsContent(article, generated)).thenReturn("## 한눈에 보기\n\n요약");
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        service.collectAndSaveSpaceNews();

        verify(translationService, times(1)).generateNewsContent(article);
        verify(newsRepository, times(1)).save(any(News.class));
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("NewsData 예외 메시지와 API 키는 로그에 남기지 않는다")
    void NewsData_비밀정보_로그_차단() {
        String secret = "TOP_SECRET_API_KEY";
        ReflectionTestUtils.setField(service, "primaryApiKey", secret);
        when(restTemplate.getForObject(anyString(), eq(NewsApiResponseDto.class)))
                .thenThrow(new RuntimeException("https://newsdata.io/api/1/news?apikey=" + secret));

        Logger logger = (Logger) LoggerFactory.getLogger(SpaceNewsService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            service.fetchKoreanSpaceNews();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        String logs = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
        assertThat(logs).doesNotContain(secret, "apikey=", "https://newsdata.io");
        assertThat(logs).contains("RuntimeException", "status=N/A");
    }

    @Test
    @DisplayName("NewsData가 429를 반환하면 예외 메시지가 아닌 상태 코드로 백업 키 재시도를 결정한다")
    void NewsData_429_상태코드로_백업키_재시도() {
        ReflectionTestUtils.setField(service, "backupApiKey", "backup-key");
        HttpClientErrorException rateLimit = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "메시지에 quota 표현이 없어도 동작",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );
        NewsApiResponseDto emptyResponse = new NewsApiResponseDto();
        emptyResponse.setStatus("success");
        emptyResponse.setResults(List.of());
        when(restTemplate.getForObject(anyString(), eq(NewsApiResponseDto.class)))
                .thenThrow(rateLimit)
                .thenReturn(emptyResponse);

        service.fetchKoreanSpaceNews();

        verify(restTemplate, times(5)).getForObject(anyString(), eq(NewsApiResponseDto.class));
    }

    private NewsApiResponseDto.Result 기사() {
        NewsApiResponseDto.Result article = new NewsApiResponseDto.Result();
        article.setTitle("NASA telescope finds a nearby exoplanet");
        article.setDescription("NASA used a space telescope to identify an exoplanet for follow-up observations.");
        article.setLink("https://example.com/nasa-exoplanet");
        article.setSourceName("NASA");
        article.setPubDate("2026-08-22 10:00:00");
        return article;
    }

    private NewsApiResponseDto 응답(NewsApiResponseDto.Result article) {
        NewsApiResponseDto response = new NewsApiResponseDto();
        response.setStatus("success");
        response.setResults(List.of(article));
        return response;
    }

    private NewsAiContentDto 생성콘텐츠() {
        NewsAiContentDto content = new NewsAiContentDto();
        content.setKoreanTitle("NASA, 새로운 외계행성 발견");
        content.setOverview("NASA가 새로운 외계행성을 확인했다.");
        content.setKeyFacts(List.of("우주망원경 관측", "후속 관측 필요"));
        content.setWhyItMatters("외계행성 연구 범위를 넓힌다.");
        content.setWatchPoints(List.of("후속 관측 결과"));
        content.setTags(List.of("NASA", "외계행성"));
        return content;
    }
}
