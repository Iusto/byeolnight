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
        int duplicateCount = 0;
        
        int actualDuplicateCount = 0;
        int filteredCount = 0;
        
        for (NewsApiResponseDto.Result result : response.getResults()) {
            log.info("\n========== 뉴스 처리 시작 ==========\n제목: {}\nURL: {}", result.getTitle(), result.getLink());
            
            if (isDuplicateNews(result)) {
                actualDuplicateCount++;
                log.info("중복으로 스킵됨");
                continue;
            }
            
            if (!isRelevantSpaceNews(result)) {
                filteredCount++;
                log.info("필터링으로 스킵됨");
                continue;
            }
            
            log.info("저장 진행 중...");
            
            // News 엔티티에 저장
            News news = convertToNews(result);
            newsRepository.save(news);
            
            // Post 엔티티로 변환하여 게시판에 표시
            Post post = convertToPost(result, newsBot);
            Post savedPost = postRepository.save(post);
            savedPosts.add(savedPost);
            
            log.info("새 뉴스 게시글 저장: {}", savedPost.getTitle());
        }
        
        log.info("한국어 우주 뉴스 수집 완료 - 저장: {}건, 실제 중복: {}건, 필터링: {}건, 총 스킵: {}건", 
                savedPosts.size(), actualDuplicateCount, filteredCount, actualDuplicateCount + filteredCount);
    }
    
    private boolean isDuplicateNews(NewsApiResponseDto.Result result) {
        // URL 기준으로만 중복 체크 (제목은 번역되거나 수정될 수 있음)
        boolean isDuplicate = newsRepository.existsByUrl(result.getLink());
        
        log.info("=== 중복 체크 ===\n제목: {}\nURL: {}\n중복 여부: {}", 
                result.getTitle(), result.getLink(), isDuplicate);
        
        return isDuplicate;
    }
    
    private boolean isRelevantSpaceNews(NewsApiResponseDto.Result result) {
        String title = (result.getTitle() != null ? result.getTitle() : "").toLowerCase();
        String description = (result.getDescription() != null ? result.getDescription() : "").toLowerCase();
        String content = title + " " + description;
        
        // 우주 관련 핵심 키워드 (최소 하나는 포함되어야 함)
        String[] spaceKeywords = {
            "우주", "space", "천문", "astronomy", "항공우주", "aerospace",
            "로켓", "rocket", "위성", "satellite", "우주선", "spacecraft",
            "화성", "mars", "달", "moon", "태양", "sun", "solar",
            "은하", "galaxy", "블랙홀", "black hole", "우주정거장", "space station",
            "우주비행사", "astronaut", "우주발사", "space launch", "우주탐사", "space exploration",
            "망원경", "telescope", "혜성", "comet", "소행성", "asteroid",
            "nasa", "spacex", "우주센터", "space center", "probe"
        };
        
        // 우주 키워드 체크
        for (String keyword : spaceKeywords) {
            if (content.contains(keyword)) {
                return true; // 우주 관련 키워드가 있으면 통과
            }
        }
        
        // 우주 관련 키워드가 없으면 제외
        return false;
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
        
        // 뉴스 요약
        if (result.getDescription() != null && !result.getDescription().trim().isEmpty()) {
            content.append("## 📰 뉴스 요약\n\n");
            content.append(result.getDescription()).append("\n\n");
        } else {
            content.append("## 📰 뉴스 요약\n\n");
            content.append("이 뉴스는 우주와 천문학 관련 최신 소식을 다룹니다. 자세한 내용은 원문 링크를 통해 확인하세요.\n\n");
        }
        
        // 상세 내용 (무료 플랜에서는 제한됨)
        if (result.getContent() != null && !result.getContent().trim().isEmpty() && !result.getContent().contains("ONLY AVAILABLE IN PAID PLANS")) {
            content.append("## 📄 상세 내용\n\n");
            content.append(result.getContent()).append("\n\n");
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
                .summary("") // 현재는 비워둠
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
        
        if (content.contains("우주") || content.contains("space")) tags.add("#우주");
        if (content.contains("nasa")) tags.add("#NASA");
        if (content.contains("천문") || content.contains("astronomy")) tags.add("#천문학");
        if (content.contains("로켓") || content.contains("rocket")) tags.add("#로켓");
        if (content.contains("위성") || content.contains("satellite")) tags.add("#위성");
        if (content.contains("화성") || content.contains("mars")) tags.add("#화성");
        if (content.contains("달") || content.contains("moon")) tags.add("#달");
        if (content.contains("태양") || content.contains("sun")) tags.add("#태양");
        if (content.contains("행성") || content.contains("planet")) tags.add("#행성");
        if (content.contains("은하") || content.contains("galaxy")) tags.add("#은하");
        
        return String.join(" ", tags);
    }
    
    private String getDefaultSpaceImage() {
        return "https://images.unsplash.com/photo-1446776877081-d282a0f896e2?w=800&h=600&fit=crop";
    }
}