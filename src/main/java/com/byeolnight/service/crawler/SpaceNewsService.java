package com.byeolnight.service.crawler;

import com.byeolnight.entity.News;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.post.Post.Category;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.NewsRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.dto.ai.NewsApiResponseDto;
import com.byeolnight.infrastructure.config.NewsCollectionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpaceNewsService {
    
    private final NewsRepository newsRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final NewsCollectionProperties newsConfig;
    
    @Value("${app.security.external-api.ai.newsdata-api-key}")
    private String primaryApiKey;
    
    @Value("${app.newsdata.api-key-backup:}")
    private String backupApiKey;
    
    @Value("${app.security.external-api.ai.openai-api-key}")
    private String openaiApiKey;
    
    private boolean usingBackupKey = false;
    
    private static final String NEWS_API_URL = "https://newsdata.io/api/1/news";
    
    // 키워드 상수
    private static final String[] KOREAN_KEYWORDS = {"우주", "로켓", "위성", "화성", "달", "태양", "지구", "목성", "토성", "천왕성", "해왕성", "수성", "금성", "명왕성", "블랙홀", "은하", "별", "항성", "혜성", "소행성", "망원경", "천문", "항공우주", "우주선", "우주정거장", "우주비행사", "우주발사", "우주탐사", "성운", "퀘이사", "중성자별", "백색왜성", "적색거성", "초신성", "성단", "성간물질", "암흑물질", "암흑에너지", "빅뱅", "우주론", "외계행성", "외계생명", "SETI", "우주망원경", "허블", "제임스웹", "케플러", "스피처", "찬드라", "컴프턴", "국제우주정거장", "ISS", "아르테미스", "아폴로", "보이저", "카시니", "갈릴레오", "뉴호라이즌스", "파커", "주노", "화성탐사", "달탐사", "목성탐사", "토성탐사", "태양탐사", "소행성탐사", "혜성탐사", "우주쓰레기", "우주날씨", "태양풍", "자기권", "오로라", "일식", "월식", "유성우", "운석", "크레이터", "화산", "대기", "중력", "궤도", "공전", "자전", "조석", "라그랑주점", "중력파", "상대성이론", "양자역학", "끈이론", "다중우주", "우주배경복사", "적색편이", "도플러효과", "허블상수", "우주나이", "우주크기", "관측가능우주", "사건지평선", "특이점", "웜홀"};
    private static final String[] ENGLISH_KEYWORDS = {"space", "rocket", "satellite", "Mars", "Moon", "Sun", "Earth", "Jupiter", "Saturn", "Uranus", "Neptune", "Mercury", "Venus", "Pluto", "blackhole", "galaxy", "star", "stellar", "comet", "asteroid", "telescope", "astronomy", "aerospace", "spacecraft", "space station", "astronaut", "space launch", "space exploration", "nebula", "quasar", "neutron star", "white dwarf", "red giant", "supernova", "cluster", "interstellar", "dark matter", "dark energy", "big bang", "cosmology", "exoplanet", "extraterrestrial", "SETI", "space telescope", "Hubble", "James Webb", "Kepler", "Spitzer", "Chandra", "Compton", "ISS", "International Space Station", "Artemis", "Apollo", "Voyager", "Cassini", "Galileo", "New Horizons", "Parker", "Juno", "Mars exploration", "lunar exploration", "Jupiter mission", "Saturn mission", "solar mission", "asteroid mission", "comet mission", "space debris", "space weather", "solar wind", "magnetosphere", "aurora", "eclipse", "lunar eclipse", "meteor shower", "meteorite", "crater", "volcano", "atmosphere", "gravity", "orbit", "revolution", "rotation", "tidal", "Lagrange point", "gravitational wave", "relativity", "quantum mechanics", "string theory", "multiverse", "cosmic background", "redshift", "Doppler effect", "Hubble constant", "universe age", "universe size", "observable universe", "event horizon", "singularity", "wormhole"};
    private static final String[] ALL_KEYWORDS = java.util.stream.Stream.concat(java.util.Arrays.stream(KOREAN_KEYWORDS), java.util.Arrays.stream(ENGLISH_KEYWORDS)).map(String::toLowerCase).toArray(String[]::new);
    private static final String[] EXCLUDE_KEYWORDS = {"trump", "obama", "democrat", "republican", "politics", "election", "트럼프", "오바마", "정치", "선거", "경제", "주식", "코인", "기상", "weather", "날씨", "예보", "forecast", "예측", "prediction", "시장", "market", "비즈니스", "business", "산업", "industry", "팬데믹", "pandemic", "전염병", "바이러스", "virus", "질병", "disease", "농업", "agriculture", "물류", "logistics", "에너지", "energy", "지속가능", "sustainable", "솔루션", "solution", "성장", "growth"};
    
    @Transactional
    public void collectAndSaveSpaceNews() {
        log.info("한국어 우주 뉴스 수집 시작");
        
        NewsApiResponseDto response = fetchKoreanSpaceNews();
        if (response == null || response.getResults() == null) {
            log.warn("뉴스 데이터를 가져올 수 없습니다");
            return;
        }
        
        // 뉴스봇 사용자 가져오기
        User newsBot = userRepository.findByEmail("newsbot@byeolnight.com")
                .orElseThrow(() -> new RuntimeException("뉴스봇 사용자를 찾을 수 없습니다"));
        
        List<Post> savedPosts = new ArrayList<>();
        int actualDuplicateCount = 0;
        int filteredCount = 0;
        
        for (NewsApiResponseDto.Result result : response.getResults()) {
            log.info("\n========== 뉴스 처리 시작 ==========\n제목: {}\nURL: {}", result.getTitle(), result.getLink());
            
            if (isDuplicateNews(result)) {
                actualDuplicateCount++;
                log.info("중복으로 스킵됨");
                continue;
            }
            
            if (!isHighQualitySpaceNews(result)) {
                filteredCount++;
                log.info("품질 기준 미달로 스킵됨");
                continue;
            }
            
            // 설정된 최대 개수만 저장 (하루에 1개만 저장)
            if (savedPosts.size() >= newsConfig.getCollection().getMaxPosts()) {
                log.info("이미 {}개 뉴스를 저장했으므로 종료 (하루 1개 제한)", newsConfig.getCollection().getMaxPosts());
                break;
            }
            
            log.info("저장 진행 중... ({}/1)", savedPosts.size() + 1);
            
            // News 엔티티에 저장
            News news = convertToNews(result);
            newsRepository.save(news);
            
            // Post 엔티티로 변환하여 게시판에 표시
            Post post = convertToPost(result, newsBot);
            Post savedPost = postRepository.save(post);
            savedPosts.add(savedPost);
            
            log.info("새 뉴스 게시글 저장: {}", savedPost.getTitle());
        }
        
        log.info("우주 뉴스 수집 완료 - 수집: {}개, 저장: {}건 (하루 최대 {}), 실제 중복: {}건, 필터링: {}건", 
                response.getResults().size(), savedPosts.size(), newsConfig.getCollection().getMaxPosts(), actualDuplicateCount, filteredCount);
        
        // 뉴스 수집과 토론 주제 생성을 분리
        // 토론 주제는 별도 스케줄러에서 매일 오전 8시에 생성
        log.info("뉴스 수집 완료 - 토론 주제는 스케줄러에서 별도 처리");
    }
    
    private boolean isDuplicateNews(NewsApiResponseDto.Result result) {
        // URL 기준으로만 중복 체크 (제목은 번역되거나 수정될 수 있음)
        boolean isDuplicate = newsRepository.existsByUrl(result.getLink());
        
        log.info("=== 중복 체크 ===\n제목: {}\nURL: {}\n중복 여부: {}", 
                result.getTitle(), result.getLink(), isDuplicate);
        
        return isDuplicate;
    }
    
    /**
     * 최고 품질 우주 뉴스 필터링
     * 1. 충분한 글 길이
     * 2. 우주 관련성 체크
     * 3. 제목 유사도 체크
     */
    private boolean isHighQualitySpaceNews(NewsApiResponseDto.Result result) {
        String title = result.getTitle() != null ? result.getTitle() : "";
        String description = result.getDescription() != null ? result.getDescription() : "";
        
        log.info("=== 고품질 뉴스 필터링 ===\n제목: {}\n설명 길이: {}글자", title, description.length());
        
        // 1. 글 길이 체크 (너무 짧으면 제외)
        if (!hasMinimumLength(title, description)) {
            log.info("글 길이 부족으로 제외");
            return false;
        }
        
        // 2. 우주 관련성 체크
        if (!isSpaceRelated(title, description)) {
            log.info("우주 관련성 부족으로 제외");
            return false;
        }
        
        // 3. 전체 DB 뉴스와 유사도 체크
        if (isSimilarToExistingNews(result)) {
            log.info("기존 뉴스와 유사하여 제외");
            return false;
        }
        
        log.info("고품질 뉴스 기준 통과!");
        return true;
    }
    
    /**
     * 최소 글 길이 체크 (영어는 번역 후 체크)
     */
    private boolean hasMinimumLength(String title, String description) {
        String checkTitle = title;
        String checkDescription = description;
        
        // 영어 기사인 경우 번역 후 길이 체크
        if (isEnglishTitle(title)) {
            log.info("영어 기사 길이 체크를 위한 번역: {}", title);
            String translated = translateWithOpenAI(title + " " + description);
            if (translated != null) {
                String[] parts = translated.split("\\n", 2);
                checkTitle = parts[0];
                checkDescription = parts.length > 1 ? parts[1] : description;
            }
        }
        
        // 설정된 최소 길이 체크 (더 엄격하게)
        int minTitleLength = Math.max(newsConfig.getQuality().getMinTitleLength(), 20);
        int minDescLength = Math.max(newsConfig.getQuality().getMinDescriptionLength(), 50);
        
        if (checkTitle.length() < minTitleLength) {
            log.debug("제목이 너무 짧음: {}글자 ({})", checkTitle.length(), checkTitle);
            return false;
        }
        
        if (checkDescription.length() < minDescLength) {
            log.debug("설명이 너무 짧음: {}글자", checkDescription.length());
            return false;
        }
        
        // 추가 품질 체크: 제목에 숫자만 너무 많으면 제외
        long digitCount = checkTitle.chars().filter(Character::isDigit).count();
        if (digitCount > checkTitle.length() * 0.3) {
            log.debug("제목에 숫자가 너무 많음: {}", checkTitle);
            return false;
        }
        
        return true;
    }
    
    /**
     * 우주 관련성 체크 (캐싱된 200개 키워드 사용)
     */
    private boolean isSpaceRelated(String title, String description) {
        String content = (title + " " + description).toLowerCase();
        
        // 비우주 키워드 체크
        for (String exclude : EXCLUDE_KEYWORDS) {
            if (content.contains(exclude)) {
                log.info("비우주 키워드 '{}' 발견으로 제외", exclude);
                return false;
            }
        }
        
        // 캐싱된 키워드 배열 사용
        String[] allKeywords = ALL_KEYWORDS;
        
        int keywordCount = 0;
        List<String> foundKeywords = new ArrayList<>();
        for (String keyword : allKeywords) {
            if (content.contains(keyword)) {
                keywordCount++;
                foundKeywords.add(keyword);
            }
        }
        
        log.info("우주 관련 키워드 {}개 발견: {}", keywordCount, foundKeywords.stream().limit(5).toList());
        
        // 최소 3개 이상의 우주 키워드 필요 (기본 설정보다 엄격)
        int minKeywords = Math.max(newsConfig.getQuality().getMinSpaceKeywords(), 3);
        return keywordCount >= minKeywords;
    }
    
    /**
     * 최근 뉴스와 유사도 체크 (성능 최적화)
     */
    private boolean isSimilarToExistingNews(NewsApiResponseDto.Result result) {
        // 최근 N일 뉴스만 비교 (성능 최적화)
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(newsConfig.getCollection().getSimilarityCheckDays());
        List<News> recentNews = newsRepository.findByPublishedAtAfter(cutoffDate);
        
        // 영어 기사인 경우 번역 후 비교
        String translatedTitle = result.getTitle();
        String translatedDescription = result.getDescription() != null ? result.getDescription() : "";
        
        if (isEnglishTitle(result.getTitle())) {
            log.info("영어 기사 번역 중: {}", result.getTitle());
            String translated = translateWithOpenAI(result.getTitle() + " " + translatedDescription);
            if (translated != null) {
                String[] parts = translated.split("\\n", 2);
                translatedTitle = parts[0];
                translatedDescription = parts.length > 1 ? parts[1] : translatedDescription;
            }
        }
        
        String normalizedNewTitle = normalizeTitle(translatedTitle);
        String normalizedNewDesc = normalizeTitle(translatedDescription);
        
        for (News news : recentNews) {
            String normalizedExistingTitle = normalizeTitle(news.getTitle());
            String normalizedExistingDesc = normalizeTitle(news.getDescription() != null ? news.getDescription() : "");
            
            // 제목 + 내용 유사도 체크 (설정된 임계값 사용)
            double titleSimilarity = calculateTitleSimilarity(normalizedNewTitle, normalizedExistingTitle);
            double descSimilarity = calculateTitleSimilarity(normalizedNewDesc, normalizedExistingDesc);
            double overallSimilarity = (titleSimilarity + descSimilarity) / 2;
            
            if (overallSimilarity > newsConfig.getCollection().getSimilarityThreshold()) {
                log.info("유사 기사 발견 (유사도: {:.1f}%): {} vs {}", 
                        overallSimilarity * 100, translatedTitle, news.getTitle());
                return true;
            }
        }
        
        return false;
    }
    
    private String normalizeTitle(String title) {
        return title.toLowerCase()
                .replaceAll("[^\\w\\s가-힣]", "") // 특수문자 제거
                .replaceAll("\\s+", " ") // 여러 공백을 하나로
                .trim();
    }
    
    private double calculateTitleSimilarity(String title1, String title2) {
        String[] words1 = title1.split("\\s+");
        String[] words2 = title2.split("\\s+");
        
        int commonWords = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) && word1.length() > 2) { // 2글자 이상의 단어만
                    commonWords++;
                    break;
                }
            }
        }
        
        return (double) commonWords / Math.max(words1.length, words2.length);
    }
    
    private Post convertToPost(NewsApiResponseDto.Result result, User writer) {
        String content = formatNewsContent(result);
        String title = translateTitleIfNeeded(result.getTitle());
        
        // 제목 길이 제한 (100자)
        if (title.length() > 100) {
            title = title.substring(0, 97) + "...";
        }
        
        return Post.builder()
                .title(title)
                .content(content)
                .category(Category.NEWS)
                .writer(writer)
                .build();
    }
    
    private String translateTitleIfNeeded(String title) {
        if (isEnglishTitle(title)) {
            String translatedTitle = translateWithOpenAI(title);
            return translatedTitle != null ? translatedTitle : "[해외뉴스] " + title;
        }
        return title;
    }
    
    private String translateWithOpenAI(String englishTitle) {
        // OpenAI API 키 체크
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            log.debug("OpenAI API 키가 설정되지 않아 번역 스킵");
            return null;
        }
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            String prompt = String.format("""
                다음 영어 뉴스 제목을 자연스럽고 정확한 한국어로 번역해주세요:
                
                "%s"
                
                요구사항:
                - 우주/과학 전문 용어는 정확하게 번역 (예: asteroid → 소행성, meteor shower → 유성우)
                - 자연스럽고 읽기 쉬운 한국어로 번역
                - 뉴스 제목답게 간결하고 임팩트 있게 번역
                - 번역문만 반환 (설명이나 따옴표 없이)
                """, englishTitle);
            
            Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 100,
                "temperature", 0.3
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions",
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String translatedTitle = (String) message.get("content");
                    log.info("번역 성공: {} -> {}", englishTitle, translatedTitle);
                    return translatedTitle.trim();
                }
            }
        } catch (Exception e) {
            log.warn("번역 실패: {} - 오류: {}", englishTitle, e.getMessage());
        }
        
        return null;
    }
    
    private boolean isEnglishTitle(String title) {
        // 영어 문자가 한국어 문자보다 많으면 영어 제목으로 판단
        int englishCount = 0;
        int koreanCount = 0;
        
        for (char c : title.toCharArray()) {
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                englishCount++;
            } else if (c >= '가' && c <= '힣') {
                koreanCount++;
            }
        }
        
        return englishCount > koreanCount;
    }
    
    private String formatNewsContent(NewsApiResponseDto.Result result) {
        String imageSection = result.getImageUrl() != null && !result.getImageUrl().trim().isEmpty() 
            ? "![뉴스 이미지](" + result.getImageUrl() + ")\n\n" : "";
        
        String description = result.getDescription();
        if (description != null && !description.trim().isEmpty() && isEnglishTitle(result.getTitle())) {
            String translated = translateWithOpenAI(description);
            description = translated != null ? translated : description;
        }
        
        String summarySection = description != null && !description.trim().isEmpty()
            ? "## 📰 뉴스 요약\n\n" + description + "\n\n"
            : "## 📰 뉴스 요약\n\n이 뉴스는 우주와 천문학 관련 최신 소식을 다룹니다. 자세한 내용은 원문 링크를 통해 확인하세요.\n\n";
        
        String sourceInfo = "";
        if (result.getSourceName() != null) sourceInfo += "**출처:** " + result.getSourceName() + "\n";
        if (result.getPubDate() != null) sourceInfo += "**발행일:** " + result.getPubDate() + "\n\n";
        
        String hashtags = generateHashtags(result.getTitle(), result.getDescription());
        
        return String.format("""
            %s%s## 🤖 AI 분석
            
            %s
            
            ## 📄 상세 내용
            
            ⚠️ **원문 크롤링 제한**: 저작권 및 기술적 제약으로 원문 내용을 직접 가져올 수 없습니다.
            
            💡 **대신 제공**: AI 기반 분석과 요약을 통해 핵심 내용을 파악하실 수 있습니다.
            
            ## 🔗 원문 보기
            
            [📰 원문 기사 보기](%s)
            
            ---
            
            %s%s
            """, 
            imageSection, summarySection, generateAIAnalysis(result), result.getLink(), sourceInfo, 
            hashtags.isEmpty() ? "" : hashtags
        );
    }
    
    private News convertToNews(NewsApiResponseDto.Result result) {
        String title = translateTitleIfNeeded(result.getTitle());
        
        return News.builder()
                .title(title)
                .description(result.getDescription())
                .imageUrl(result.getImageUrl() != null ? result.getImageUrl() : getDefaultSpaceImage())
                .url(result.getLink())
                .publishedAt(parsePublishedAt(result.getPubDate()))
                .hashtags(generateHashtags(result.getTitle(), result.getDescription()))
                .source(result.getSourceName() != null ? result.getSourceName() : "Unknown")
                .summary(generateSummary(result))
                .build();
    }
    
    private LocalDateTime parsePublishedAt(String publishedAt) {
        try {
            // NewsData.io 날짜 형식: "2024-01-15 12:30:45"
            return LocalDateTime.parse(publishedAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            try {
                // ISO 형식도 시도
                return LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception ex) {
                log.warn("발행일 파싱 실패: {}, 현재 시간으로 설정", publishedAt);
                return LocalDateTime.now();
            }
        }
    }
    
    private String generateHashtags(String title, String description) {
        List<String> tags = new ArrayList<>();
        String content = (title + " " + (description != null ? description : "")).toLowerCase();
        
        // 키워드 기반 해시태그 생성
        String[] koreanKeywords = KOREAN_KEYWORDS;
        String[] englishKeywords = ENGLISH_KEYWORDS;
        
        // 한국어 키워드 체크
        for (String keyword : koreanKeywords) {
            if (content.contains(keyword.toLowerCase()) && tags.size() < 10) {
                tags.add("#" + keyword);
            }
        }
        
        // 영어 키워드 체크 (한국어로 변환)
        if (content.contains("nasa")) tags.add("#NASA");
        if (content.contains("spacex")) tags.add("#SpaceX");
        if (content.contains("space") && !tags.contains("#우주")) tags.add("#우주");
        if (content.contains("mars") && !tags.contains("#화성")) tags.add("#화성");
        if (content.contains("moon") && !tags.contains("#달")) tags.add("#달");
        if (content.contains("blackhole") && !tags.contains("#블랙홀")) tags.add("#블랙홀");
        if (content.contains("galaxy") && !tags.contains("#은하")) tags.add("#은하");
        
        return String.join(" ", tags.stream().distinct().limit(newsConfig.getQuality().getMaxHashtags()).toList());
    }
    
    private String getDefaultSpaceImage() {
        return "https://images.unsplash.com/photo-1446776877081-d282a0f896e2?w=800&h=600&fit=crop";
    }
    
    /**
     * AI 기반 뉴스 상세 분석 생성
     */
    private String generateAIAnalysis(NewsApiResponseDto.Result result) {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            return "현재 이용 가능한 정보를 바탕으로 한 분석입니다. 더 자세한 내용은 원문 링크를 통해 확인하세요.";
        }
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            String content = result.getTitle() + "\n" + (result.getDescription() != null ? result.getDescription() : "");
            String prompt = String.format("""
                다음 우주 뉴스를 분석하여 주요 포인트를 정리해주세요:
                
                "%s"
                
                요구사항:
                - 핵심 내용을 3-4개 포인트로 정리
                - 과학적 의미와 중요성 설명
                - 일반인이 이해하기 쉬운 언어로 설명
                - 미래 영향이나 의미가 있다면 언급
                - 250자 내외로 작성
                - 이모지나 특수문자 사용 금지
                """, content);
            
            Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 300,
                "temperature", 0.4
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions", HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String analysis = ((String) message.get("content")).trim();
                    log.info("AI 분석 성공: {}", result.getTitle());
                    return analysis;
                }
            }
        } catch (Exception e) {
            log.warn("AI 분석 생성 실패: {} - 오류: {}", result.getTitle(), e.getMessage());
        }
        
        return "현재 이용 가능한 정보를 바탕으로 한 분석입니다. 더 자세한 내용은 원문 링크를 통해 확인하세요.";
    }
    
    /**
     * AI 기반 뉴스 요약 생성
     */
    private String generateSummary(NewsApiResponseDto.Result result) {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            return result.getDescription() != null ? 
                result.getDescription().substring(0, Math.min(100, result.getDescription().length())) + "..." : 
                "우주 관련 최신 뉴스";
        }
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            String content = result.getTitle() + " " + (result.getDescription() != null ? result.getDescription() : "");
            String prompt = String.format("""
                다음 우주 뉴스를 50자 이내로 요약해주세요:
                
                "%s"
                
                요구사항:
                - 50자 이내로 간결하게
                - 핵심 내용만 포함
                - 요약문만 반환
                """, content);
            
            Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 60,
                "temperature", 0.3
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions", HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String summary = ((String) message.get("content")).trim();
                    log.info("요약 생성 성공: {}", result.getTitle());
                    return summary;
                }
            }
        } catch (Exception e) {
            log.warn("요약 생성 실패: {} - 오류: {}", result.getTitle(), e.getMessage());
        }
        
        return result.getDescription() != null ? 
            result.getDescription().substring(0, Math.min(100, result.getDescription().length())) + "..." : 
            "우주 관련 최신 뉴스";
    }
    
    // NewsDataService에서 이동된 메서드들
    public NewsApiResponseDto fetchKoreanSpaceNews() {
        try {
            int callCount = 4;
            
            String koreanQuery = "NASA OR SpaceX OR 우주탐사 OR 화성탐사 OR 달탐사";
            log.info("한국어 뉴스 수집 키워드: {}", koreanQuery);
            NewsApiResponseDto koreanNews = fetchMultipleNewsByLanguage("ko", koreanQuery, callCount / 2);
            
            String englishQuery = "NASA OR SpaceX OR Mars OR Moon OR space exploration OR astronomy";
            log.info("영어 뉴스 수집 키워드: {}", englishQuery);
            NewsApiResponseDto englishNews = fetchMultipleNewsByLanguage("en", englishQuery, callCount / 2);
            
            NewsApiResponseDto combinedNews = new NewsApiResponseDto();
            combinedNews.setStatus("success");
            combinedNews.setResults(new java.util.ArrayList<>());
            
            if (koreanNews != null && koreanNews.getResults() != null) {
                combinedNews.getResults().addAll(koreanNews.getResults());
            }
            if (englishNews != null && englishNews.getResults() != null) {
                combinedNews.getResults().addAll(englishNews.getResults());
            }
            
            combinedNews.setTotalResults(combinedNews.getResults().size());
            log.info("총 {}+{} = {}개 뉴스 수집 완료", 
                    koreanNews != null ? koreanNews.getResults().size() : 0,
                    englishNews != null ? englishNews.getResults().size() : 0,
                    combinedNews.getResults().size());
            
            return combinedNews;
            
        } catch (Exception e) {
            log.error("NewsData.io API 호출 중 오류 발생", e);
            return null;
        }
    }
    
    private NewsApiResponseDto fetchNewsByLanguage(String language, String query, int size) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(NEWS_API_URL)
                    .queryParam("apikey", getCurrentApiKey())
                    .queryParam("language", language)
                    .queryParam("q", query)
                    .queryParam("category", "science")
                    .queryParam("size", String.valueOf(size))
                    .build()
                    .toUriString();
            
            log.info("NewsData.io API 호출 ({}): {}", language, url);
            
            NewsApiResponseDto response = restTemplate.getForObject(url, NewsApiResponseDto.class);
            
            if (response != null && "success".equals(response.getStatus())) {
                log.info("{} 뉴스 수집 성공: {}개", language, response.getResults().size());
                return response;
            } else {
                log.warn("{} 뉴스 수집 실패: {}", language, response != null ? response.getStatus() : "null response");
                return null;
            }
            
        } catch (Exception e) {
            log.error("{} NewsData.io API 호출 중 오류 발생: {}", language, e.getMessage());
            
            if (isQuotaExceededError(e) && !usingBackupKey && !backupApiKey.isEmpty()) {
                log.warn("기본 API 키 한도 초과, 백업 키로 재시도합니다.");
                usingBackupKey = true;
                return fetchNewsByLanguage(language, query, size);
            }
            
            return null;
        }
    }
    
    private NewsApiResponseDto fetchMultipleNewsByLanguage(String language, String query, int callCount) {
        NewsApiResponseDto combinedResponse = new NewsApiResponseDto();
        combinedResponse.setStatus("success");
        combinedResponse.setResults(new java.util.ArrayList<>());
        java.util.Set<String> seenUrls = new java.util.HashSet<>();
        
        for (int i = 0; i < callCount; i++) {
            try {
                String[] keywords = language.equals("ko") ? KOREAN_KEYWORDS : ENGLISH_KEYWORDS;
                String randomQuery = getRandomSpaceKeywords(keywords, 3);
                
                NewsApiResponseDto response = fetchNewsByLanguage(language, randomQuery, 10);
                if (response != null && response.getResults() != null) {
                    for (NewsApiResponseDto.Result result : response.getResults()) {
                        if (result.getLink() != null && !seenUrls.contains(result.getLink())) {
                            seenUrls.add(result.getLink());
                            combinedResponse.getResults().add(result);
                        }
                    }
                }
                
                if (i < callCount - 1) {
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                log.warn("{} 뉴스 {}번째 호출 실패", language, i + 1, e);
            }
        }
        
        combinedResponse.setTotalResults(combinedResponse.getResults().size());
        log.info("{} 뉴스 {}번 호출 완료: {}개 수집", language, callCount, combinedResponse.getResults().size());
        return combinedResponse;
    }
    
    private String getRandomSpaceKeywords(String[] keywords, int count) {
        java.util.Random random = new java.util.Random();
        java.util.Set<String> selectedKeywords = new java.util.HashSet<>();
        
        while (selectedKeywords.size() < count && selectedKeywords.size() < keywords.length) {
            int randomIndex = random.nextInt(keywords.length);
            selectedKeywords.add(keywords[randomIndex]);
        }
        
        return String.join(" OR ", selectedKeywords);
    }
    
    private String getCurrentApiKey() {
        String currentKey = usingBackupKey ? backupApiKey : primaryApiKey;
        log.debug("현재 사용 중인 API 키: {} (백업 키 사용: {})", 
                currentKey.substring(0, Math.min(10, currentKey.length())) + "...", usingBackupKey);
        return currentKey;
    }
    
    private boolean isQuotaExceededError(Exception e) {
        String errorMessage = e.getMessage().toLowerCase();
        return errorMessage.contains("quota") || 
               errorMessage.contains("limit") || 
               errorMessage.contains("exceeded") ||
               errorMessage.contains("429");
    }
    
    public long getTodayNewsCount() {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        // 오늘 작성된 NEWS 게시글 개수 조회
        return newsRepository.countByCreatedAtAfter(todayStart);
    }
}