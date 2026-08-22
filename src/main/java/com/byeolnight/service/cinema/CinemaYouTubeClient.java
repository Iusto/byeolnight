package com.byeolnight.service.cinema;

import com.byeolnight.dto.external.youtube.YouTubeSearchResponse;
import com.byeolnight.dto.external.youtube.YouTubeSnippet;
import com.byeolnight.dto.external.youtube.YouTubeVideoDetailItem;
import com.byeolnight.dto.external.youtube.YouTubeVideoId;
import com.byeolnight.dto.external.youtube.YouTubeVideoItem;
import com.byeolnight.dto.external.youtube.YouTubeVideoListResponse;
import com.byeolnight.dto.video.VideoDto;
import com.byeolnight.infrastructure.config.CinemaCollectionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** YouTube Data API 요청과 응답 변환만 담당한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CinemaYouTubeClient {

    private static final String SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final String VIDEOS_URL = "https://www.googleapis.com/youtube/v3/videos";
    private static final List<List<String>> DAILY_TOPICS = List.of(
            List.of("space rocket launch mission", "우주 발사체 탐사"),
            List.of("solar system planetary exploration", "태양계 행성 탐사"),
            List.of("black hole galaxy cosmology documentary", "블랙홀 은하 우주론"),
            List.of("astronomy telescope observatory", "천체관측 망원경"),
            List.of("NASA ESA space mission", "NASA ESA 우주 임무"),
            List.of("space science documentary", "우주 과학 다큐멘터리"),
            List.of("Korea space astronomy KARI", "한국 우주개발 천문학")
    );

    private final CinemaCollectionProperties cinemaConfig;
    private final RestTemplate restTemplate;

    @Value("${app.security.external-api.ai.google-api-key:}")
    private String googleApiKey;

    public boolean isConfigured() {
        return googleApiKey != null && !googleApiKey.isBlank();
    }

    /** 오늘 요일에 해당하는 검색 주제를 설정된 호출 수만큼 반환한다. */
    public List<String> todayTopics() {
        List<String> topics = DAILY_TOPICS.get(
                LocalDate.now(ZoneId.of("Asia/Seoul")).getDayOfWeek().getValue() - 1);
        return topics.stream()
                .limit(Math.max(1, cinemaConfig.getCollection().getQueriesPerRun()))
                .toList();
    }

    /** search.list 결과의 ID를 모아 videos.list 한 번으로 상세 정보를 조회한다. */
    public List<YouTubeVideoDetailItem> fetchCandidates(List<String> queries) {
        LinkedHashSet<String> videoIds = new LinkedHashSet<>();
        for (String query : queries) {
            YouTubeSearchResponse response = restTemplate.getForObject(
                    buildSearchUri(query, cinemaConfig.getQuality().getMaxResults()),
                    YouTubeSearchResponse.class);
            if (response == null || response.getItems() == null) {
                continue;
            }
            response.getItems().stream()
                    .map(YouTubeVideoItem::getId)
                    .filter(Objects::nonNull)
                    .map(YouTubeVideoId::getVideoId)
                    .filter(id -> id != null && !id.isBlank())
                    .forEach(videoIds::add);
        }
        if (videoIds.isEmpty()) {
            return List.of();
        }

        String detailIds = videoIds.stream().limit(50).collect(Collectors.joining(","));
        URI detailsUri = UriComponentsBuilder.fromHttpUrl(VIDEOS_URL)
                .queryParam("part", "snippet,statistics,contentDetails,status")
                .queryParam("id", detailIds)
                .queryParam("key", googleApiKey)
                .build().encode().toUri();
        YouTubeVideoListResponse response = restTemplate.getForObject(detailsUri, YouTubeVideoListResponse.class);
        return response == null || response.getItems() == null ? List.of() : response.getItems();
    }

    public List<VideoDto> searchSpaceVideos() {
        if (!isConfigured()) {
            return List.of();
        }
        try {
            return searchPublic(todayTopics().getFirst(), 12);
        } catch (Exception exception) {
            log.warn("공개 우주 영상 검색 실패: type={}", exception.getClass().getSimpleName());
            return List.of();
        }
    }

    public List<VideoDto> searchVideosByKeyword(String keyword) {
        if (!isConfigured() || keyword == null || keyword.isBlank()) {
            return List.of();
        }
        try {
            return searchPublic(keyword.strip() + " space", 6);
        } catch (Exception exception) {
            log.warn("키워드 우주 영상 검색 실패: type={}", exception.getClass().getSimpleName());
            return List.of();
        }
    }

    private List<VideoDto> searchPublic(String query, int count) {
        YouTubeSearchResponse response = restTemplate.getForObject(
                buildSearchUri(query, count), YouTubeSearchResponse.class);
        if (response == null || response.getItems() == null) {
            return List.of();
        }
        return response.getItems().stream().map(this::toVideoDto).filter(Objects::nonNull).toList();
    }

    private URI buildSearchUri(String query, int maxResults) {
        String publishedAfter = OffsetDateTime.now(ZoneOffset.UTC)
                .minusYears(cinemaConfig.getYoutube().getPublishedAfterYears())
                .format(DateTimeFormatter.ISO_INSTANT);
        return UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                .queryParam("part", "snippet")
                .queryParam("q", query)
                .queryParam("type", "video")
                .queryParam("maxResults", maxResults)
                .queryParam("order", "relevance")
                .queryParam("publishedAfter", publishedAfter)
                .queryParam("videoDuration", cinemaConfig.getQuality().getVideoDuration())
                .queryParam("videoDefinition", cinemaConfig.getQuality().getVideoDefinition())
                .queryParam("videoEmbeddable", "true")
                .queryParam("videoSyndicated", "true")
                .queryParam("safeSearch", "strict")
                .queryParam("key", googleApiKey)
                .build().encode().toUri();
    }

    private VideoDto toVideoDto(YouTubeVideoItem item) {
        if (item.getId() == null || item.getId().getVideoId() == null || item.getSnippet() == null) {
            return null;
        }
        YouTubeSnippet snippet = item.getSnippet();
        return VideoDto.builder()
                .videoId(item.getId().getVideoId())
                .title(snippet.getTitle())
                .description(snippet.getDescription())
                .thumbnailUrl(snippet.getThumbnails() == null ? null : snippet.getThumbnails().getBestUrl())
                .publishedAt(snippet.getPublishedAt())
                .channelTitle(snippet.getChannelTitle())
                .build();
    }
}
