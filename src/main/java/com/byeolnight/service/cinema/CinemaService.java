package com.byeolnight.service.cinema;

import com.byeolnight.dto.admin.CinemaStatusDto;
import com.byeolnight.dto.cinema.CinemaAiContentDto;
import com.byeolnight.dto.cinema.CinemaCollectionResultDto;
import com.byeolnight.dto.cinema.CinemaVideoData;
import com.byeolnight.dto.external.youtube.YouTubeSnippet;
import com.byeolnight.dto.external.youtube.YouTubeVideoDetailItem;
import com.byeolnight.dto.external.youtube.YouTubeVideoStatus;
import com.byeolnight.dto.video.VideoDto;
import com.byeolnight.entity.Cinema;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.config.CinemaCollectionProperties;
import com.byeolnight.repository.CinemaRepository;
import com.byeolnight.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 별빛시네마 수집 유스케이스를 조정한다.
 *
 * <p>YouTube와 OpenAI 호출은 각각 전용 클라이언트에 위임하고 트랜잭션을 열지 않는다.
 * 후보가 확정된 뒤 {@link CinemaPersistenceService}만 짧은 저장 트랜잭션을 시작한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CinemaService {

    private static final List<String> SPACE_TERMS = List.of(
            "space", "nasa", "esa", "spacex", "rocket", "astronomy", "telescope", "galaxy", "universe",
            "planet", "mars", "moon", "lunar", "solar", "asteroid", "comet", "black hole", "cosmos",
            "우주", "천문", "로켓", "발사체", "행성", "은하", "블랙홀", "망원경", "태양", "달", "화성"
    );
    private static final List<String> EXCLUDED_TERMS = List.of(
            "music video", "lyrics", "k-pop", "kpop", "gameplay", "gaming", "video game", "space game",
            "trailer", "movie clip", "sponsored", "advertisement", "paid promotion", "product review", "buy now",
            "드라마", "뮤직비디오", "가사", "게임", "협찬", "유료 광고", "제품 리뷰", "할인"
    );
    private static final Set<String> GENERIC_TAGS = Set.of("우주", "천문", "천문학", "과학", "space", "science");

    private final CinemaRepository cinemaRepository;
    private final PostRepository postRepository;
    private final CinemaCollectionProperties cinemaConfig;
    private final CinemaYouTubeClient youtubeClient;
    private final CinemaAiContentService aiContentService;
    private final CinemaPersistenceService persistenceService;

    private final ReentrantLock collectionLock = new ReentrantLock(true);
    private volatile CinemaCollectionResultDto lastResult;

    public CinemaCollectionResultDto createCinemaPostManually(User admin) {
        Objects.requireNonNull(admin, "관리자 정보가 없습니다.");
        return collectAndSaveSpaceVideo(admin);
    }

    /** 같은 서버 안에서 스케줄 실행과 관리자 수동 실행이 겹치지 않도록 단일 실행한다. */
    public CinemaCollectionResultDto collectAndSaveSpaceVideo(User user) {
        collectionLock.lock();
        try {
            return collectAndSaveSpaceVideoUnderLock(user);
        } finally {
            collectionLock.unlock();
        }
    }

    private CinemaCollectionResultDto collectAndSaveSpaceVideoUnderLock(User user) {
        if (hasTodayPost()) {
            return remember(result(CinemaCollectionResultDto.Status.ALREADY_CREATED_TODAY,
                    "오늘의 별빛시네마가 이미 등록되었습니다.", 0, 0, null, null));
        }
        if (!youtubeClient.isConfigured()) {
            return remember(result(CinemaCollectionResultDto.Status.YOUTUBE_API_KEY_MISSING,
                    "YouTube API 키가 설정되지 않았습니다.", 0, 0, null, null));
        }
        if (!aiContentService.isConfigured()) {
            return remember(result(CinemaCollectionResultDto.Status.OPENAI_API_KEY_MISSING,
                    "OpenAI API 키가 설정되지 않았습니다.", 0, 0, null, null));
        }

        List<YouTubeVideoDetailItem> candidates;
        try {
            candidates = youtubeClient.fetchCandidates(youtubeClient.todayTopics());
        } catch (Exception exception) {
            log.warn("별빛시네마 YouTube 후보 수집 실패: type={}", exception.getClass().getSimpleName());
            return remember(result(CinemaCollectionResultDto.Status.YOUTUBE_API_FAILED,
                    "YouTube API 호출에 실패했습니다.", 0, 0, null, null));
        }

        int searched = candidates.size();
        List<Cinema> recentCinema = cinemaRepository.findByCreatedAtAfter(
                LocalDateTime.now().minusDays(cinemaConfig.getCollection().getSimilarityCheckDays()));
        List<ScoredCandidate> valid = candidates.stream()
                .filter(this::passesHardFilters)
                .filter(this::isNewVideo)
                .map(candidate -> new ScoredCandidate(candidate, score(candidate)))
                .filter(candidate -> candidate.score() >= 35)
                .sorted(Comparator.comparingInt(ScoredCandidate::score).reversed())
                .toList();

        boolean aiAttempted = false;
        boolean recentTopicDuplicate = false;
        for (ScoredCandidate candidate : valid.stream()
                .limit(Math.max(1, cinemaConfig.getCollection().getMaxAiAttempts())).toList()) {
            aiAttempted = true;
            CinemaAiContentService.GenerationResult aiResult = aiContentService.generate(candidate.video());
            if (aiResult.content() == null) {
                if (aiResult.terminal()) {
                    return remember(result(aiResult.status(), aiResult.message(), searched, valid.size(), candidate, null));
                }
                continue;
            }
            if (isSimilarToRecentContent(aiResult.content(), recentCinema)) {
                recentTopicDuplicate = true;
                continue;
            }

            CinemaVideoData data = toVideoData(candidate.video(), aiResult.content());
            try {
                // 외부 호출이 모두 끝난 뒤 이 메서드 안에서만 DB 트랜잭션이 열린다.
                persistenceService.save(data, user);
                log.info("별빛시네마 등록 완료: videoId={}, score={}", data.videoId(), candidate.score());
                return remember(result(CinemaCollectionResultDto.Status.CREATED,
                        "별빛시네마 영상이 등록되었습니다.", searched, valid.size(), candidate, data.title()));
            } catch (RuntimeException exception) {
                log.warn("별빛시네마 저장 실패: videoId={}, type={}",
                        data.videoId(), exception.getClass().getSimpleName());
                return remember(result(CinemaCollectionResultDto.Status.SAVE_FAILED,
                        "영상과 게시물을 저장하지 못했습니다.", searched, valid.size(), candidate, data.title()));
            }
        }

        CinemaCollectionResultDto.Status status = recentTopicDuplicate
                ? CinemaCollectionResultDto.Status.RECENT_TOPIC_DUPLICATE
                : aiAttempted
                ? CinemaCollectionResultDto.Status.AI_GENERATION_FAILED
                : CinemaCollectionResultDto.Status.NO_VALID_CANDIDATE;
        String message = recentTopicDuplicate
                ? "최근 큐레이션과 주제가 겹쳐 등록하지 않았습니다."
                : aiAttempted
                ? "후보 영상의 AI 소개 생성에 실패했습니다."
                : "조건을 만족하는 후보 영상이 없습니다.";
        return remember(result(status, message, searched, valid.size(), null, null));
    }

    public List<VideoDto> searchSpaceVideos() {
        return youtubeClient.searchSpaceVideos();
    }

    public List<VideoDto> searchVideosByKeyword(String keyword) {
        return youtubeClient.searchVideosByKeyword(keyword);
    }

    public List<VideoDto> getUniqueSpaceVideos() {
        return searchSpaceVideos();
    }

    public CinemaStatusDto getCinemaStatus() {
        long total = postRepository.countByCategory(Post.Category.STARLIGHT_CINEMA);
        Optional<Post> latest = postRepository.findFirstByCategoryOrderByCreatedAtDesc(Post.Category.STARLIGHT_CINEMA);
        long today = postRepository.countByCategoryAndCreatedAtAfter(Post.Category.STARLIGHT_CINEMA, todayStart());
        CinemaStatusDto.CinemaStatusDtoBuilder builder = CinemaStatusDto.builder()
                .totalCinemaPosts(total)
                .latestPostExists(latest.isPresent())
                .todayPosts(today)
                .googleApiConfigured(youtubeClient.isConfigured())
                .openaiApiConfigured(aiContentService.isConfigured())
                .systemConfig(CinemaStatusDto.SystemConfigDto.builder()
                        .schedulerEnabled(true)
                        .dailyScheduleTime("20:00 (KST)")
                        .retryTimes("20:15, 21:00 (KST)")
                        .maxRetryCount(cinemaConfig.getCollection().getRetryCount())
                        .keywordCount(0)
                        .build())
                .lastExecution(lastResult);
        if (latest.isPresent()) {
            Post post = latest.get();
            long days = ChronoUnit.DAYS.between(post.getCreatedAt(), LocalDateTime.now());
            builder.latestPostTitle(post.getTitle())
                    .lastUpdated(post.getCreatedAt())
                    .daysSinceLastUpdate(days)
                    .systemHealthy(days < 2)
                    .warning(days < 2 ? null : "최근 2일간 등록된 영상이 없습니다.");
        } else {
            builder.daysSinceLastUpdate(-1L)
                    .systemHealthy(false)
                    .warning("등록된 별빛시네마 게시물이 없습니다.");
        }
        builder.statusMessage(today > 0
                ? "오늘의 영상이 등록되었습니다."
                : "오늘의 영상이 아직 등록되지 않았습니다.");
        return builder.build();
    }

    private boolean passesHardFilters(YouTubeVideoDetailItem video) {
        if (video == null || video.getId() == null || video.getSnippet() == null || video.getStatus() == null) {
            return false;
        }
        YouTubeSnippet snippet = video.getSnippet();
        YouTubeVideoStatus status = video.getStatus();
        if (!Boolean.TRUE.equals(status.getEmbeddable()) || !"public".equals(status.getPrivacyStatus())) return false;
        if (status.getUploadStatus() != null && !"processed".equals(status.getUploadStatus())) return false;
        if ("20".equals(snippet.getCategoryId())) return false;
        if (snippet.getTitle() == null
                || snippet.getTitle().length() < cinemaConfig.getQuality().getMinTitleLength()) return false;
        if (snippet.getDescription() == null
                || snippet.getDescription().length() < cinemaConfig.getQuality().getMinDescriptionLength()) return false;
        if (snippet.getChannelId() == null || snippet.getChannelId().isBlank()
                || snippet.getChannelTitle() == null || snippet.getChannelTitle().isBlank()) return false;
        if (!hasValidPublishedAt(snippet.getPublishedAt())) return false;
        if (snippet.getThumbnails() == null || snippet.getThumbnails().getBestUrl() == null
                || snippet.getThumbnails().getBestUrl().isBlank()) return false;
        int seconds = durationSeconds(video);
        if (seconds < cinemaConfig.getQuality().getMinDurationSeconds()
                || seconds > cinemaConfig.getQuality().getMaxDurationSeconds()) return false;
        String text = normalizedText(video);
        if (EXCLUDED_TERMS.stream().anyMatch(text::contains) || relevanceCount(text) < 2) return false;
        return isTrustedChannel(video) || (video.getStatistics() != null
                && video.getStatistics().getViewCountAsLong() >= cinemaConfig.getQuality().getMinViewCount());
    }

    private boolean isNewVideo(YouTubeVideoDetailItem candidate) {
        if (cinemaRepository.existsByVideoId(candidate.getId())) return false;
        String title = candidate.getSnippet().getTitle();
        return title == null || !cinemaRepository.existsByTitle(title);
    }

    private boolean isSimilarToRecentContent(CinemaAiContentDto generated, List<Cinema> recentCinema) {
        Set<String> generatedTags = normalizedTags(generated.getTags());
        return recentCinema.stream().anyMatch(existing -> {
            if (titleSimilarity(generated.getKoreanTitle(), existing.getTitle())
                    >= cinemaConfig.getCollection().getSimilarityThreshold()) return true;
            Set<String> existingTags = normalizedTags(existing.getHashtags() == null
                    ? List.of()
                    : Arrays.asList(existing.getHashtags().split("\\s+")));
            long sharedTags = generatedTags.stream().filter(existingTags::contains).count();
            return generatedTags.size() >= 2 && existingTags.size() >= 2 && sharedTags >= 2;
        });
    }

    private int score(YouTubeVideoDetailItem video) {
        int score = isTrustedChannel(video) ? 35 : 0;
        score += Math.min(25, relevanceCount(normalizedText(video)) * 5);
        long ageDays = Math.max(0, ChronoUnit.DAYS.between(publishedAt(video), LocalDateTime.now()));
        score += ageDays <= 30 ? 15 : ageDays <= 180 ? 10 : 5;
        long views = video.getStatistics() == null ? 0 : video.getStatistics().getViewCountAsLong();
        long likes = video.getStatistics() == null ? 0 : video.getStatistics().getLikeCountAsLong();
        score += views <= 0 ? 0
                : Math.min(15, 5 + (int) Math.min(5, views / 10_000)
                + (int) Math.min(5, likes * 1_000 / views));
        return score + 10;
    }

    private CinemaVideoData toVideoData(YouTubeVideoDetailItem video, CinemaAiContentDto ai) {
        YouTubeSnippet snippet = video.getSnippet();
        String hashtags = ai.getTags().stream()
                .map(tag -> "#" + tag.replace("#", ""))
                .collect(Collectors.joining(" "));
        String url = "https://www.youtube.com/watch?v=" + video.getId();
        return new CinemaVideoData(
                ai.getKoreanTitle(),
                ai.getIntroduction(),
                video.getId(),
                url,
                snippet.getChannelTitle(),
                publishedAt(video),
                ai.getWhySelected(),
                hashtags,
                formatContent(ai, video, url, hashtags));
    }

    private String formatContent(CinemaAiContentDto ai, YouTubeVideoDetailItem video,
                                 String url, String hashtags) {
        String points = ai.getKeyPoints().stream()
                .map(point -> "- " + point)
                .collect(Collectors.joining("\n"));
        return """
                ## 선정 이유
                %s

                ## 영상 소개
                %s

                ## 핵심 포인트
                %s

                ## 추천 대상
                %s

                ## 출처
                채널: %s
                발행일: %s
                원본 영상: %s

                %s

                > 이 소개는 YouTube가 제공한 제목과 설명을 바탕으로 AI가 생성했습니다.
                """.formatted(ai.getWhySelected(), ai.getIntroduction(), points, ai.getRecommendedFor(),
                video.getSnippet().getChannelTitle(), publishedAt(video).toLocalDate(), url, hashtags);
    }

    private Set<String> normalizedTags(Collection<String> tags) {
        return tags.stream()
                .filter(Objects::nonNull)
                .map(tag -> tag.replace("#", "").strip().toLowerCase(Locale.ROOT))
                .filter(tag -> !tag.isBlank() && !GENERIC_TAGS.contains(tag))
                .collect(Collectors.toSet());
    }

    private boolean isTrustedChannel(YouTubeVideoDetailItem video) {
        String[] trustedIds = cinemaConfig.getYoutube().getTrustedChannelIds();
        return trustedIds != null && Arrays.asList(trustedIds).contains(video.getSnippet().getChannelId());
    }

    private String normalizedText(YouTubeVideoDetailItem video) {
        YouTubeSnippet snippet = video.getSnippet();
        return ((snippet.getTitle() == null ? "" : snippet.getTitle()) + " "
                + (snippet.getDescription() == null ? "" : snippet.getDescription()) + " "
                + (snippet.getChannelTitle() == null ? "" : snippet.getChannelTitle()))
                .toLowerCase(Locale.ROOT);
    }

    private int relevanceCount(String text) {
        return (int) SPACE_TERMS.stream().filter(text::contains).count();
    }

    private int durationSeconds(YouTubeVideoDetailItem video) {
        try {
            return (int) Duration.parse(video.getContentDetails().getDuration()).getSeconds();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private LocalDateTime publishedAt(YouTubeVideoDetailItem video) {
        return OffsetDateTime.parse(video.getSnippet().getPublishedAt())
                .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();
    }

    private boolean hasValidPublishedAt(String value) {
        try {
            OffsetDateTime.parse(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private double titleSimilarity(String left, String right) {
        if (left == null || right == null) return 0;
        String normalizedLeft = left.strip().toLowerCase(Locale.ROOT);
        String normalizedRight = right.strip().toLowerCase(Locale.ROOT);
        if (normalizedLeft.equals(normalizedRight)) return 1;
        Set<String> a = Arrays.stream(left.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(word -> word.length() >= 2).collect(Collectors.toSet());
        Set<String> b = Arrays.stream(right.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(word -> word.length() >= 2).collect(Collectors.toSet());
        if (a.isEmpty() || b.isEmpty()) return 0;
        long common = a.stream().filter(b::contains).count();
        return (double) common / Math.max(a.size(), b.size());
    }

    private boolean hasTodayPost() {
        return postRepository.countByCategoryAndCreatedAtAfter(
                Post.Category.STARLIGHT_CINEMA, todayStart()) > 0;
    }

    private LocalDateTime todayStart() {
        return LocalDate.now(ZoneId.of("Asia/Seoul")).atStartOfDay();
    }

    private CinemaCollectionResultDto remember(CinemaCollectionResultDto result) {
        lastResult = result;
        return result;
    }

    private CinemaCollectionResultDto result(CinemaCollectionResultDto.Status status, String message,
                                              int searched, int valid, ScoredCandidate candidate, String title) {
        return CinemaCollectionResultDto.builder()
                .status(status)
                .message(message)
                .searchedCandidates(searched)
                .validCandidates(valid)
                .selectedVideoId(candidate == null ? null : candidate.video().getId())
                .selectedTitle(title)
                .selectedScore(candidate == null ? null : candidate.score())
                .executedAt(LocalDateTime.now())
                .build();
    }

    private record ScoredCandidate(YouTubeVideoDetailItem video, int score) {
    }
}
