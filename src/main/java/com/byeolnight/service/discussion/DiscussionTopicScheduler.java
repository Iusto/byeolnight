package com.byeolnight.service.discussion;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.post.PostRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.service.ai.OpenAIService;
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

    private final OpenAIService openAIService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Value("${system.password.system}")
    private String systemRawPassword;

    @Scheduled(cron = "0 0 8 * * *") // 매일 오전 8시
    @Transactional
    public void generateDailyDiscussionTopic() {
        log.info("일일 토론 주제 생성 시작");

        try {
            // 기존 토론 주제 비활성화
            deactivateOldTopics();

            // 시스템 사용자 조회 (토론봇)
            User systemUser = getSystemUser();

            // 새 토론 주제 생성
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

            log.info("새로운 토론 주제 생성 완료: {}", title);

        } catch (Exception e) {
            log.error("토론 주제 생성 실패", e);
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
        int maxRetries = 3;
        
        for (int i = 0; i < maxRetries; i++) {
            String topicContent = openAIService.generateDiscussionTopic();
            
            if (isUniqueContent(topicContent)) {
                return topicContent;
            }
            
            log.warn("중복된 토론 주제 감지, 재시도 {}/{}", i + 1, maxRetries);
        }
        
        log.warn("최대 재시도 횟수 초과, fallback 주제 사용");
        return openAIService.generateDiscussionTopic();
    }

    private boolean isUniqueContent(String topicContent) {
        String[] parsed = parseTopicContent(topicContent);
        String title = parsed[0];
        String content = parsed[1];

        // 최근 60일간의 토론 주제와 비교
        LocalDateTime since = LocalDateTime.now().minusDays(60);
        List<Post> recentTopics = postRepository.findByDiscussionTopicTrueAndCreatedAtAfter(since);

        for (Post topic : recentTopics) {
            if (isSimilarContent(title, topic.getTitle()) || 
                isSimilarContent(content, topic.getContent())) {
                return false;
            }
        }
        
        return true;
    }

    private boolean isSimilarContent(String content1, String content2) {
        // 간단한 유사도 검사 (포함 관계)
        String normalized1 = content1.toLowerCase().replaceAll("\\s+", "");
        String normalized2 = content2.toLowerCase().replaceAll("\\s+", "");
        
        return normalized1.contains(normalized2) || 
               normalized2.contains(normalized1) ||
               calculateSimilarity(normalized1, normalized2) > 0.7;
    }

    private double calculateSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        
        int editDistance = levenshteinDistance(s1, s2);
        return 1.0 - (double) editDistance / maxLength;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1),
                        dp[i-1][j-1] + (s1.charAt(i-1) == s2.charAt(j-1) ? 0 : 1)
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
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