package com.byeolnight.infrastructure.config;

/**
 * 스텔라 아이콘 상점 설정
 * 
 * 역할:
 * - 45개 우주 테마 아이콘 메타데이터 관리
 * - 등급별 가격 설정 (COMMON, RARE, EPIC, LEGENDARY)
 * - 아이콘 한글명, 설명 매핑 정보
 * - 아이콘 파일 경로 및 확장자 설정
 * - 등급별 아이콘 분류 및 가격 조회 유틸리티
 */

import com.byeolnight.domain.entity.shop.StellaIconGrade;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "stella.icons")
public class StellaIconConfig {
    
    private String folderPath;
    private String fileExtension;
    private List<String> excludeFiles;
    private Map<StellaIconGrade, GradeConfig> grades;
    private Map<String, String> names;
    private Map<String, String> descriptions;
    
    @Data
    public static class GradeConfig {
        private int price;
        private List<String> icons;
    }
    
    public StellaIconGrade getGradeByIconName(String iconName) {
        return grades.entrySet().stream()
                .filter(entry -> entry.getValue().getIcons().contains(iconName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(StellaIconGrade.COMMON);
    }
    
    public int getPriceByGrade(StellaIconGrade grade) {
        return grades.getOrDefault(grade, new GradeConfig()).getPrice();
    }
    
    public String getKoreanName(String iconName) {
        return names.getOrDefault(iconName, iconName);
    }
    
    public String getDescription(String iconName) {
        return descriptions.getOrDefault(iconName, iconName + " 아이콘");
    }
}