package com.byeolnight.service.discussion;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.post.PostRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.service.ai.NewsBasedDiscussionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscussionTopicScheduler {

    private final NewsBasedDiscussionService newsBasedDiscussionService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Value("${system.password.system}")
    private String systemRawPassword;

    @Scheduled(cron = "0 5 8 * * *", zone = "Asia/Seoul") // 매일 오전 8시 (한국 시간)
    @Transactional
    public void generateDailyDiscussionTopic() {
        log.info("일일 토론 주제 생성 시작 - {}", java.time.LocalDateTime.now());

        try {
            if (newsBasedDiscussionService == null) {
                log.error("NewsBasedDiscussionService가 주입되지 않았습니다!");
                return;
            }
            // 기존 토론 주제 비활성화
            deactivateOldTopics();

            // 시스템 사용자 조회 (토론봇)
            User systemUser = getSystemUser();

            // 새 토론 주제 생성 (뉴스 기반)
            String topicContent = generateUniqueTopicWithRetry();
            
            // 제목과 내용 파싱
            String[] parsed = parseTopicContent(topicContent);
            String title = parsed[0];
            String content = parsed[1];

            // 토론 주제 게시글 생성
            Post discussionPost = Post.builder()
                    .title(title)
                    .content(content)
                    .category(Post.Category.DISCUSSION)
                    .writer(systemUser)
                    .build();
            
            discussionPost.setAsDiscussionTopic();
            postRepository.save(discussionPost);

            log.info("새로운 토론 주제 생성 완료: {} - {}", title, java.time.LocalDateTime.now());

        } catch (Exception e) {
            log.error("토론 주제 생성 실패 - {}", java.time.LocalDateTime.now(), e);
        }
    }

    public void deactivateOldTopics() {
        List<Post> oldTopics = postRepository.findByDiscussionTopicTrueAndPinnedTrue();
        for (Post topic : oldTopics) {
            topic.unpin();
        }
        postRepository.saveAll(oldTopics);
        log.info("기존 토론 주제 {} 개 비활성화", oldTopics.size());
    }

    public User getSystemUser() {
        return userRepository.findByEmail("system@byeolnight.com")
                .orElseGet(() -> {
                    User systemUser = User.builder()
                            .email("system@byeolnight.com")
                            .nickname("별 헤는 밤")
                            .password(systemRawPassword)
                            .phone("000-0000-0000")
                            .role(User.Role.ADMIN)
                            .build();
                    return userRepository.save(systemUser);
                });
    }

    public String generateUniqueTopicWithRetry() {
        // 뉴스 수집에서 이미 중복 검사를 완료했으므로 간단한 체크만 수행
        String topicContent = newsBasedDiscussionService.generateNewsBasedDiscussion();
        
        // 기본적인 중복 체크 (오늘 날짜만)
        if (isDuplicatedToday(topicContent)) {
            log.warn("오늘 이미 비슷한 토론 주제 존재, fallback 사용");
            return newsBasedDiscussionService.generateNewsBasedDiscussion(); // fallback 시도
        }
        
        return topicContent;
    }
    
    /**
     * 오늘 날짜에만 중복 체크 (간소화된 버전)
     */
    private boolean isDuplicatedToday(String topicContent) {
        String[] parsed = parseTopicContent(topicContent);
        String title = parsed[0];
        
        // 오늘 날짜의 토론 주제만 체크
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        List<Post> todayTopics = postRepository.findByDiscussionTopicTrueAndCreatedAtAfter(todayStart);
        
        for (Post topic : todayTopics) {
            if (title.toLowerCase().contains(topic.getTitle().toLowerCase()) || 
                topic.getTitle().toLowerCase().contains(title.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }



    public String[] parseTopicContent(String content) {
        Pattern pattern = Pattern.compile("제목:\\s*(.+?)\\n내용:\\s*(.+)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            String title = matcher.group(1).trim();
            String description = matcher.group(2).trim();
            
            // 길이 제한 적용
            if (title.length() > 30) {
                title = title.substring(0, 27) + "...";
            }
            if (description.length() > 200) {
                description = description.substring(0, 197) + "...";
            }
            
            return new String[]{title, description};
        }
        
        // 파싱 실패 시 기본값
        return new String[]{"오늘의 우주 토론", "우주에 대한 흥미로운 주제로 토론해보세요."};
    }
}