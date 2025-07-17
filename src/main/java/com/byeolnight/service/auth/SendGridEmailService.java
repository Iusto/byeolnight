package com.byeolnight.service.auth;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 승인 거부되어 잠시 보류
 */
//@Service
@RequiredArgsConstructor
public class SendGridEmailService {

    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    public void send(String to, String subject, String content) {
        Email from = new Email(fromEmail);
        Email toEmail = new Email(to);
        Content body = new Content("text/plain", content);
        Mail mail = new Mail(from, subject, toEmail, body);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            sg.api(request);
        } catch (IOException ex) {
            throw new RuntimeException("이메일 전송 실패", ex);
        }
    }
}
