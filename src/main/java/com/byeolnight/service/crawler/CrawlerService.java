package com.byeolnight.service.crawler;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.dto.ai.NewsDto;

import com.byeolnight.service.post.PostService;
import com.byeolnight.domain.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService {
    
    private final PostService postService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    
    @Transactional
    public void processNewsData(NewsDto newsDto) {
        try {
            // 뉴스봇 계정 조회
            User newsBot = getNewsBotUser();
            
            // 뉴스는 매번 새 게시글로 등록 (중복 처리 없음)
            String formattedContent = formatNewsContent(newsDto);
            
            postService.createNewsPost(
                newsDto.getTitle(),
                formattedContent,
                Post.Category.NEWS,
                newsBot
            );
            
            log.info("뉴스 게시글 등록 완료: {}", newsDto.getTitle());
            
        } catch (Exception e) {
            log.error("뉴스 처리 중 오류 발생 - 제목: {}, 오류: {}", newsDto.getTitle(), e.getMessage(), e);
            throw new RuntimeException("뉴스 처리 실패", e);
        }
    }
    
    private User getNewsBotUser() {
        return userRepository.findByEmail("newsbot@byeolnight.com")
                .orElseThrow(() -> new RuntimeException("뉴스봇 사용자를 찾을 수 없습니다."));
    }
    

    
    private String formatNewsContent(NewsDto newsDto) {
        StringBuilder content = new StringBuilder();
        
        content.append(newsDto.getContent()).append("\n\n");
        
        // 출처 정보 추가
        if (newsDto.getSource() != null) {
            content.append("**출처:** ").append(newsDto.getSource()).append("\n");
        }
        
        // 원본 URL 추가
        if (newsDto.getUrl() != null) {
            content.append("**원문 링크:** [바로가기](").append(newsDto.getUrl()).append(")\n");
        }
        
        // 발행 시간 추가
        if (newsDto.getPublishedAt() != null) {
            content.append("**발행 시간:** ").append(newsDto.getPublishedAt()).append("\n");
        }
        
        // 태그 추가
        if (newsDto.getTags() != null && newsDto.getTags().length > 0) {
            content.append("**태그:** ");
            for (int i = 0; i < newsDto.getTags().length; i++) {
                content.append("#").append(newsDto.getTags()[i]);
                if (i < newsDto.getTags().length - 1) {
                    content.append(" ");
                }
            }
        }
        
        return content.toString();
    }
    

}