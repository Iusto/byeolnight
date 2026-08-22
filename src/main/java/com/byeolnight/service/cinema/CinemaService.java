package com.byeolnight.service.cinema;

import com.byeolnight.dto.admin.CinemaStatusDto;
import com.byeolnight.dto.cinema.CinemaCollectionResultDto;
import com.byeolnight.dto.cinema.CinemaVideoData;
import com.byeolnight.dto.external.youtube.YouTubeVideoDetailItem;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

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
                .filter(candidate -> CinemaVideoPolicy.passesHardFilters(candidate, cinemaConfig))
                .filter(this::isNewVideo)
                .map(candidate -> new ScoredCandidate(candidate, CinemaVideoPolicy.score(candidate, cinemaConfig)))
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
            if (CinemaVideoPolicy.isSimilarToRecentContent(
                    aiResult.content(), recentCinema, cinemaConfig.getCollection().getSimilarityThreshold())) {
                recentTopicDuplicate = true;
                continue;
            }

            CinemaVideoData data = CinemaContentFactory.create(candidate.video(), aiResult.content());
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

    private boolean isNewVideo(YouTubeVideoDetailItem candidate) {
        if (cinemaRepository.existsByVideoId(candidate.getId())) return false;
        String title = candidate.getSnippet().getTitle();
        return title == null || !cinemaRepository.existsByTitle(title);
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
