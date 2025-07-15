package com.byeolnight.service.crawler;

import com.byeolnight.domain.entity.News;
import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.Post.Category;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.NewsRepository;
import com.byeolnight.domain.repository.post.PostRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.dto.ai.NewsApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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
    private final NewsDataService newsDataService;
    private final com.byeolnight.service.discussion.DiscussionTopicScheduler discussionTopicScheduler;
    private final com.byeolnight.infrastructure.config.NewsCollectionProperties newsConfig;
    
    @Transactional
    public void collectAndSaveSpaceNews() {
        log.info("한국어 우주 뉴스 수집 시작");
        
        NewsApiResponseDto response = newsDataService.fetchKoreanSpaceNews();
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
            
            // 설정된 최대 개수만 저장
            if (savedPosts.size() >= newsConfig.getCollection().getMaxPosts()) {
                log.info("이미 {}개 뉴스를 저장했으므로 종료", newsConfig.getCollection().getMaxPosts());
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
        
        log.info("우주 뉴스 수집 완료 - 수집: {}개, 저장: {}건, 실제 중복: {}건, 필터링: {}건", 
                response.getResults().size(), savedPosts.size(), actualDuplicateCount, filteredCount);
        
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
        
        // 설정된 최소 길이 체크
        if (checkTitle.length() < newsConfig.getQuality().getMinTitleLength()) {
            log.debug("제목이 너무 짧음: {}글자 ({})", checkTitle.length(), checkTitle);
            return false;
        }
        
        if (checkDescription.length() < newsConfig.getQuality().getMinDescriptionLength()) {
            log.debug("설명이 너무 짧음: {}글자", checkDescription.length());
            return false;
        }
        
        return true;
    }
    
    /**
     * 우주 관련성 체크 (캐싱된 200개 키워드 사용)
     */
    private boolean isSpaceRelated(String title, String description) {
        String content = (title + " " + description).toLowerCase();
        
        // 비우주 키워드 먼저 체크 (정치, 경제 등 제외)
        String[] excludeKeywords = {"trump", "obama", "democrat", "republican", "politics", "election", 
                                   "트럼프", "오바마", "정치", "선거", "경제", "주식", "코인"};
        for (String exclude : excludeKeywords) {
            if (content.contains(exclude)) {
                log.info("비우주 키워드 '{}' 발견으로 제외", exclude);
                return false;
            }
        }
        
        // 캐싱된 키워드 배열 사용 (성능 최적화)
        String[] allKeywords = newsDataService.getAllSpaceKeywordsCached();
        
        int keywordCount = 0;
        List<String> foundKeywords = new ArrayList<>();
        for (String keyword : allKeywords) {
            if (content.contains(keyword)) {
                keywordCount++;
                foundKeywords.add(keyword);
            }
        }
        
        log.info("우주 관련 키워드 {}개 발견: {}", keywordCount, foundKeywords.stream().limit(5).toList());
        return keywordCount >= newsConfig.getQuality().getMinSpaceKeywords();
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
        String apiKey = System.getProperty("openai.api.key", System.getenv("OPENAI_API_KEY"));
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.debug("OpenAI API 키가 설정되지 않아 번역 스킵");
            return null;
        }
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            String prompt = String.format("""
                다음 영어 뉴스 제목을 자연스럽고 정확한 한국어로 번역해주세요:
                
                "%s"
                
                요구사항:
                - 우주/과학 전문 용어는 정확하게 번역
                - 자연스럽고 읽기 쉬운 한국어로 번역
                - 번역문만 반환 (설명 없이)
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
            log.warn("번역 실패: {}", englishTitle, e);
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
        StringBuilder content = new StringBuilder();
        
        // 뉴스 이미지 (있는 경우)
        if (result.getImageUrl() != null && !result.getImageUrl().trim().isEmpty()) {
            content.append("![뉴스 이미지](").append(result.getImageUrl()).append(")\n\n");
        }
        
        // 뉴스 요약 (영어인 경우 번역)
        if (result.getDescription() != null && !result.getDescription().trim().isEmpty()) {
            content.append("## 📰 뉴스 요약\n\n");
            String description = result.getDescription();
            if (isEnglishTitle(result.getTitle())) {
                String translatedDesc = translateWithOpenAI(description);
                description = translatedDesc != null ? translatedDesc : description;
            }
            content.append(description).append("\n\n");
        } else {
            content.append("## 📰 뉴스 요약\n\n");
            content.append("이 뉴스는 우주와 천문학 관련 최신 소식을 다룹니다. 자세한 내용은 원문 링크를 통해 확인하세요.\n\n");
        }
        
        // 상세 내용 (무료 플랜에서는 제한됨)
        if (result.getContent() != null && !result.getContent().trim().isEmpty() && !result.getContent().contains("ONLY AVAILABLE IN PAID PLANS")) {
            content.append("## 📄 상세 내용\n\n");
            String contentText = result.getContent();
            if (isEnglishTitle(result.getTitle())) {
                String translatedContent = translateWithOpenAI(contentText);
                contentText = translatedContent != null ? translatedContent : contentText;
            }
            content.append(contentText).append("\n\n");
        } else {
            content.append("## 📄 상세 내용\n\n");
            content.append("상세한 내용은 아래 원문 링크를 통해 확인하실 수 있습니다.\n\n");
        }
        
        // 원문 링크
        content.append("## 🔗 원문 보기\n\n");
        content.append("[📰 원문 기사 보기](").append(result.getLink()).append(")\n\n");
        
        // 출처 정보
        content.append("---\n\n");
        if (result.getSourceName() != null) {
            content.append("**출처:** ").append(result.getSourceName()).append("\n");
        }
        
        if (result.getPubDate() != null) {
            content.append("**발행일:** ").append(result.getPubDate()).append("\n\n");
        }
        
        // 해시태그
        String hashtags = generateHashtags(result.getTitle(), result.getDescription());
        if (!hashtags.isEmpty()) {
            content.append(hashtags);
        }
        
        return content.toString();
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
        
        // 200개 키워드 기반 해시태그 생성
        String[] koreanKeywords = newsDataService.getKoreanSpaceKeywords();
        String[] englishKeywords = newsDataService.getEnglishSpaceKeywords();
        
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
     * AI 기반 뉴스 요약 생성
     */
    private String generateSummary(NewsApiResponseDto.Result result) {
        String apiKey = System.getProperty("openai.api.key", System.getenv("OPENAI_API_KEY"));
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return result.getDescription() != null ? 
                result.getDescription().substring(0, Math.min(100, result.getDescription().length())) + "..." : 
                "우주 관련 최신 뉴스";
        }
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
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
                    return ((String) message.get("content")).trim();
                }
            }
        } catch (Exception e) {
            log.warn("요약 생성 실패: {}", result.getTitle(), e);
        }
        
        return result.getDescription() != null ? 
            result.getDescription().substring(0, Math.min(100, result.getDescription().length())) + "..." : 
            "우주 관련 최신 뉴스";
    }
}