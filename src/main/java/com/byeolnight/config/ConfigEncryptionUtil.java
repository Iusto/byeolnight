package com.byeolnight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Config Server에서 관리되는 암호화된 설정값들을 처리하는 유틸리티
 */
@Component
@RefreshScope
public class ConfigEncryptionUtil {

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Value("${app.security.external-api.aws.access-key-id}")
    private String awsAccessKey;

    @Value("${app.security.external-api.aws.secret-access-key}")
    private String awsSecretKey;

    @Value("${app.security.external-api.email.gmail-password}")
    private String gmailPassword;

    @Value("${app.security.external-api.sms.coolsms-api-key}")
    private String coolsmsApiKey;

    @Value("${app.security.external-api.sms.coolsms-api-secret}")
    private String coolsmsApiSecret;

    @Value("${app.security.external-api.ai.openai-api-key}")
    private String openaiApiKey;

    @Value("${app.security.external-api.ai.google-api-key}")
    private String googleApiKey;

    @Value("${system.password.newsbot}")
    private String newsbotPassword;

    @Value("${system.password.system}")
    private String systemPassword;

    // Getter 메서드들
    public String getJwtSecret() { return jwtSecret; }
    public String getAwsAccessKey() { return awsAccessKey; }
    public String getAwsSecretKey() { return awsSecretKey; }
    public String getGmailPassword() { return gmailPassword; }
    public String getCoolsmsApiKey() { return coolsmsApiKey; }
    public String getCoolsmsApiSecret() { return coolsmsApiSecret; }
    public String getOpenaiApiKey() { return openaiApiKey; }
    public String getGoogleApiKey() { return googleApiKey; }
    public String getNewsbotPassword() { return newsbotPassword; }
    public String getSystemPassword() { return systemPassword; }
}