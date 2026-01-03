package com.byeolnight.service.auth;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * HTML 이메일 전송 서비스
 */
@Service
@RequiredArgsConstructor
public class GmailEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.security.external-api.email.mail-from}")
    private String fromEmail;

    /**
     * HTML 이메일 전송
     */
    public void sendHtml(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom(fromEmail);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("이메일 전송 중 예상치 못한 오류 발생: " + e.getMessage(), e);
        }
    }
}
