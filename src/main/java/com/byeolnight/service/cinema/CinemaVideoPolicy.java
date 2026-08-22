package com.byeolnight.service.cinema;

import com.byeolnight.dto.cinema.CinemaAiContentDto;
import com.byeolnight.dto.external.youtube.YouTubeSnippet;
import com.byeolnight.dto.external.youtube.YouTubeVideoDetailItem;
import com.byeolnight.dto.external.youtube.YouTubeVideoStatus;
import com.byeolnight.entity.Cinema;
import com.byeolnight.infrastructure.config.CinemaCollectionProperties;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** YouTube 후보의 품질 필터, 점수, 최근 콘텐츠 유사도 정책을 한곳에 모은다. */
final class CinemaVideoPolicy {

    private static final List<String> SPACE_TERMS = List.of(
            "space", "nasa", "esa", "spacex", "rocket", "astronomy", "telescope", "galaxy",
            "universe", "planet", "mars", "moon", "lunar", "solar", "asteroid", "comet",
            "black hole", "cosmos", "우주", "천문", "로켓", "발사체", "행성", "달",
            "블랙홀", "망원경", "태양", "별", "위성");
    private static final List<String> EXCLUDED_TERMS = List.of(
            "music video", "lyrics", "k-pop", "kpop", "gameplay", "gaming", "video game",
            "space game", "trailer", "movie clip", "sponsored", "advertisement", "paid promotion",
            "product review", "buy now", "드라마", "뮤직비디오", "가사", "게임", "협찬",
            "유료 광고", "제품 리뷰", "할인");
    private static final Set<String> GENERIC_TAGS = Set.of(
            "우주", "천문", "천문학", "과학", "space", "science");

    private CinemaVideoPolicy() {
    }

    static boolean passesHardFilters(
            YouTubeVideoDetailItem video,
            CinemaCollectionProperties config
    ) {
        if (video == null || video.getId() == null || video.getSnippet() == null || video.getStatus() == null) {
            return false;
        }
        YouTubeSnippet snippet = video.getSnippet();
        YouTubeVideoStatus status = video.getStatus();
        if (!Boolean.TRUE.equals(status.getEmbeddable()) || !"public".equals(status.getPrivacyStatus())) return false;
        if (status.getUploadStatus() != null && !"processed".equals(status.getUploadStatus())) return false;
        if ("20".equals(snippet.getCategoryId())) return false;
        if (snippet.getTitle() == null || snippet.getTitle().length() < config.getQuality().getMinTitleLength()) return false;
        if (snippet.getDescription() == null || snippet.getDescription().length() < config.getQuality().getMinDescriptionLength()) return false;
        if (snippet.getChannelId() == null || snippet.getChannelId().isBlank()
                || snippet.getChannelTitle() == null || snippet.getChannelTitle().isBlank()) return false;
        if (!hasValidPublishedAt(snippet.getPublishedAt())) return false;
        if (snippet.getThumbnails() == null || snippet.getThumbnails().getBestUrl() == null
                || snippet.getThumbnails().getBestUrl().isBlank()) return false;
        int seconds = durationSeconds(video);
        if (seconds < config.getQuality().getMinDurationSeconds()
                || seconds > config.getQuality().getMaxDurationSeconds()) return false;
        String text = normalizedText(video);
        if (EXCLUDED_TERMS.stream().anyMatch(text::contains) || relevanceCount(text) < 2) return false;
        return isTrustedChannel(video, config) || (video.getStatistics() != null
                && video.getStatistics().getViewCountAsLong() >= config.getQuality().getMinViewCount());
    }

    static int score(YouTubeVideoDetailItem video, CinemaCollectionProperties config) {
        int score = isTrustedChannel(video, config) ? 35 : 0;
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

    static boolean isSimilarToRecentContent(
            CinemaAiContentDto generated,
            List<Cinema> recentCinema,
            double threshold
    ) {
        Set<String> generatedTags = normalizedTags(generated.getTags());
        return recentCinema.stream().anyMatch(existing -> {
            if (titleSimilarity(generated.getKoreanTitle(), existing.getTitle()) >= threshold) return true;
            Set<String> existingTags = normalizedTags(existing.getHashtags() == null
                    ? List.of()
                    : Arrays.asList(existing.getHashtags().split("\\s+")));
            long sharedTags = generatedTags.stream().filter(existingTags::contains).count();
            return generatedTags.size() >= 2 && existingTags.size() >= 2 && sharedTags >= 2;
        });
    }

    static LocalDateTime publishedAt(YouTubeVideoDetailItem video) {
        return OffsetDateTime.parse(video.getSnippet().getPublishedAt())
                .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();
    }

    private static Set<String> normalizedTags(Collection<String> tags) {
        return tags.stream()
                .filter(Objects::nonNull)
                .map(tag -> tag.replace("#", "").strip().toLowerCase(Locale.ROOT))
                .filter(tag -> !tag.isBlank() && !GENERIC_TAGS.contains(tag))
                .collect(Collectors.toSet());
    }

    private static boolean isTrustedChannel(YouTubeVideoDetailItem video, CinemaCollectionProperties config) {
        String[] trustedIds = config.getYoutube().getTrustedChannelIds();
        return trustedIds != null && Arrays.asList(trustedIds).contains(video.getSnippet().getChannelId());
    }

    private static String normalizedText(YouTubeVideoDetailItem video) {
        YouTubeSnippet snippet = video.getSnippet();
        return ((snippet.getTitle() == null ? "" : snippet.getTitle()) + " "
                + (snippet.getDescription() == null ? "" : snippet.getDescription()) + " "
                + (snippet.getChannelTitle() == null ? "" : snippet.getChannelTitle()))
                .toLowerCase(Locale.ROOT);
    }

    private static int relevanceCount(String text) {
        return (int) SPACE_TERMS.stream().filter(text::contains).count();
    }

    private static int durationSeconds(YouTubeVideoDetailItem video) {
        try {
            return (int) Duration.parse(video.getContentDetails().getDuration()).getSeconds();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static boolean hasValidPublishedAt(String value) {
        try {
            OffsetDateTime.parse(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static double titleSimilarity(String left, String right) {
        if (left == null || right == null) return 0;
        String normalizedLeft = left.strip().toLowerCase(Locale.ROOT);
        String normalizedRight = right.strip().toLowerCase(Locale.ROOT);
        if (normalizedLeft.equals(normalizedRight)) return 1;
        Set<String> first = Arrays.stream(left.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(word -> word.length() >= 2).collect(Collectors.toSet());
        Set<String> second = Arrays.stream(right.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(word -> word.length() >= 2).collect(Collectors.toSet());
        if (first.isEmpty() || second.isEmpty()) return 0;
        long common = first.stream().filter(second::contains).count();
        return (double) common / Math.max(first.size(), second.size());
    }
}
