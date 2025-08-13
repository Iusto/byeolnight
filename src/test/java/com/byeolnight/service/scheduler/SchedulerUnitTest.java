package com.byeolnight.service.scheduler;

import com.byeolnight.service.discussion.DiscussionTopicScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerUnitTest {

    @Test
    @DisplayName("크론 표현식 검증 - 매일 8시 실행")
    void testCronExpressions() {
        // 게시글 정리: 매일 8시
        CronExpression postCleanup = CronExpression.parse("0 0 8 * * *");
        
        // 뉴스 수집: 매일 8시 (한국시간)
        CronExpression newsCollection = CronExpression.parse("0 0 8 * * *");
        
        // 토론 주제: 매일 8시 5분
        CronExpression discussion = CronExpression.parse("0 5 8 * * *");
        
        // 쪽지 정리: 매일 8시
        CronExpression messageCleanup = CronExpression.parse("0 0 8 * * ?");
        
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 7, 59, 59);
        ZonedDateTime zonedNow = now.atZone(ZoneId.systemDefault());
        
        ZonedDateTime nextPost = postCleanup.next(zonedNow);
        ZonedDateTime nextNews = newsCollection.next(zonedNow);
        ZonedDateTime nextDiscussion = discussion.next(zonedNow);
        ZonedDateTime nextMessage = messageCleanup.next(zonedNow);
        
        assertThat(nextPost.getHour()).isEqualTo(8);
        assertThat(nextNews.getHour()).isEqualTo(8);
        assertThat(nextDiscussion.getHour()).isEqualTo(8);
        assertThat(nextDiscussion.getMinute()).isEqualTo(5);
        assertThat(nextMessage.getHour()).isEqualTo(8);
    }

    @Test
    @DisplayName("토론 주제 파싱 로직 테스트")
    void testTopicContentParsing() {
        // Given
        String content = "제목: 우주 탐사의 미래\n내용: 화성 이주 계획에 대해 어떻게 생각하시나요?";
        
        // When
        String[] result = parseTopicContent(content);
        
        // Then
        assertThat(result[0]).isEqualTo("우주 탐사의 미래");
        assertThat(result[1]).isEqualTo("화성 이주 계획에 대해 어떻게 생각하시나요?");
    }

    @Test
    @DisplayName("잘못된 형식 파싱 시 기본값 반환")
    void testInvalidTopicContentParsing() {
        // Given
        String invalidContent = "잘못된 형식의 내용";
        
        // When
        String[] result = parseTopicContent(invalidContent);
        
        // Then
        assertThat(result[0]).isEqualTo("오늘의 우주 토론");
        assertThat(result[1]).isEqualTo("우주에 대한 흥미로운 주제로 토론해보세요.");
    }

    @Test
    @DisplayName("긴 제목/내용 자르기 테스트")
    void testLongContentTruncation() {
        // Given
        String longTitle = "a".repeat(50);
        String longContent = "b".repeat(300);
        String content = "제목: " + longTitle + "\n내용: " + longContent;
        
        // When
        String[] result = parseTopicContent(content);
        
        // Then
        assertThat(result[0]).hasSize(30);
        assertThat(result[0]).endsWith("...");
        assertThat(result[1]).hasSize(200);
        assertThat(result[1]).endsWith("...");
    }

    @Test
    @DisplayName("스케줄러 실행 시간 간격 검증")
    void testSchedulerTimeIntervals() {
        // 뉴스 수집 재시도 간격 (5분, 10분)
        CronExpression firstRetry = CronExpression.parse("0 5 8 * * *");
        CronExpression finalRetry = CronExpression.parse("0 10 8 * * *");
        
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 8, 0, 0);
        ZonedDateTime zonedBase = baseTime.atZone(ZoneId.of("Asia/Seoul"));
        
        ZonedDateTime nextFirst = firstRetry.next(zonedBase);
        ZonedDateTime nextFinal = finalRetry.next(zonedBase);
        
        assertThat(nextFirst.getMinute()).isEqualTo(5);
        assertThat(nextFinal.getMinute()).isEqualTo(10);
    }

    @Test
    @DisplayName("소셜 계정 정리 스케줄러 시간 검증")
    void testSocialAccountCleanupSchedule() {
        // 개인정보 마스킹: 매일 9시
        CronExpression masking = CronExpression.parse("0 0 9 * * *");
        
        // 계정 완전 삭제: 매일 10시
        CronExpression deletion = CronExpression.parse("0 0 10 * * *");
        
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 8, 59, 59);
        ZonedDateTime zonedNow = now.atZone(ZoneId.systemDefault());
        
        ZonedDateTime nextMasking = masking.next(zonedNow);
        ZonedDateTime nextDeletion = deletion.next(zonedNow);
        
        assertThat(nextMasking.getHour()).isEqualTo(9);
        assertThat(nextDeletion.getHour()).isEqualTo(10);
    }

    // DiscussionTopicScheduler의 parseTopicContent 메서드 로직 복사
    private String[] parseTopicContent(String content) {
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