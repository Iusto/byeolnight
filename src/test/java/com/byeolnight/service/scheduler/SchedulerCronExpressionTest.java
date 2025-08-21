package com.byeolnight.service.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerCronExpressionTest {

    @Test
    @DisplayName("게시글 정리 스케줄러 - 매일 8시 실행 확인")
    void testPostCleanupCronExpression() {
        CronExpression cron = CronExpression.parse("0 0 8 * * *");
        
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 7, 59, 59);
        ZonedDateTime zonedNow = now.atZone(ZoneId.systemDefault());
        
        ZonedDateTime nextExecution = cron.next(zonedNow);
        
        assertThat(nextExecution.getHour()).isEqualTo(8);
        assertThat(nextExecution.getMinute()).isEqualTo(0);
        assertThat(nextExecution.getSecond()).isEqualTo(0);
    }

    @Test
    @DisplayName("뉴스 수집 스케줄러 - 매일 8시 (한국시간) 실행 확인")
    void testNewsCollectionCronExpression() {
        CronExpression cron = CronExpression.parse("0 0 8 * * *");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 7, 59, 59);
        ZonedDateTime zonedNow = now.atZone(koreaZone);
        
        ZonedDateTime nextExecution = cron.next(zonedNow);
        
        assertThat(nextExecution.getHour()).isEqualTo(8);
        assertThat(nextExecution.getZone()).isEqualTo(koreaZone);
    }

    @Test
    @DisplayName("토론 주제 생성 스케줄러 - 매일 8시 5분 실행 확인")
    void testDiscussionTopicCronExpression() {
        CronExpression cron = CronExpression.parse("0 5 8 * * *");
        
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 8, 4, 59);
        ZonedDateTime zonedNow = now.atZone(ZoneId.of("Asia/Seoul"));
        
        ZonedDateTime nextExecution = cron.next(zonedNow);
        
        assertThat(nextExecution.getHour()).isEqualTo(8);
        assertThat(nextExecution.getMinute()).isEqualTo(5);
    }

    @Test
    @DisplayName("쪽지 정리 스케줄러 - 매일 8시 실행 확인")
    void testMessageCleanupCronExpression() {
        CronExpression cron = CronExpression.parse("0 0 8 * * ?");
        
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 7, 59, 59);
        ZonedDateTime zonedNow = now.atZone(ZoneId.systemDefault());
        
        ZonedDateTime nextExecution = cron.next(zonedNow);
        
        assertThat(nextExecution.getHour()).isEqualTo(8);
        assertThat(nextExecution.getMinute()).isEqualTo(0);
    }

    @Test
    @DisplayName("소셜 계정 정리 스케줄러 - 매일 9시, 10시 실행 확인")
    void testSocialAccountCleanupCronExpressions() {
        CronExpression maskingCron = CronExpression.parse("0 0 9 * * *");
        CronExpression deletionCron = CronExpression.parse("0 0 10 * * *");
        
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 8, 59, 59);
        ZonedDateTime zonedNow = now.atZone(ZoneId.systemDefault());
        
        ZonedDateTime nextMasking = maskingCron.next(zonedNow);
        ZonedDateTime nextDeletion = deletionCron.next(zonedNow);
        
        assertThat(nextMasking.getHour()).isEqualTo(9);
        assertThat(nextDeletion.getHour()).isEqualTo(10);
    }

    @Test
    @DisplayName("탈퇴 회원 정리 스케줄러 - 매일 10시 실행 확인")
    void testWithdrawnUserCleanupCronExpression() {
        CronExpression cron = CronExpression.parse("0 0 10 * * *");
        
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 9, 59, 59);
        ZonedDateTime zonedNow = now.atZone(ZoneId.systemDefault());
        
        ZonedDateTime nextExecution = cron.next(zonedNow);
        
        assertThat(nextExecution.getHour()).isEqualTo(10);
        assertThat(nextExecution.getMinute()).isEqualTo(0);
    }

    @Test
    @DisplayName("뉴스 재시도 스케줄러 - 8시 5분, 10분 실행 확인")
    void testNewsRetryCronExpressions() {
        CronExpression firstRetry = CronExpression.parse("0 5 8 * * *");
        CronExpression finalRetry = CronExpression.parse("0 10 8 * * *");
        
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 8, 4, 59);
        ZonedDateTime zonedNow = now.atZone(ZoneId.of("Asia/Seoul"));
        
        ZonedDateTime nextFirstRetry = firstRetry.next(zonedNow);
        ZonedDateTime nextFinalRetry = finalRetry.next(zonedNow);
        
        assertThat(nextFirstRetry.getMinute()).isEqualTo(5);
        assertThat(nextFinalRetry.getMinute()).isEqualTo(10);
    }
}