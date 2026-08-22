package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsApiResponseDto;
import com.byeolnight.entity.News;
import com.byeolnight.infrastructure.config.NewsCollectionProperties;
import com.byeolnight.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsContentValidatorTest {

    @Mock
    private NewsRepository newsRepository;

    private NewsContentValidator validator;
    private NewsCollectionProperties properties;

    @BeforeEach
    void setUp() {
        properties = new NewsCollectionProperties();
        properties.getQuality().setMinTitleLength(10);
        properties.getQuality().setMinDescriptionLength(20);
        properties.getQuality().setMinSpaceKeywords(2);
        properties.getCollection().setSimilarityCheckDays(3);
        properties.getCollection().setSimilarityThreshold(0.7);
        validator = new NewsContentValidator(newsRepository, properties);
    }

    @Test
    @DisplayName("우주 배경 게임 기사는 우주 키워드가 있어도 제외한다")
    void 우주_배경_게임_기사_제외() {
        NewsApiResponseDto.Result result = 기사(
                "Outer Ring MMO opens a new space temple beta",
                "The space exploration game introduces planets, spacecraft and a player-driven economy.");

        assertThat(validator.isHighQualityNews(result)).isFalse();
    }

    @Test
    @DisplayName("우주 테마 가상자산 기사는 제외한다")
    void 우주_테마_가상자산_기사_제외() {
        NewsApiResponseDto.Result result = 기사(
                "Space token project announces lunar NFT sale",
                "The cryptocurrency project promotes an NFT collection inspired by NASA and Moon missions.");

        assertThat(validator.isHighQualityNews(result)).isFalse();
    }

    @Test
    @DisplayName("복수의 우주 근거가 있는 과학 기사는 통과한다")
    void 실제_우주_과학_기사_통과() {
        when(newsRepository.findByPublishedAtAfter(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        NewsApiResponseDto.Result result = 기사(
                "NASA telescope discovers a nearby exoplanet",
                "NASA researchers used a space telescope to confirm the atmosphere of a nearby exoplanet.");

        assertThat(validator.isHighQualityNews(result)).isTrue();
    }

    @Test
    @DisplayName("game-changing 표현은 게임 기사로 오인하지 않는다")
    void game_changing_표현은_허용() {
        when(newsRepository.findByPublishedAtAfter(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        NewsApiResponseDto.Result result = 기사(
                "Game-changing NASA telescope discovers exoplanet",
                "NASA confirmed that the space telescope found an exoplanet and scheduled follow-up observations.");

        assertThat(validator.isHighQualityNews(result)).isTrue();
    }

    @Test
    @DisplayName("설명이 없으면 기사 본문으로 길이와 관련성을 검증한다")
    void 설명이_없으면_본문_사용() {
        when(newsRepository.findByPublishedAtAfter(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        NewsApiResponseDto.Result result = 기사("NASA telescope confirms exoplanet", null);
        result.setContent("NASA used a space telescope to confirm an exoplanet and plan follow-up observations.");

        assertThat(validator.isHighQualityNews(result)).isTrue();
    }

    @Test
    @DisplayName("설정된 유사도 임계값을 중복 판정에 적용한다")
    void 설정된_유사도_임계값_적용() {
        News existing = News.builder().title("NASA telescope discovers nearby exoplanet today").build();
        when(newsRepository.findByPublishedAtAfter(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(existing));
        NewsApiResponseDto.Result result = 기사(
                "NASA telescope discovers nearby planet today",
                "NASA used a space telescope to observe a nearby planet and prepare follow-up astronomy research.");

        properties.getCollection().setSimilarityThreshold(0.9);
        assertThat(validator.isHighQualityNews(result)).isTrue();

        properties.getCollection().setSimilarityThreshold(0.8);
        assertThat(validator.isHighQualityNews(result)).isFalse();
    }

    @Test
    @DisplayName("동일 수집 배치의 유사한 원문 제목을 중복으로 판정한다")
    void 동일_배치_유사_원문_제목_중복() {
        properties.getCollection().setSimilarityThreshold(0.7);
        NewsApiResponseDto.Result result = 기사(
                "NASA telescope discovers nearby planet today",
                "NASA used a space telescope to observe a nearby planet and prepare follow-up research.");

        boolean duplicate = validator.isSimilarToBatch(
                result, List.of("NASA telescope discovers nearby exoplanet today"));

        assertThat(duplicate).isTrue();
    }

    @Test
    @DisplayName("설정된 유사도 검사 일수로 기존 뉴스 조회 범위를 계산한다")
    void 설정된_유사도_검사_일수_적용() {
        when(newsRepository.findByPublishedAtAfter(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        NewsApiResponseDto.Result result = 기사(
                "NASA telescope discovers another exoplanet",
                "NASA used a space telescope to observe an exoplanet and prepare follow-up astronomy research.");
        LocalDateTime lowerBound = LocalDateTime.now().minusDays(3).minusSeconds(1);

        validator.isHighQualityNews(result);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(newsRepository).findByPublishedAtAfter(captor.capture());
        assertThat(captor.getValue()).isAfter(lowerBound).isBefore(LocalDateTime.now().minusDays(3).plusSeconds(1));
    }

    private NewsApiResponseDto.Result 기사(String title, String description) {
        NewsApiResponseDto.Result result = new NewsApiResponseDto.Result();
        result.setTitle(title);
        result.setDescription(description);
        result.setSourceName("Reuters");
        return result;
    }
}
