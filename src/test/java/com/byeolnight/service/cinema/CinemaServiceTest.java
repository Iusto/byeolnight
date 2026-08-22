package com.byeolnight.service.cinema;

import com.byeolnight.dto.cinema.CinemaCollectionResultDto;
import com.byeolnight.dto.external.openai.OpenAiChatResponse;
import com.byeolnight.dto.external.youtube.YouTubeSearchResponse;
import com.byeolnight.dto.external.youtube.YouTubeVideoListResponse;
import com.byeolnight.entity.Cinema;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.config.CinemaCollectionProperties;
import com.byeolnight.repository.CinemaRepository;
import com.byeolnight.repository.post.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CinemaServiceTest {
    @Mock CinemaRepository cinemaRepository;
    @Mock PostRepository postRepository;
    @Mock RestTemplate restTemplate;
    @Mock User user;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CinemaService service;
    private CinemaYouTubeClient youtubeClient;
    private CinemaAiContentService aiContentService;

    @BeforeEach
    void setUp() {
        CinemaCollectionProperties properties = new CinemaCollectionProperties();
        properties.getCollection().setQueriesPerRun(2);
        properties.getCollection().setSimilarityThreshold(0.8);
        properties.getYoutube().setTrustedChannelIds(new String[]{"trusted-channel"});
        youtubeClient = new CinemaYouTubeClient(properties, restTemplate);
        aiContentService = new CinemaAiContentService(restTemplate, objectMapper);
        CinemaPersistenceService persistenceService = new CinemaPersistenceService(cinemaRepository, postRepository);
        service = new CinemaService(
                cinemaRepository, postRepository, properties,
                youtubeClient, aiContentService, persistenceService);
        ReflectionTestUtils.setField(youtubeClient, "googleApiKey", "youtube-secret");
        ReflectionTestUtils.setField(aiContentService, "openaiApiKey", "openai-secret");
        lenient().when(postRepository.countByCategoryAndCreatedAtAfter(eq(Post.Category.STARLIGHT_CINEMA), any())).thenReturn(0L);
        lenient().when(cinemaRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("API 키가 없으면 가짜 영상이나 게시물을 저장하지 않는다")
    void missingApiKeyDoesNotSaveFallback() {
        ReflectionTestUtils.setField(youtubeClient, "googleApiKey", "");

        CinemaCollectionResultDto result = service.collectAndSaveSpaceVideo(user);

        assertThat(result.getStatus()).isEqualTo(CinemaCollectionResultDto.Status.YOUTUBE_API_KEY_MISSING);
        verifyNoInteractions(restTemplate);
        verify(cinemaRepository, never()).save(any(Cinema.class));
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("OpenAI API 키가 없으면 YouTube 쿼터를 사용하지 않고 원인을 반환한다")
    void missingOpenAiApiKeyDoesNotSearchYoutube() {
        ReflectionTestUtils.setField(aiContentService, "openaiApiKey", "");

        CinemaCollectionResultDto result = service.collectAndSaveSpaceVideo(user);

        assertThat(result.getStatus()).isEqualTo(CinemaCollectionResultDto.Status.OPENAI_API_KEY_MISSING);
        verifyNoInteractions(restTemplate);
        verify(cinemaRepository, never()).save(any(Cinema.class));
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("검색 후보 상세 정보는 videos.list 한 번으로 일괄 조회한다")
    void fetchesVideoDetailsInSingleBatch() throws Exception {
        stubYoutubeResponses(singleCandidateDetails("video-1"));
        stubOpenAi();

        CinemaCollectionResultDto result = service.collectAndSaveSpaceVideo(user);

        assertThat(result.getStatus()).isEqualTo(CinemaCollectionResultDto.Status.CREATED);
        verify(restTemplate, times(3)).getForObject(any(URI.class), any(Class.class));
        verify(cinemaRepository).save(any(Cinema.class));
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("첫 후보가 중복이면 AI 호출 없이 두 번째 후보를 저장한다")
    void skipsDuplicateBeforeAiAndSavesSecondCandidate() throws Exception {
        stubYoutubeResponses(twoCandidateDetails());
        when(cinemaRepository.existsByVideoId("duplicate-video")).thenReturn(true);
        when(cinemaRepository.existsByVideoId("fresh-video")).thenReturn(false);
        stubOpenAi();

        CinemaCollectionResultDto result = service.collectAndSaveSpaceVideo(user);

        assertThat(result.getStatus()).isEqualTo(CinemaCollectionResultDto.Status.CREATED);
        assertThat(result.getSelectedVideoId()).isEqualTo("fresh-video");
        verify(restTemplate, times(1)).exchange(eq("https://api.openai.com/v1/chat/completions"), any(), any(), eq(OpenAiChatResponse.class));
        verify(cinemaRepository).save(argThat(cinema -> cinema.getVideoId().equals("fresh-video")));
    }

    @Test
    @DisplayName("상위 후보의 AI 생성이 실패하면 다음 후보를 시도한다")
    void triesNextCandidateWhenAiGenerationFails() throws Exception {
        stubYoutubeResponses(twoCandidateDetails());
        OpenAiChatResponse incomplete = objectMapper.readValue("""
                {"choices":[{"message":{"role":"assistant","content":"{}"}}]}
                """, OpenAiChatResponse.class);
        OpenAiChatResponse complete = completeOpenAiResponse();
        when(restTemplate.exchange(eq("https://api.openai.com/v1/chat/completions"), any(), any(), eq(OpenAiChatResponse.class)))
                .thenReturn(ResponseEntity.ok(incomplete), ResponseEntity.ok(complete));

        CinemaCollectionResultDto result = service.collectAndSaveSpaceVideo(user);

        assertThat(result.getStatus()).isEqualTo(CinemaCollectionResultDto.Status.CREATED);
        verify(restTemplate, times(2)).exchange(eq("https://api.openai.com/v1/chat/completions"), any(), any(), eq(OpenAiChatResponse.class));
    }

    @Test
    @DisplayName("최근 큐레이션과 제목이 겹치면 다음 후보를 저장한다")
    void triesNextCandidateWhenGeneratedTopicIsDuplicate() throws Exception {
        stubYoutubeResponses(twoCandidateDetails());
        Cinema recent = Cinema.builder().title("화성 탐사 임무의 핵심").videoId("old-video")
                .videoUrl("https://www.youtube.com/watch?v=old-video").hashtags("#화성 #탐사").build();
        when(cinemaRepository.findByCreatedAtAfter(any())).thenReturn(List.of(recent));
        OpenAiChatResponse duplicate = objectMapper.readValue("""
                {"choices":[{"message":{"role":"assistant","content":"{\\"koreanTitle\\":\\"화성 탐사 임무의 핵심\\",\\"introduction\\":\\"공식 설명을 바탕으로 화성 탐사 임무를 소개합니다.\\",\\"whySelected\\":\\"화성 탐사의 의미를 이해하기 좋은 자료입니다.\\",\\"keyPoints\\":[\\"탐사 목표를 확인합니다.\\",\\"임무의 의미를 살펴봅니다.\\"],\\"recommendedFor\\":\\"화성 탐사에 관심 있는 분께 추천합니다.\\",\\"tags\\":[\\"화성\\",\\"탐사\\"]}"}}]}
                """, OpenAiChatResponse.class);
        when(restTemplate.exchange(eq("https://api.openai.com/v1/chat/completions"), any(), any(), eq(OpenAiChatResponse.class)))
                .thenReturn(ResponseEntity.ok(duplicate), ResponseEntity.ok(completeOpenAiResponse()));

        CinemaCollectionResultDto result = service.collectAndSaveSpaceVideo(user);

        assertThat(result.getStatus()).isEqualTo(CinemaCollectionResultDto.Status.CREATED);
        assertThat(result.getSelectedTitle()).isEqualTo("우주 망원경이 밝히는 은하의 비밀");
        verify(restTemplate, times(2)).exchange(eq("https://api.openai.com/v1/chat/completions"), any(), any(), eq(OpenAiChatResponse.class));
    }

    @Test
    @DisplayName("OpenAI 한도 초과는 다음 후보를 반복 호출하지 않고 원인을 반환한다")
    void stopsImmediatelyWhenOpenAiQuotaIsExceeded() throws Exception {
        stubYoutubeResponses(twoCandidateDetails());
        when(restTemplate.exchange(eq("https://api.openai.com/v1/chat/completions"), any(), any(), eq(OpenAiChatResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        CinemaCollectionResultDto result = service.collectAndSaveSpaceVideo(user);

        assertThat(result.getStatus()).isEqualTo(CinemaCollectionResultDto.Status.OPENAI_QUOTA_OR_RATE_LIMIT);
        assertThat(result.getMessage()).contains("크레딧");
        verify(restTemplate, times(1)).exchange(eq("https://api.openai.com/v1/chat/completions"), any(), any(), eq(OpenAiChatResponse.class));
        verify(cinemaRepository, never()).save(any());
    }

    @Test
    @DisplayName("임베드 불가능한 영상은 AI 호출 전에 제외한다")
    void rejectsNonEmbeddableBeforeAi() throws Exception {
        stubYoutubeResponses(singleCandidateDetails("blocked-video").replace("\"embeddable\":true", "\"embeddable\":false"));

        CinemaCollectionResultDto result = service.collectAndSaveSpaceVideo(user);

        assertThat(result.getStatus()).isEqualTo(CinemaCollectionResultDto.Status.NO_VALID_CANDIDATE);
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(OpenAiChatResponse.class));
        verify(cinemaRepository, never()).save(any());
    }

    @Test
    @DisplayName("게임 카테고리 영상은 우주 키워드가 있어도 제외한다")
    void rejectsGamingCategoryBeforeAi() throws Exception {
        stubYoutubeResponses(singleCandidateDetails("game-video").replace("\"categoryId\":\"28\"", "\"categoryId\":\"20\""));

        CinemaCollectionResultDto result = service.collectAndSaveSpaceVideo(user);

        assertThat(result.getStatus()).isEqualTo(CinemaCollectionResultDto.Status.NO_VALID_CANDIDATE);
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(OpenAiChatResponse.class));
    }

    @Test
    @DisplayName("썸네일이 없는 영상은 저장 후보에서 제외한다")
    void rejectsVideoWithoutThumbnail() throws Exception {
        stubYoutubeResponses(singleCandidateDetails("no-thumbnail").replace(",\"thumbnails\":{\"high\":{\"url\":\"https://img.example/video.jpg\"}}", ""));

        CinemaCollectionResultDto result = service.collectAndSaveSpaceVideo(user);

        assertThat(result.getStatus()).isEqualTo(CinemaCollectionResultDto.Status.NO_VALID_CANDIDATE);
        verify(cinemaRepository, never()).save(any());
    }

    @Test
    @DisplayName("오늘 게시물이 있으면 Cinema 테이블과 무관하게 수집을 건너뛴다")
    void usesPostAsTodaySourceOfTruth() {
        when(postRepository.countByCategoryAndCreatedAtAfter(eq(Post.Category.STARLIGHT_CINEMA), any())).thenReturn(1L);

        CinemaCollectionResultDto result = service.collectAndSaveSpaceVideo(user);

        assertThat(result.getStatus()).isEqualTo(CinemaCollectionResultDto.Status.ALREADY_CREATED_TODAY);
        verifyNoInteractions(restTemplate);
    }

    private void stubYoutubeResponses(String detailsJson) throws Exception {
        YouTubeSearchResponse search = objectMapper.readValue("""
                {"items":[{"id":{"videoId":"duplicate-video"},"snippet":{"title":"NASA Space Mars Mission","description":"NASA space Mars exploration documentary","channelId":"trusted-channel"}},
                {"id":{"videoId":"fresh-video"},"snippet":{"title":"ESA Galaxy Telescope","description":"ESA space astronomy galaxy telescope documentary","channelId":"trusted-channel"}}]}
                """, YouTubeSearchResponse.class);
        YouTubeVideoListResponse details = objectMapper.readValue(detailsJson, YouTubeVideoListResponse.class);
        when(restTemplate.getForObject(any(URI.class), any(Class.class))).thenAnswer(invocation -> {
            URI uri = invocation.getArgument(0);
            return uri.getPath().endsWith("/videos") ? details : search;
        });
    }

    private void stubOpenAi() throws Exception {
        OpenAiChatResponse response = completeOpenAiResponse();
        when(restTemplate.exchange(eq("https://api.openai.com/v1/chat/completions"), any(), any(), eq(OpenAiChatResponse.class)))
                .thenReturn(ResponseEntity.ok(response));
    }

    private OpenAiChatResponse completeOpenAiResponse() throws Exception {
        return objectMapper.readValue("""
                {"choices":[{"message":{"role":"assistant","content":"{\\"koreanTitle\\":\\"우주 망원경이 밝히는 은하의 비밀\\",\\"introduction\\":\\"공식 설명을 바탕으로 우주 망원경과 은하 탐사를 소개합니다.\\",\\"whySelected\\":\\"신뢰할 수 있는 우주 기관의 최신 탐사 내용을 이해하기 좋습니다.\\",\\"keyPoints\\":[\\"망원경의 관측 목표를 확인합니다.\\",\\"은하 연구의 의미를 살펴봅니다.\\"],\\"recommendedFor\\":\\"천문학과 우주 탐사에 관심 있는 분께 추천합니다.\\",\\"tags\\":[\\"우주\\",\\"천문학\\"]}"}}]}
                """, OpenAiChatResponse.class);
    }

    private String singleCandidateDetails(String id) {
        return """
                {"items":[{"id":"%s","snippet":{"title":"NASA Mars Space Mission","description":"NASA space Mars exploration mission documentary from the official space agency","channelTitle":"NASA","channelId":"trusted-channel","categoryId":"28","publishedAt":"2026-08-20T10:00:00Z","thumbnails":{"high":{"url":"https://img.example/video.jpg"}}},"statistics":{"viewCount":"20000","likeCount":"1000"},"contentDetails":{"duration":"PT12M"},"status":{"privacyStatus":"public","embeddable":true,"uploadStatus":"processed"}}]}
                """.formatted(id);
    }

    private String twoCandidateDetails() {
        return """
                {"items":[
                {"id":"duplicate-video","snippet":{"title":"NASA Mars Space Mission","description":"NASA space Mars exploration mission documentary from the official space agency","channelTitle":"NASA","channelId":"trusted-channel","categoryId":"28","publishedAt":"2026-08-20T10:00:00Z","thumbnails":{"high":{"url":"https://img.example/duplicate.jpg"}}},"statistics":{"viewCount":"50000","likeCount":"2000"},"contentDetails":{"duration":"PT12M"},"status":{"privacyStatus":"public","embeddable":true,"uploadStatus":"processed"}},
                {"id":"fresh-video","snippet":{"title":"ESA Galaxy Space Telescope","description":"ESA space astronomy galaxy telescope documentary from the official space agency","channelTitle":"ESA","channelId":"trusted-channel","categoryId":"28","publishedAt":"2026-08-21T10:00:00Z","thumbnails":{"high":{"url":"https://img.example/fresh.jpg"}}},"statistics":{"viewCount":"20000","likeCount":"1000"},"contentDetails":{"duration":"PT14M"},"status":{"privacyStatus":"public","embeddable":true,"uploadStatus":"processed"}}
                ]}
                """;
    }
}
