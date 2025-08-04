package com.byeolnight.service.cinema;

import com.byeolnight.domain.entity.Cinema;
import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.CinemaRepository;
import com.byeolnight.domain.repository.post.PostRepository;
import com.byeolnight.domain.repository.user.UserRepository;

import com.byeolnight.infrastructure.config.CinemaCollectionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CinemaService {

    private final CinemaRepository cinemaRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CinemaCollectionProperties cinemaConfig;
    private final RestTemplate restTemplate;
    
    // 키워드 상수
    private static final String[] KOREAN_KEYWORDS = {"우주", "로켓", "위성", "화성", "달", "태양", "지구", "목성", "토성", "천왕성", "해왕성", "수성", "금성", "명왕성", "블랙홀", "은하", "별", "항성", "혜성", "소행성", "망원경", "천문", "항공우주", "우주선", "우주정거장", "우주비행사", "우주발사", "우주탐사", "성운", "퀘이사", "중성자별", "백색왜성", "적색거성", "초신성", "성단", "성간물질", "암흑물질", "암흑에너지", "빅뱅", "우주론", "외계행성", "외계생명", "SETI", "우주망원경", "허블", "제임스웹", "케플러", "스피처", "찬드라", "컴프턴", "국제우주정거장", "ISS", "아르테미스", "아폴로", "보이저", "카시니", "갈릴레오", "뉴호라이즌스", "파커", "주노", "화성탐사", "달탐사", "목성탐사", "토성탐사", "태양탐사", "소행성탐사", "혜성탐사", "우주쓰레기", "우주날씨", "태양풍", "자기권", "오로라", "일식", "월식", "유성우", "운석", "크레이터", "화산", "대기", "중력", "궤도", "공전", "자전", "조석", "라그랑주점", "중력파", "상대성이론", "양자역학", "끈이론", "다중우주", "우주배경복사", "적색편이", "도플러효과", "허블상수", "우주나이", "우주크기", "관측가능우주", "사건지평선", "특이점", "웜홀"};
    private static final String[] ENGLISH_KEYWORDS = {"space", "rocket", "satellite", "Mars", "Moon", "Sun", "Earth", "Jupiter", "Saturn", "Uranus", "Neptune", "Mercury", "Venus", "Pluto", "blackhole", "galaxy", "star", "stellar", "comet", "asteroid", "telescope", "astronomy", "aerospace", "spacecraft", "space station", "astronaut", "space launch", "space exploration", "nebula", "quasar", "neutron star", "white dwarf", "red giant", "supernova", "cluster", "interstellar", "dark matter", "dark energy", "big bang", "cosmology", "exoplanet", "extraterrestrial", "SETI", "space telescope", "Hubble", "James Webb", "Kepler", "Spitzer", "Chandra", "Compton", "ISS", "International Space Station", "Artemis", "Apollo", "Voyager", "Cassini", "Galileo", "New Horizons", "Parker", "Juno", "Mars exploration", "lunar exploration", "Jupiter mission", "Saturn mission", "solar mission", "asteroid mission", "comet mission", "space debris", "space weather", "solar wind", "magnetosphere", "aurora", "eclipse", "lunar eclipse", "meteor shower", "meteorite", "crater", "volcano", "atmosphere", "gravity", "orbit", "revolution", "rotation", "tidal", "Lagrange point", "gravitational wave", "relativity", "quantum mechanics", "string theory", "multiverse", "cosmic background", "redshift", "Doppler effect", "Hubble constant", "universe age", "universe size", "observable universe", "event horizon", "singularity", "wormhole"};
    private static final String[] ALL_KEYWORDS = java.util.stream.Stream.concat(java.util.Arrays.stream(KOREAN_KEYWORDS), java.util.Arrays.stream(ENGLISH_KEYWORDS)).map(String::toLowerCase).toArray(String[]::new);
    private static final String[] MUSIC_KEYWORDS = {"원위", "onewe", "bts", "blackpink", "twice", "red velvet", "aespa", "itzy", "ive", "newjeans", "stray kids", "seventeen", "nct", "exo", "bigbang", "2ne1", "girls generation", "snsd", "더 쇼", "the show", "music bank", "inkigayo", "m countdown", "show champion", "뮤직뱅크", "인기가요", "엠카운트다운", "쇼챔피언", "음악중심", "music core", "comeback", "컴백", "debut", "데뷔", "mv", "뮤직비디오", "music video", "live stage", "라이브", "performance", "퍼포먼스", "dance practice", "안무", "idol", "아이돌", "kpop", "k-pop", "케이팝", "한류", "hallyu", "가사", "lyrics", "노래", "song", "음악", "music", "앨범", "album", "미발매", "unreleased", "콘서트", "concert", "페스티벌", "festival", "칸타빌레", "cantabile", "더 시즌즈", "the seasons", "박보검", "샘 킴", "sam kim", "오현우", "ohHyunwoo", "일식", "eclipse", "[가사]", "[lyrics]", "kbs", "방송"};

    @Value("${google.api.key:}")
    private String googleApiKey;
    
    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Seoul")
    @Transactional
    public void createDailyCinemaPost() {
        createDailyCinemaPostWithRetry();
    }
    
    // 5분 후 재시도
    @Scheduled(cron = "0 5 20 * * *", zone = "Asia/Seoul")
    @Transactional
    public void retryDailyCinemaPost() {
        if (shouldRetryToday()) {
            log.info("별빛 시네마 재시도 시작");
            createDailyCinemaPostWithRetry();
        }
    }
    
    // 10분 후 마지막 재시도
    @Scheduled(cron = "0 10 20 * * *", zone = "Asia/Seoul")
    @Transactional
    public void finalRetryDailyCinemaPost() {
        if (shouldRetryToday()) {
            log.info("별빛 시네마 마지막 재시도 시작");
            createDailyCinemaPostWithRetry();
        }
    }
    
    private void createDailyCinemaPostWithRetry() {
        try {
            log.info("별빛 시네마 자동 포스팅 시작");
            collectAndSaveSpaceVideo(getSystemUser());
        } catch (Exception e) {
            log.error("별빛 시네마 자동 포스팅 실패", e);
        }
    }
    
    private boolean shouldRetryToday() {
        // 오늘 이미 성공한 게시글이 있는지 확인
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        // 오늘 작성된 STARLIGHT_CINEMA 게시글 개수 조회
        long todayPosts = cinemaRepository.countByCreatedAtAfter(todayStart);
            
        boolean shouldRetry = todayPosts == 0;
        log.info("오늘 별빛시네마 게시글 수: {}, 재시도 필요: {}", todayPosts, shouldRetry);
        return shouldRetry;
    }

    public void createCinemaPostManually(User admin) {
        try {
            log.info("수동 별빛 시네마 포스팅 시작 - 관리자: {}", admin.getNickname());
            collectAndSaveSpaceVideo(admin);
            log.info("수동 별빛 시네마 포스팅 성공");
        } catch (Exception e) {
            log.error("수동 별빛 시네마 포스팅 실패", e);
            throw new RuntimeException("별빛 시네마 포스팅에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void collectAndSaveSpaceVideo(User user) {
        log.info("우주 영상 수집 시작");
        
        Map<String, Object> videoData = fetchSpaceVideo();
        if (videoData == null) {
            log.warn("영상 데이터를 가져올 수 없습니다");
            return;
        }
        
        String videoId = (String) videoData.get("videoId");
        String title = (String) videoData.get("title");
        
        if (isDuplicateVideo(videoId, title)) {
            log.info("중복 영상으로 스킵: {}", title);
            return;
        }
        
        Cinema cinema = convertToCinema(videoData);
        cinemaRepository.save(cinema);
        
        Post post = convertToPost(videoData, user);
        Post savedPost = postRepository.save(post);
        
        log.info("새 별빛시네마 게시글 저장: {}", savedPost.getTitle());
    }
    
    private Map<String, Object> fetchSpaceVideo() {
        if (googleApiKey == null || googleApiKey.isEmpty()) {
            return createMockVideoData();
        }

        // 한국어 → 영어 순서로 시도
        String[][] keywordSets = {KOREAN_KEYWORDS, ENGLISH_KEYWORDS};
        
        for (String[] keywords : keywordSets) {
            for (int attempt = 0; attempt < cinemaConfig.getCollection().getRetryCount(); attempt++) {
                try {
                    Map<String, Object> video = searchYouTube(keywords);
                    if (video != null && !isSimilarToExistingVideos(video)) {
                        return video;
                    }
                    Thread.sleep(1000 * (attempt + 1));
                } catch (Exception e) {
                    log.warn("YouTube 검색 시도 {}/{} 실패: {}", attempt + 1, cinemaConfig.getCollection().getRetryCount(), e.getMessage());
                }
            }
        }
        
        return createMockVideoData();
    }
    
    private Map<String, Object> searchYouTube(String[] keywords) {
        String query = getRandomKeywords(keywords, cinemaConfig.getCollection().getKeywordCount());
        
        // 1차: 조회수 순으로 검색
        String url = String.format(
            "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s&type=video&maxResults=%d&order=viewCount&publishedAfter=%s&videoDuration=%s&videoDefinition=%s&key=%s",
            query, cinemaConfig.getQuality().getMaxResults(), getPublishedAfterDate(), 
            cinemaConfig.getQuality().getVideoDuration(), cinemaConfig.getQuality().getVideoDefinition(), googleApiKey
        );
        
        log.info("YouTube API 호출 (조회수 순): {}", query);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        
        if (response == null) return null;
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
        
        if (items == null || items.isEmpty()) return null;
        
        // 품질 필터링 및 통계 검증
        List<Map<String, Object>> qualityVideos = items.stream()
            .filter(this::isQualityVideo)
            .map(this::enrichWithVideoStats)
            .filter(Objects::nonNull)
            .filter(this::hasMinimumEngagement)
            .sorted(this::compareVideoQuality)
            .collect(java.util.stream.Collectors.toList());
            
        if (qualityVideos.isEmpty()) {
            log.warn("고품질 영상을 찾지 못함, 관련도 순으로 재검색");
            return searchYouTubeByRelevance(query);
        }
        
        // 상위 30% 중에서 랜덤 선택 (품질과 다양성 균형)
        int topCount = Math.max(1, qualityVideos.size() / 3);
        List<Map<String, Object>> topVideos = qualityVideos.subList(0, topCount);
        Map<String, Object> selectedVideo = topVideos.get(new Random().nextInt(topVideos.size()));
        
        log.info("선택된 영상: {} (조회수: {}, 좋아요: {})", 
                getVideoTitle(selectedVideo), 
                getVideoViewCount(selectedVideo),
                getVideoLikeCount(selectedVideo));
                
        return parseVideoData(selectedVideo);
    }
    
    private Map<String, Object> searchYouTubeByRelevance(String query) {
        String url = String.format(
            "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s&type=video&maxResults=%d&order=relevance&publishedAfter=%s&videoDuration=%s&key=%s",
            query, cinemaConfig.getQuality().getMaxResults(), getPublishedAfterDate(), 
            cinemaConfig.getQuality().getVideoDuration(), googleApiKey
        );
        
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        
        if (response == null) return null;
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
        
        if (items == null || items.isEmpty()) return null;
        
        List<Map<String, Object>> qualityVideos = items.stream()
            .filter(this::isQualityVideo)
            .collect(java.util.stream.Collectors.toList());
            
        if (qualityVideos.isEmpty()) return null;
        
        Map<String, Object> selectedVideo = qualityVideos.get(new Random().nextInt(qualityVideos.size()));
        return parseVideoData(selectedVideo);
    }
    
    private Map<String, Object> parseVideoData(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> snippet = (Map<String, Object>) video.get("snippet");
        @SuppressWarnings("unchecked")
        Map<String, Object> videoId = (Map<String, Object>) video.get("id");
        
        return formatVideoData(
            (String) snippet.get("title"),
            (String) snippet.get("description"),
            (String) videoId.get("videoId"),
            (String) snippet.get("channelTitle"),
            parsePublishedDateTime((String) snippet.get("publishedAt"))
        );
    }
    
    private boolean isQualityVideo(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> snippet = (Map<String, Object>) video.get("snippet");
        String title = (String) snippet.get("title");
        String description = (String) snippet.get("description");
        String channelTitle = (String) snippet.get("channelTitle");
        
        if (title == null || description == null) return false;
        
        String titleLower = title.toLowerCase();
        String descLower = description.toLowerCase();
        
        // 기본 품질 체크
        if (titleLower.contains("shorts") || titleLower.contains("#shorts") ||
            title.length() < cinemaConfig.getQuality().getMinTitleLength() ||
            description.length() < cinemaConfig.getQuality().getMinDescriptionLength()) {
            return false;
        }
        
        // K-POP 및 음악 관련 키워드 필터링
        if (isKPopOrMusicContent(titleLower, descLower)) {
            return false;
        }
        
        // 고품질 채널 우선순위 체크
        if (channelTitle != null && isProfessionalChannel(channelTitle)) {
            log.info("고품질 채널 발견: {}", channelTitle);
            return true; // 전문 채널은 우주 키워드 체크 완화
        }
        
        // 우주 키워드 체크 (최소 2개 이상 필요)
        int spaceKeywordCount = 0;
        for (String keyword : ALL_KEYWORDS) {
            if (titleLower.contains(keyword) || descLower.contains(keyword)) {
                spaceKeywordCount++;
            }
        }
        
        // 우주 키워드가 2개 이상 있어야 함
        return spaceKeywordCount >= 2;
    }
    
    private boolean isProfessionalChannel(String channelTitle) {
        String channelLower = channelTitle.toLowerCase();
        String[] qualityChannels = cinemaConfig.getYoutube().getQualityChannels();
        
        for (String qualityChannel : qualityChannels) {
            if (channelLower.contains(qualityChannel.toLowerCase())) {
                return true;
            }
        }
        
        // 추가 전문 채널 패턴
        return channelLower.contains("science") || 
               channelLower.contains("space") || 
               channelLower.contains("astronomy") ||
               channelLower.contains("documentary") ||
               channelLower.contains("education");
    }
    
    private boolean isKPopOrMusicContent(String titleLower, String descLower) {
        return java.util.Arrays.stream(MUSIC_KEYWORDS)
                .anyMatch(keyword -> titleLower.contains(keyword) || descLower.contains(keyword));
    }
    
    private Map<String, Object> enrichWithVideoStats(Map<String, Object> video) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> videoId = (Map<String, Object>) video.get("id");
            String id = (String) videoId.get("videoId");
            
            if (id == null) return null;
            
            // YouTube Data API v3로 영상 통계 조회
            String statsUrl = String.format(
                "https://www.googleapis.com/youtube/v3/videos?part=statistics,contentDetails&id=%s&key=%s",
                id, googleApiKey
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> statsResponse = restTemplate.getForObject(statsUrl, Map.class);
            
            if (statsResponse != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) statsResponse.get("items");
                
                if (items != null && !items.isEmpty()) {
                    Map<String, Object> videoStats = items.get(0);
                    video.put("statistics", videoStats.get("statistics"));
                    video.put("contentDetails", videoStats.get("contentDetails"));
                }
            }
            
            return video;
        } catch (Exception e) {
            log.warn("영상 통계 조회 실패: {}", e.getMessage());
            return video; // 통계 조회 실패해도 영상은 유지
        }
    }
    
    private boolean hasMinimumEngagement(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) video.get("statistics");
        
        if (statistics == null) return true; // 통계가 없으면 통과
        
        try {
            String viewCountStr = (String) statistics.get("viewCount");
            String likeCountStr = (String) statistics.get("likeCount");
            
            if (viewCountStr != null) {
                long viewCount = Long.parseLong(viewCountStr);
                // 최소 1만 조회수 이상
                if (viewCount < 10000) {
                    log.debug("조회수 부족으로 제외: {} ({}회)", getVideoTitle(video), viewCount);
                    return false;
                }
            }
            
            if (likeCountStr != null) {
                long likeCount = Long.parseLong(likeCountStr);
                // 최소 100 좋아요 이상
                if (likeCount < 100) {
                    log.debug("좋아요 부족으로 제외: {} ({}개)", getVideoTitle(video), likeCount);
                    return false;
                }
            }
            
            return true;
        } catch (NumberFormatException e) {
            return true; // 파싱 실패시 통과
        }
    }
    
    private int compareVideoQuality(Map<String, Object> v1, Map<String, Object> v2) {
        // 1. 전문 채널 우선순위
        boolean v1Professional = isProfessionalChannel(getChannelTitle(v1));
        boolean v2Professional = isProfessionalChannel(getChannelTitle(v2));
        
        if (v1Professional != v2Professional) {
            return v1Professional ? -1 : 1;
        }
        
        // 2. 조회수 비교
        long v1Views = getVideoViewCount(v1);
        long v2Views = getVideoViewCount(v2);
        
        if (v1Views != v2Views) {
            return Long.compare(v2Views, v1Views); // 높은 조회수 우선
        }
        
        // 3. 좋아요 비교
        long v1Likes = getVideoLikeCount(v1);
        long v2Likes = getVideoLikeCount(v2);
        
        return Long.compare(v2Likes, v1Likes);
    }
    
    private String getVideoTitle(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> snippet = (Map<String, Object>) video.get("snippet");
        return snippet != null ? (String) snippet.get("title") : "";
    }
    
    private String getChannelTitle(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> snippet = (Map<String, Object>) video.get("snippet");
        return snippet != null ? (String) snippet.get("channelTitle") : "";
    }
    
    private long getVideoViewCount(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) video.get("statistics");
        if (statistics != null) {
            String viewCountStr = (String) statistics.get("viewCount");
            if (viewCountStr != null) {
                try {
                    return Long.parseLong(viewCountStr);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
    
    private long getVideoLikeCount(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) video.get("statistics");
        if (statistics != null) {
            String likeCountStr = (String) statistics.get("likeCount");
            if (likeCountStr != null) {
                try {
                    return Long.parseLong(likeCountStr);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
    
    private String getRandomKeywords(String[] keywords, int count) {
        Random random = new Random();
        Set<String> selected = new HashSet<>();
        
        while (selected.size() < count && selected.size() < keywords.length) {
            selected.add(keywords[random.nextInt(keywords.length)]);
        }
        
        return String.join(" OR ", selected);
    }
    
    private String getPublishedAfterDate() {
        return LocalDateTime.now()
                .minusYears(cinemaConfig.getYoutube().getPublishedAfterYears())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }
    
    private boolean isDuplicateVideo(String videoId, String title) {
        return cinemaRepository.existsByVideoId(videoId) || cinemaRepository.existsByTitle(title);
    }
    
    private boolean isSimilarToExistingVideos(Map<String, Object> videoData) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cinemaConfig.getCollection().getSimilarityCheckDays());
        List<Cinema> recentVideos = cinemaRepository.findByCreatedAtAfter(cutoffDate);
        
        String newTitle = (String) videoData.get("title");
        
        for (Cinema cinema : recentVideos) {
            double similarity = calculateTitleSimilarity(newTitle, cinema.getTitle());
            if (similarity > cinemaConfig.getCollection().getSimilarityThreshold()) {
                log.info("유사 영상 발견 (유사도: {:.1f}%): {} vs {}", 
                        similarity * 100, newTitle, cinema.getTitle());
                return true;
            }
        }
        
        return false;
    }
    
    private double calculateTitleSimilarity(String title1, String title2) {
        String[] words1 = title1.toLowerCase().split("\\s+");
        String[] words2 = title2.toLowerCase().split("\\s+");
        
        int commonWords = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) && word1.length() > 2) {
                    commonWords++;
                    break;
                }
            }
        }
        
        return (double) commonWords / Math.max(words1.length, words2.length);
    }
    
    private Cinema convertToCinema(Map<String, Object> videoData) {
        return Cinema.builder()
                .title((String) videoData.get("title"))
                .description((String) videoData.get("description"))
                .videoId((String) videoData.get("videoId"))
                .videoUrl((String) videoData.get("videoUrl"))
                .channelTitle((String) videoData.get("channelTitle"))
                .publishedAt((LocalDateTime) videoData.get("publishedAt"))
                .summary((String) videoData.get("summary"))
                .hashtags((String) videoData.get("hashtags"))
                .build();
    }
    
    private Post convertToPost(Map<String, Object> videoData, User writer) {
        return Post.builder()
                .title((String) videoData.get("title"))
                .content((String) videoData.get("content"))
                .category(Post.Category.STARLIGHT_CINEMA)
                .writer(writer)
                .build();
    }
    
    private Map<String, Object> formatVideoData(String title, String description, String videoId, String channelTitle, LocalDateTime publishedAt) {
        Map<String, Object> data = new HashMap<>();
        
        String translatedTitle = translateIfNeeded(title);
        String translatedDescription = translateIfNeeded(description);
        
        data.put("title", translatedTitle);
        data.put("description", translatedDescription);
        data.put("videoId", videoId);
        data.put("videoUrl", "https://www.youtube.com/watch?v=" + videoId);
        data.put("channelTitle", channelTitle);
        data.put("publishedAt", publishedAt);
        data.put("summary", generateSummary(translatedTitle));
        data.put("hashtags", generateHashtags(translatedTitle));
        data.put("content", formatVideoContent(translatedTitle, translatedDescription, videoId, channelTitle, publishedAt));
        
        return data;
    }
    
    private String translateIfNeeded(String text) {
        if (text == null || text.trim().isEmpty()) return text;
        
        if (isEnglishText(text)) {
            String translated = translateWithOpenAI(text);
            return translated != null ? translated : "[해외영상] " + text;
        }
        return text;
    }
    
    private boolean isEnglishText(String text) {
        int englishCount = 0;
        int koreanCount = 0;
        
        for (char c : text.toCharArray()) {
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                englishCount++;
            } else if (c >= '가' && c <= '힣') {
                koreanCount++;
            }
        }
        
        return englishCount > koreanCount;
    }
    
    private String translateWithOpenAI(String englishText) {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            return null;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            String prompt = String.format("""
                다음 영어 텍스트를 자연스럽고 정확한 한국어로 번역해주세요:
                
                "%s"
                
                요구사항:
                - 우주/과학 전문 용어는 정확하게 번역
                - 자연스럽고 읽기 쉬운 한국어로 번역
                - 번역문만 반환 (설명 없이)
                """, englishText);
            
            Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 200,
                "temperature", 0.3
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                "https://api.openai.com/v1/chat/completions", entity, Map.class);
            
            if (response != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String translatedText = (String) message.get("content");
                    log.info("번역 성공: {} -> {}", englishText, translatedText);
                    return translatedText.trim();
                }
            }
        } catch (Exception e) {
            log.warn("번역 실패: {}", englishText, e);
        }
        
        return null;
    }
    
    private String generateSummary(String title) {
        return title.length() > 50 ? title.substring(0, 47) + "..." : title;
    }
    
    private String generateHashtags(String title) {
        List<String> tags = new ArrayList<>();
        String content = title.toLowerCase();
        
        if (content.contains("우주") || content.contains("space")) tags.add("#우주");
        if (content.contains("블랙홀") || content.contains("blackhole")) tags.add("#블랙홀");
        if (content.contains("화성") || content.contains("mars")) tags.add("#화성");
        if (content.contains("nasa")) tags.add("#NASA");
        if (content.contains("spacex")) tags.add("#SpaceX");
        
        return String.join(" ", tags);
    }
    
    private String formatVideoContent(String title, String description, String videoId, String channelTitle, LocalDateTime publishedAt) {
        StringBuilder content = new StringBuilder();
        
        content.append("🎬 **오늘의 우주 영상**: ").append(title).append("\n\n");
        
        content.append("▶️ **영상 보기**\n\n");
        content.append(String.format("""
            <div class="video-container" style="position: relative; padding-bottom: 56.25%%; height: 0; overflow: hidden; max-width: 100%%; background: #000; margin: 20px 0;">
                <iframe src="https://www.youtube.com/embed/%s" 
                        frameborder="0" 
                        allowfullscreen 
                        style="position: absolute; top: 0; left: 0; width: 100%%; height: 100%%;">
                </iframe>
            </div>
            
            """, videoId));
        
        content.append("⚠️ **영상이 보이지 않나요?** [YouTube에서 보기](https://www.youtube.com/watch?v=").append(videoId).append(")\n\n");
        
        content.append("📺 **채널명**: ").append(channelTitle);
        if (publishedAt != null) {
            content.append(" 📅 **발행일**: ").append(publishedAt.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
        }
        content.append("\n\n");
        
        if (description != null && !description.trim().isEmpty()) {
            content.append("📝 **설명** ").append(description).append("\n\n");
        }

        content.append("💬 **자유롭게 의견을 나눠주세요!**\n\n");
        content.append("---\n\n");
        
        return content.toString();
    }
    
    private Map<String, Object> createMockVideoData() {
        String[] mockTitles = {
            "우주의 신비: 블랙홀의 비밀",
            "은하수 너머의 세계",
            "화성 탐사의 최신 소식"
        };
        
        String[] mockDescriptions = {
            "우주의 가장 신비로운 천체인 블랙홀에 대해 알아봅시다.",
            "우리 은하 너머에 존재하는 놀라운 우주의 모습을 탐험해보세요.",
            "화성 탐사 로버가 전해주는 최신 발견들을 소개합니다."
        };

        Random random = new Random();
        int index = random.nextInt(mockTitles.length);
        
        return formatVideoData(
            mockTitles[index],
            mockDescriptions[index],
            "dQw4w9WgXcQ",
            "우주 채널",
            LocalDateTime.now()
        );
    }
    
    private User getSystemUser() {
        return userRepository.findByEmail("newsbot@byeolnight.com")
                .orElseThrow(() -> new RuntimeException("시스템 사용자를 찾을 수 없습니다"));
    }
    
    private LocalDateTime parsePublishedDateTime(String publishedAt) {
        try {
            return LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    // YouTubeService에서 이동된 공개 API 메서드들
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchSpaceVideos() {
        try {
            String url = String.format(
                "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s&type=video&maxResults=%d&order=relevance&regionCode=KR&relevanceLanguage=ko&key=%s",
                getRandomSpaceQuery(), 12, googleApiKey
            );
            
            log.info("YouTube API 호출: 우주 관련 영상 검색");
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                var items = (List<Map<String, Object>>) response.get("items");
                log.info("YouTube 영상 검색 성공: {}개", items != null ? items.size() : 0);
                return items != null ? items : List.of();
            }
            
            log.warn("YouTube API 호출 실패");
            return List.of();
            
        } catch (Exception e) {
            log.error("YouTube 영상 검색 중 오류 발생", e);
            return List.of();
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchVideosByKeyword(String keyword) {
        try {
            String url = String.format(
                "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s 우주&type=video&maxResults=%d&order=relevance&regionCode=KR&relevanceLanguage=ko&key=%s",
                keyword, 6, googleApiKey
            );
            
            log.info("YouTube API 호출: {} 관련 영상 검색", keyword);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                var items = (List<Map<String, Object>>) response.get("items");
                return items != null ? items : List.of();
            }
            
            return List.of();
            
        } catch (Exception e) {
            log.error("YouTube 키워드 검색 중 오류 발생: {}", keyword, e);
            return List.of();
        }
    }
    
    public List<Map<String, Object>> getUniqueSpaceVideos() {
        List<Map<String, Object>> allVideos = new java.util.ArrayList<>();
        java.util.Set<String> videoIds = new java.util.HashSet<>();
        
        for (int i = 0; i < 3; i++) {
            List<Map<String, Object>> videos = searchSpaceVideos();
            for (Map<String, Object> video : videos) {
                @SuppressWarnings("unchecked")
                Map<String, Object> id = (Map<String, Object>) video.get("id");
                if (id != null) {
                    String videoId = (String) id.get("videoId");
                    if (videoId != null && !videoIds.contains(videoId)) {
                        videoIds.add(videoId);
                        allVideos.add(video);
                    }
                }
            }
        }
        
        log.info("중복 제거 후 YouTube 영상: {}개", allVideos.size());
        return allVideos;
    }
    
    private String getRandomSpaceQuery() {
        java.util.Random random = new java.util.Random();
        java.util.Set<String> selectedKeywords = new java.util.HashSet<>();
        
        while (selectedKeywords.size() < 3 && selectedKeywords.size() < KOREAN_KEYWORDS.length) {
            int randomIndex = random.nextInt(KOREAN_KEYWORDS.length);
            selectedKeywords.add(KOREAN_KEYWORDS[randomIndex]);
        }
        
        String query = String.join(" ", selectedKeywords);
        log.info("YouTube 검색 키워드: {}", query);
        return query;
    }
    
    public Map<String, Object> getCinemaStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalVideos", cinemaRepository.count());
        status.put("recentVideos", cinemaRepository.findTop10ByOrderByCreatedAtDesc().size());
        status.put("lastCollectionTime", LocalDateTime.now());
        status.put("configuration", cinemaConfig);
        return status;
    }
}