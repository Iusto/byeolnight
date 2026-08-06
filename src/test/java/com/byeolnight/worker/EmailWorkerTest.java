package com.byeolnight.worker;

import com.byeolnight.dto.auth.EmailJob;
import com.byeolnight.infrastructure.cache.RedisCacheService;
import com.byeolnight.service.auth.GmailEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailWorkerTest {

    private static final Instant NOW = Instant.parse("2026-08-06T01:00:00Z");

    @Mock
    private RedisCacheService cacheService;

    @Mock
    private GmailEmailService gmailEmailService;

    private EmailWorker emailWorker;

    @BeforeEach
    void setUp() {
        emailWorker = new EmailWorker(
                cacheService,
                gmailEmailService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("메일 전송 실패 시 즉시 재큐잉하지 않고 지수 백오프로 예약한다")
    void schedulesRetryWithBackoff() {
        EmailJob job = emailJob(0);
        when(cacheService.dequeueDue("queue:mail:retry", NOW, EmailJob.class)).thenReturn(null);
        when(cacheService.dequeue("queue:mail", Duration.ofSeconds(1), EmailJob.class)).thenReturn(job);
        doThrow(new RuntimeException("SMTP 장애"))
                .when(gmailEmailService).sendHtml(job.getEmail(), job.getSubject(), job.getHtmlBody());

        emailWorker.processEmailJobs();

        ArgumentCaptor<EmailJob> jobCaptor = ArgumentCaptor.forClass(EmailJob.class);
        ArgumentCaptor<Instant> dueAtCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(cacheService).enqueueDelayed(eq("queue:mail:retry"), jobCaptor.capture(), dueAtCaptor.capture());
        verify(cacheService, never()).enqueue(eq("queue:mail"), any());

        EmailJob retryJob = jobCaptor.getValue();
        assertAll(
                () -> assertThat(retryJob.getAttempt()).isEqualTo(1),
                () -> assertThat(retryJob.getLastAttemptAt()).isEqualTo(NOW.toString()),
                () -> assertThat(retryJob.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(5).toString()),
                () -> assertThat(retryJob.getErrorMessage()).isEqualTo("SMTP 장애"),
                () -> assertThat(dueAtCaptor.getValue()).isEqualTo(NOW.plusSeconds(5))
        );
    }

    @Test
    @DisplayName("실행 시각이 지난 재시도 작업을 신규 작업보다 먼저 처리한다")
    void processesDueRetryFirst() {
        EmailJob retryJob = emailJob(2);
        when(cacheService.dequeueDue("queue:mail:retry", NOW, EmailJob.class)).thenReturn(retryJob);

        emailWorker.processEmailJobs();

        verify(gmailEmailService).sendHtml(retryJob.getEmail(), retryJob.getSubject(), retryJob.getHtmlBody());
        verify(cacheService, never()).dequeue(anyString(), any(), eq(EmailJob.class));
    }

    @Test
    @DisplayName("다섯 번째 전송 실패는 최종 횟수를 기록해 DLQ로 이동한다")
    void movesFifthFailureToDlq() {
        EmailJob job = emailJob(4);
        when(cacheService.dequeueDue("queue:mail:retry", NOW, EmailJob.class)).thenReturn(job);
        doThrow(new RuntimeException("SMTP 장애"))
                .when(gmailEmailService).sendHtml(job.getEmail(), job.getSubject(), job.getHtmlBody());

        emailWorker.processEmailJobs();

        ArgumentCaptor<EmailJob> jobCaptor = ArgumentCaptor.forClass(EmailJob.class);
        verify(cacheService).enqueue(eq("queue:mail:dlq"), jobCaptor.capture());
        verify(cacheService, never()).enqueueDelayed(anyString(), any(), any());
        assertAll(
                () -> assertThat(jobCaptor.getValue().getAttempt()).isEqualTo(5),
                () -> assertThat(jobCaptor.getValue().getLastAttemptAt()).isEqualTo(NOW.toString()),
                () -> assertThat(jobCaptor.getValue().getNextAttemptAt()).isNull()
        );
    }

    @Test
    @DisplayName("재시도 간격은 5초부터 지수적으로 증가한다")
    void calculatesExponentialBackoff() {
        assertAll(
                () -> assertThat(EmailWorker.retryDelayFor(1)).isEqualTo(Duration.ofSeconds(5)),
                () -> assertThat(EmailWorker.retryDelayFor(2)).isEqualTo(Duration.ofSeconds(10)),
                () -> assertThat(EmailWorker.retryDelayFor(3)).isEqualTo(Duration.ofSeconds(20)),
                () -> assertThat(EmailWorker.retryDelayFor(4)).isEqualTo(Duration.ofSeconds(40))
        );
    }

    private EmailJob emailJob(int attempt) {
        return EmailJob.builder()
                .jobId("job-1")
                .email("user@example.com")
                .subject("인증 메일")
                .htmlBody("<p>인증 코드</p>")
                .attempt(attempt)
                .createdAt(NOW.minusSeconds(10).toString())
                .build();
    }
}
