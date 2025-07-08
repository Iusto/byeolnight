package com.byeolnight.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
@Slf4j
public class ImageModerationService {

    @Value("${google.api.key:}")
    private String googleApiKey;

    /**
     * 이미지 콘텐츠 검증
     * 현재는 기본적인 검증만 수행하며, 향후 AI 기반 검증으로 확장 가능
     */
    public boolean isImageAppropriate(MultipartFile file) {
        try {
            // 1. 파일 형식 검증
            if (!isValidImageFormat(file)) {
                return false;
            }
            
            // 2. 이미지 크기 검증 (너무 작은 이미지는 의심스러움)
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                return false;
            }
            
            // 최소 크기 검증 (50x50 픽셀 이상)
            if (image.getWidth() < 50 || image.getHeight() < 50) {
                System.out.println("이미지가 너무 작습니다: " + image.getWidth() + "x" + image.getHeight());
                return false;
            }
            
            // 3. 파일 크기 검증 (10MB 이하)
            if (file.getSize() > 10 * 1024 * 1024) {
                System.out.println("파일 크기가 너무 큽니다: " + file.getSize() + " bytes");
                return false;
            }
            
            // 4. 기본적인 색상 분석 (향후 확장 가능)
            if (hasInappropriateColorPattern(image)) {
                System.out.println("부적절한 색상 패턴이 감지되었습니다.");
                return false;
            }
            
            return true;
            
        } catch (IOException e) {
            System.err.println("이미지 검증 중 오류 발생: " + e.getMessage());
            return false;
        }
    }
    
    private boolean isValidImageFormat(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
            contentType.equals("image/jpeg") ||
            contentType.equals("image/png") ||
            contentType.equals("image/gif") ||
            contentType.equals("image/webp")
        );
    }
    
    /**
     * 기본적인 색상 패턴 분석
     * 향후 더 정교한 AI 기반 분석으로 대체 가능
     */
    private boolean hasInappropriateColorPattern(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // 샘플링을 통한 색상 분석 (성능 최적화)
        int sampleSize = Math.min(100, Math.min(width, height));
        int stepX = Math.max(1, width / sampleSize);
        int stepY = Math.max(1, height / sampleSize);
        
        int totalPixels = 0;
        int suspiciousPixels = 0;
        
        for (int x = 0; x < width; x += stepX) {
            for (int y = 0; y < height; y += stepY) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                
                // 과도한 붉은색 계열 검출 (매우 기본적인 휴리스틱)
                if (red > 200 && green < 100 && blue < 100) {
                    suspiciousPixels++;
                }
                
                totalPixels++;
            }
        }
        
        // 의심스러운 픽셀이 전체의 30% 이상인 경우
        double suspiciousRatio = (double) suspiciousPixels / totalPixels;
        return suspiciousRatio > 0.3;
    }
    
    /**
     * Google Vision API를 사용한 이미지 검증
     * Safe Search Detection 기능 사용
     */
    public boolean checkWithGoogleVisionAPI(MultipartFile file) {
        if (googleApiKey == null || googleApiKey.isEmpty()) {
            log.warn("Google API 키가 설정되지 않았습니다. 기본 검증만 수행합니다.");
            return true;
        }
        
        try {
            // TODO: Google Vision API Safe Search Detection 구현
            // 현재는 기본 검증만 수행
            log.info("Google Vision API 검증 예정 - 현재는 기본 검증 수행");
            return true;
        } catch (Exception e) {
            log.error("Google Vision API 호출 실패", e);
            return true; // 오류 시 통과로 처리
        }
    }
    
    /**
     * 향후 확장을 위한 메서드
     * 외부 AI 서비스 (Google Vision API, AWS Rekognition 등) 연동 가능
     */
    public boolean checkWithExternalService(MultipartFile file) {
        return checkWithGoogleVisionAPI(file);
    }
}