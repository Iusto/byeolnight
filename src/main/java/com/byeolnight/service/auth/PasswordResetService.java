package com.byeolnight.service.auth;

import com.byeolnight.entity.token.PasswordResetToken;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.PasswordResetTokenRepository;
import com.byeolnight.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserService userService;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public void sendPasswordResetEmail(String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("비밀번호 재설정 요청 - 존재하지 않는 이메일: {}", email);
            return; // 보안상 이메일 존재 여부를 노출하지 않음
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.create(email, token, Duration.ofMinutes(30));
        passwordResetTokenRepository.save(resetToken);

        try {
            sendResetEmail(email, token);
            log.info("비밀번호 재설정 이메일 전송 완료: {}", email);
        } catch (MessagingException e) {
            log.error("비밀번호 재설정 이메일 전송 실패: {}", email, e);
            throw new RuntimeException("이메일 전송에 실패했습니다.");
        }
    }

    public PasswordResetToken validateToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("만료되었거나 이미 사용된 토큰입니다.");
        }

        return resetToken;
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("만료되었거나 이미 사용된 토큰입니다.");
        }

        User user = userService.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 소셜 로그인 사용자 체크
        if (user.isSocialUser()) {
            String providerName = user.getSocialProviderName();
            throw new IllegalArgumentException(
                String.format("소셜 로그인(%s) 계정입니다. %s에서 비밀번호를 변경해주세요.", 
                    providerName, providerName)
            );
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        // 비밀번호 재설정 시 계정 잠금 해제
        user.unlockAccount();
        resetToken.markAsUsed();

        log.info("비밀번호 재설정 완료 (계정 잠금 해제): {}", user.getEmail());
    }

    private void sendResetEmail(String email, String token) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(email);
        helper.setSubject("🌌 별 헤는 밤 - 비밀번호 재설정");
        helper.setText(createResetEmailContent(token), true);

        mailSender.send(message);
    }

    private String createResetEmailContent(String token) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { text-align: center; color: #4A90E2; margin-bottom: 30px; }
                    .content { background: #f9f9f9; padding: 20px; border-radius: 8px; }
                    .button { display: inline-block; padding: 12px 24px; background: #4A90E2; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🌌 별 헤는 밤</h1>
                        <h2>비밀번호 재설정</h2>
                    </div>
                    <div class="content">
                        <p>안녕하세요!</p>
                        <p>비밀번호 재설정을 요청하셨습니다. 아래 링크를 클릭하여 새로운 비밀번호를 설정해주세요.</p>
                        <p style="text-align: center;">
                            <a href="https://byeolnight.com/password-reset?token=%s" class="button">비밀번호 재설정하기</a>
                        </p>
                        <p><strong>주의사항:</strong></p>
                        <ul>
                            <li>이 링크는 30분 후에 만료됩니다.</li>
                            <li>비밀번호 재설정을 요청하지 않으셨다면 이 이메일을 무시해주세요.</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>© 2024 별 헤는 밤. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, token);
    }
}