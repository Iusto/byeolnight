package com.byeolnight.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 전체 애플리케이션 설정 Properties
 * Spring Cloud Config를 통해 중앙화된 관리
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class SecurityProperties {
    
    /**
     * 보안 관련 설정
     */
    private Security security = new Security();
    
    /**
     * 시스템 관련 설정
     */
    private System system = new System();
    
    /**
     * 프론트엔드 관련 설정
     */
    private Frontend frontend = new Frontend();
    
    /**
     * 뉴스 API 관련 설정
     */
    private Newsdata newsdata = new Newsdata();
    
    @Data
    public static class Security {
        private Jwt jwt = new Jwt();
        private Database database = new Database();
        private ExternalApi externalApi = new ExternalApi();
    }
    
    @Data
    public static class System {
        private Passwords passwords = new Passwords();
        
        @Data
        public static class Passwords {
            private String newsbot;
            private String system;
        }
    }
    
    @Data
    public static class Frontend {
        private String apiBaseUrl;
        private String wsUrl;
    }
    
    @Data
    public static class Newsdata {
        private String apiKeyBackup;
    }
    
    @Data
    public static class Jwt {
        private String secret;
        private long accessExpiration = 3600000; // 1시간
        private long refreshExpiration = 604800000; // 7일
    }
    
    @Data
    public static class Database {
        private String password;
        private Redis redis = new Redis();
        
        @Data
        public static class Redis {
            private String password;
        }
    }
    
    @Data
    public static class ExternalApi {
        private Aws aws = new Aws();
        private Email email = new Email();
        private Sms sms = new Sms();
        private Ai ai = new Ai();
        
        @Data
        public static class Aws {
            private String accessKeyId;
            private String secretAccessKey;
            private String region = "ap-northeast-2";
            private String s3BucketName;
        }
        
        @Data
        public static class Email {
            private String gmailUsername;
            private String gmailPassword;
        }
        
        @Data
        public static class Sms {
            private String coolsmsApiKey;
            private String coolsmsApiSecret;
            private String fromNumber;
        }
        
        @Data
        public static class Ai {
            private String openaiApiKey;
            private String googleApiKey;
            private String newsdataApiKey;
        }
    }
}