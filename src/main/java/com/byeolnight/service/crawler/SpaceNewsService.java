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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        
        for (NewsApiResponseDto.Result result : response.getResults()) {
            if (isDuplicateNews(result)) {
                duplicateCount++;
                log.debug("중복 뉴스 스킵: {}", result.getTitle());
                continue;
            }
            
            if (!isRelevantSpaceNews(result)) {
                duplicateCount++; // 필터링된 것도 스킵 카운트에 포함
                log.debug("우주 관련성 부족으로 스킵: {}", result.getTitle());
                continue;
            }
            
            // News 엔티티에 저장
            News news = convertToNews(result);
            newsRepository.save(news);
            
            // Post 엔티티로 변환하여 게시판에 표시
            Post post = convertToPost(result, newsBot);
            Post savedPost = postRepository.save(post);
            savedPosts.add(savedPost);
            
            log.info("새 뉴스 게시글 저장: {}", savedPost.getTitle());
        }
        
        log.info("한국어 우주 뉴스 수집 완료 - 저장: {}건, 중복 스킵: {}건", savedPosts.size(), duplicateCount);
    }
    
    private boolean isDuplicateNews(NewsApiResponseDto.Result result) {
        return newsRepository.existsByTitle(result.getTitle()) || 
               newsRepository.existsByUrl(result.getLink()) ||
               postRepository.existsByTitle(result.getTitle());
    }
    
    private boolean isRelevantSpaceNews(NewsApiResponseDto.Result result) {
        String title = (result.getTitle() != null ? result.getTitle() : "").toLowerCase();
        String description = (result.getDescription() != null ? result.getDescription() : "").toLowerCase();
        String content = title + " " + description;
        
        // 우주 관련 핵심 키워드 (최소 하나는 포함되어야 함)
        String[] spaceKeywords = {
            "우주", "space", "천문", "astronomy", "항공우주", "aerospace",
            "로켓", "rocket", "위성", "satellite", "우주선", "spacecraft",
            "화성", "mars", "달 탐사", "moon mission", "태양계", "solar system",
            "은하", "galaxy", "블랙홀", "black hole", "우주정거장", "space station",
            "우주비행사", "astronaut", "우주발사", "space launch", "우주탐사", "space exploration",
            "망원경", "telescope", "혜성", "comet", "소행성", "asteroid"
        };
        
        boolean hasSpaceKeyword = false;
        for (String keyword : spaceKeywords) {
            if (content.contains(keyword)) {
                hasSpaceKeyword = true;
                break;
            }
        }
        
        // 우주 키워드가 없으면 제외
        if (!hasSpaceKeyword) {
            log.debug("우주 관련 키워드 없음으로 제외: {}", title);
            return false;
        }
        
        // 교육/연수 관련 기사 제외 (NASA 방문이 부차적인 경우)
        String[] educationKeywords = {
            "연수", "교육", "장학", "학생", "대학", "캠프", "체험", "견학",
            "training", "education", "scholarship", "student", "university", "camp", "visit"
        };
        
        for (String keyword : educationKeywords) {
            if (content.contains(keyword)) {
                // NASA나 우주센터 방문이 주요 내용이 아닌 경우 제외
                if (!content.contains("nasa 발표") && !content.contains("nasa announces") && 
                    !content.contains("우주 연구") && !content.contains("space research")) {
                    log.debug("교육/연수 관련 기사로 제외: {}", title);
                    return false;
                }
            }
        }
        
        return true;
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
        // 영어 제목인 경우 간단한 번역 처리
        if (isEnglishTitle(title)) {
            return "[해외뉴스] " + title;
        }
        return title;
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