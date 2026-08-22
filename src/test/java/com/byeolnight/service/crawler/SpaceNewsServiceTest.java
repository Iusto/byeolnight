package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsAiContentDto;
import com.byeolnight.dto.ai.NewsApiResponseDto;
import com.byeolnight.entity.News;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.config.BaseCollectionProperties;
import com.byeolnight.infrastructure.config.NewsCollectionProperties;
import com.byeolnight.repository.NewsRepository;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceNewsServiceTest {

    @Mock private NewsRepository newsRepository;
    @Mock private UserRepository userRepository;
    @Mock private NewsCollectionProperties newsConfig;
    @Mock private NewsContentValidator validator;
    @Mock private NewsTranslationService translationService;
    @Mock private NewsDataClient newsDataClient;
    @Mock private SpaceNewsPersistenceService persistenceService;
    @Mock private User newsBot;

    private SpaceNewsService service;

    @BeforeEach
    void setUp() {
        service = new SpaceNewsService(newsRepository, userRepository, newsConfig, validator,
                translationService, newsDataClient, persistenceService);
    }

    @Test
    @DisplayName("AI 생성 실패 시 저장 서비스가 호출되지 않는다")
    void doesNotSaveWhenAiGenerationFails() {
        NewsApiResponseDto.Result article = article();
        stubCollection(article, 1);
        when(validator.isHighQualityNews(article)).thenReturn(true);
        when(translationService.generateNewsContent(article)).thenReturn(Optional.empty());

        service.collectAndSaveSpaceNews();

        verify(persistenceService, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("검증된 기사 한 건은 AI 생성 후 한 번 저장한다")
    void savesValidatedArticleOnce() {
        NewsApiResponseDto.Result article = article();
        NewsAiContentDto generated = generatedContent();
        stubCollection(article, 1);
        when(validator.isHighQualityNews(article)).thenReturn(true);
        when(translationService.generateNewsContent(article)).thenReturn(Optional.of(generated));
        when(persistenceService.save(article, generated, newsBot))
                .thenReturn(Post.builder().title(generated.getKoreanTitle()).build());

        service.collectAndSaveSpaceNews();

        verify(translationService).generateNewsContent(article);
        verify(persistenceService).save(article, generated, newsBot);
    }

    @Test
    @DisplayName("같은 수집 배치의 유사 기사는 AI 호출 전에 제외한다")
    void skipsSimilarArticleBeforeAi() {
        NewsApiResponseDto.Result article = article();
        stubCollection(article, 2);
        when(validator.isHighQualityNews(article)).thenReturn(true);
        when(validator.isSimilarToBatch(eq(article), anyList()))
                .thenAnswer(invocation -> !((List<?>) invocation.getArgument(1)).isEmpty());
        NewsAiContentDto generated = generatedContent();
        when(translationService.generateNewsContent(article)).thenReturn(Optional.of(generated));
        when(persistenceService.save(article, generated, newsBot))
                .thenReturn(Post.builder().title(generated.getKoreanTitle()).build());

        service.collectAndSaveSpaceNews();

        verify(translationService, times(1)).generateNewsContent(article);
        verify(persistenceService, times(1)).save(article, generated, newsBot);
    }

    private void stubCollection(NewsApiResponseDto.Result article, int maxPosts) {
        BaseCollectionProperties.Collection collection = new BaseCollectionProperties.Collection();
        collection.setMaxPosts(maxPosts);
        when(newsConfig.getCollection()).thenReturn(collection);
        when(newsDataClient.fetchSpaceNews()).thenReturn(response(article));
        when(userRepository.findByEmail("newsbot@byeolnight.com")).thenReturn(Optional.of(newsBot));
    }

    private NewsApiResponseDto.Result article() {
        NewsApiResponseDto.Result article = new NewsApiResponseDto.Result();
        article.setTitle("NASA telescope finds a nearby exoplanet");
        article.setDescription("NASA used a space telescope to identify an exoplanet.");
        article.setLink("https://example.com/nasa-exoplanet");
        article.setSourceName("NASA");
        article.setPubDate("2026-08-22 10:00:00");
        return article;
    }

    private NewsApiResponseDto response(NewsApiResponseDto.Result article) {
        NewsApiResponseDto response = new NewsApiResponseDto();
        response.setStatus("success");
        response.setResults(List.of(article, article));
        return response;
    }

    private NewsAiContentDto generatedContent() {
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
