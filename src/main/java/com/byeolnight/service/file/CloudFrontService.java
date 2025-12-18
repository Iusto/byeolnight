package com.byeolnight.service.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * CloudFront Signed URL 생성 서비스
 * - 보안: S3 직접 접근 차단
 * - 성능: 엣지 캐싱으로 빠른 응답
 * - 비용: 데이터 전송비 절약
 */
@Slf4j
@Service
public class CloudFrontService {

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudFrontDomain;
    
    @Value("${cloud.aws.cloudfront.key-pair-id}")
    private String keyPairId;
    
    @Value("${cloud.aws.cloudfront.private-key-content}")
    private String privateKeyContent;

    /**
     * 이미지 조회용 CloudFront Signed URL 생성
     * @param s3Key S3 객체 키
     * @param expirationMinutes 만료 시간(분)
     * @return Signed URL
     */
    public String generateSignedUrl(String s3Key, int expirationMinutes) {
        try {
            // CloudFront URL 구성
            String resourceUrl = String.format("https://%s/%s", cloudFrontDomain, s3Key);
            
            // 만료 시간 설정
            Instant expiration = Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
            
            // Private Key 내용을 임시 파일로 생성
            Path tempKeyFile = createTempPrivateKeyFile(privateKeyContent);
            
            // Signed URL 생성
            CloudFrontUtilities utilities = CloudFrontUtilities.create();
            CannedSignerRequest signerRequest = CannedSignerRequest.builder()
                    .resourceUrl(resourceUrl)
                    .privateKey(tempKeyFile)
                    .keyPairId(keyPairId)
                    .expirationDate(expiration)
                    .build();
            
            SignedUrl signedUrl = utilities.getSignedUrlWithCannedPolicy(signerRequest);
            
            // 임시 파일 삭제
            tempKeyFile.toFile().delete();
            
            log.info("CloudFront Signed URL 생성: s3Key={}, expires={}", s3Key, expiration);
            return signedUrl.url();
            
        } catch (Exception e) {
            log.error("CloudFront Signed URL 생성 실패: s3Key={}", s3Key, e);
            throw new RuntimeException("이미지 URL 생성에 실패했습니다.", e);
        }
    }

    /**
     * 공개 이미지용 일반 CloudFront URL (Signed 불필요)
     */
    public String getPublicUrl(String s3Key) {
        return String.format("https://%s/%s", cloudFrontDomain, s3Key);
    }
    
    /**
     * Private Key 내용을 임시 파일로 생성
     */
    private Path createTempPrivateKeyFile(String privateKeyContent) throws Exception {
        // 헤더/푸터가 없으면 자동 추가
        String fullKey = privateKeyContent;
        if (!privateKeyContent.startsWith("-----BEGIN")) {
            fullKey = "-----BEGIN PRIVATE KEY-----\n" + privateKeyContent + "\n-----END PRIVATE KEY-----";
        }
        
        Path tempFile = java.nio.file.Files.createTempFile("cloudfront-key-", ".pem");
        java.nio.file.Files.write(tempFile, fullKey.getBytes());
        return tempFile;
    }
}