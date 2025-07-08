package com.byeolnight.infrastructure.config;

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