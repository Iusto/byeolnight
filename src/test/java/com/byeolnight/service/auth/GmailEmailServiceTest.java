package com.byeolnight.service.auth;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

class GmailEmailServiceTest {

    private JavaMailSender mailSender;
    private GmailEmailService gmailEmailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        gmailEmailService = new GmailEmailService(mailSender);
        ReflectionTestUtils.setField(gmailEmailService, "fromEmail", "test@byeolnight.com"); // ★ 핵심!
    }

    @Test
    @DisplayName("이메일 전송 성공")
    void testSendEmail() {
        MimeMessage message = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(message);

        gmailEmailService.send("to@example.com", "제목", "본문");

        verify(mailSender).send(any(MimeMessage.class));
    }
}
