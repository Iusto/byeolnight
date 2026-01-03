package com.byeolnight.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * 이메일 전송 작업 DTO
 * - Redis 큐에서 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailJob implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private String email;
    private String subject;
    private String htmlBody;

    @Builder.Default
    private int attempt = 0;

    private String createdAt;
    private String lastAttemptAt;
    private String errorMessage;

    /**
     * attempt 증가 및 오류 정보 업데이트
     */
    public EmailJob withRetry(String errorMessage) {
        return EmailJob.builder()
                .jobId(this.jobId)
                .email(this.email)
                .subject(this.subject)
                .htmlBody(this.htmlBody)
                .attempt(this.attempt + 1)
                .createdAt(this.createdAt)
                .lastAttemptAt(Instant.now().toString())
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * DLQ 이동용 최종 실패 정보 추가
     */
    public EmailJob withFinalFailure(String finalError) {
        return EmailJob.builder()
                .jobId(this.jobId)
                .email(this.email)
                .subject(this.subject)
                .htmlBody(this.htmlBody)
                .attempt(this.attempt)
                .createdAt(this.createdAt)
                .lastAttemptAt(Instant.now().toString())
                .errorMessage(finalError)
                .build();
    }
}