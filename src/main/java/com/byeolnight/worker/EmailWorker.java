package com.byeolnight.worker;

import com.byeolnight.dto.auth.EmailJob;
import com.byeolnight.infrastructure.cache.RedissonCacheService;
import com.byeolnight.service.auth.GmailEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 큐 기반 비동기 이메일 전송 워커
 * - 메일 전송 작업을 큐에서 가져와 처리
 * - 실패 시 재시도 (최대 5회)
 * - 5회 실패 시 DLQ로 이동
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class EmailWorker {

    private static final String MAIL_QUEUE = "queue:mail";
    private static final String DLQ = "queue:mail:dlq";
    private static final int MAX_ATTEMPTS = 5;

    private final RedissonCacheService cacheService;
    private final GmailEmailService gmailEmailService;

    /**
     * 1초마다 메일 전송 작업 처리
     */
    @Scheduled(fixedDelay = 1000)
    public void processEmailJobs() {
        try {
            // 큐에서 작업 가져오기 (1초 대기)
            EmailJob emailJob = cacheService.dequeue(MAIL_QUEUE, Duration.ofSeconds(1));

            if (emailJob != null) {
                processMessage(emailJob);
            }
        } catch (Exception e) {
            log.error("워커 실행 실패", e);
        }
    }

    /**
     * 개별 메시지 처리
     */
    private void processMessage(EmailJob emailJob) {
        log.info("이메일 전송 작업 처리 시작: jobId={}, email={}, attempt={}",
                emailJob.getJobId(), emailJob.getEmail(), emailJob.getAttempt());

        try {
            // 메일 전송
            gmailEmailService.sendHtml(emailJob.getEmail(), emailJob.getSubject(), emailJob.getHtmlBody());
            log.info("이메일 전송 성공: jobId={}, email={}", emailJob.getJobId(), emailJob.getEmail());

            // 성공 시 완료 (큐에서 이미 제거됨)
        } catch (Exception e) {
            log.warn("이메일 전송 실패: jobId={}, email={}, attempt={}/{}, error={}",
                    emailJob.getJobId(), emailJob.getEmail(), emailJob.getAttempt() + 1, MAX_ATTEMPTS, e.getMessage());

            // 재시도 처리
            handleRetry(emailJob, e.getMessage());
        }
    }

    /**
     * 재시도 처리
     */
    private void handleRetry(EmailJob emailJob, String errorMessage) {
        int newAttempt = emailJob.getAttempt() + 1;

        if (newAttempt >= MAX_ATTEMPTS) {
            // DLQ로 이동
            moveToDLQ(emailJob, errorMessage);
            log.error("이메일 전송 최종 실패 - DLQ로 이동: jobId={}", emailJob.getJobId());
        } else {
            // 재시도를 위해 attempt 증가 후 다시 큐에 추가
            EmailJob retryJob = emailJob.withRetry(errorMessage);
            cacheService.enqueue(MAIL_QUEUE, retryJob);
            log.info("이메일 재시도 예약: jobId={}, attempt={}", emailJob.getJobId(), newAttempt);
        }
    }

    /**
     * DLQ로 이동
     */
    private void moveToDLQ(EmailJob emailJob, String finalError) {
        EmailJob dlqJob = emailJob.withFinalFailure(finalError);
        cacheService.enqueue(DLQ, dlqJob);
        log.info("DLQ로 이동 완료: jobId={}", emailJob.getJobId());
    }
}