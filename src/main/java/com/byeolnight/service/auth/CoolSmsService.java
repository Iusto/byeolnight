package com.byeolnight.service.auth;

import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.message.exception.NurigoEmptyResponseException;
import net.nurigo.sdk.message.exception.NurigoMessageNotReceivedException;
import net.nurigo.sdk.message.exception.NurigoUnknownException;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.service.DefaultMessageService;
import net.nurigo.sdk.NurigoApp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CoolSmsService {

    private final DefaultMessageService messageService;

    @Value("${coolsms.sender-number}")
    private String from;

    public CoolSmsService(@Value("${coolsms.api-key}") String apiKey,
                          @Value("${coolsms.api-secret}") String apiSecret) {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.coolsms.co.kr");
    }

    public void send(String to, String messageText) {
        Message message = new Message();
        message.setFrom(from);  // 반드시 등록된 발신 번호
        message.setTo(to);      // 수신자 번호 (본인 인증된 번호)
        message.setText(messageText);

        try {
            messageService.send(message);
        } catch (NurigoMessageNotReceivedException e) {
            log.error("❌ 메시지 전송 실패 - 수신 거부 또는 인증 미완료", e);
            throw new RuntimeException("메시지 전송 실패: " + e.getMessage());
        } catch (NurigoEmptyResponseException e) {
            log.error("❌ 응답 없음 - 네트워크 오류 또는 서버 다운", e);
            throw new RuntimeException("응답 없음: " + e.getMessage());
        } catch (NurigoUnknownException e) {
            log.error("❌ 알 수 없는 오류 발생", e);
            throw new RuntimeException("알 수 없는 오류: " + e.getMessage());
        }
    }

}
