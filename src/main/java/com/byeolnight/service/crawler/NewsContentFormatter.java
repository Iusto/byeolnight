package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NewsContentFormatter {
    
    private final NewsTranslationService translationService;
    
    public String formatNewsContent(NewsApiResponseDto.Result result) {
        String imageSection = result.getImageUrl() != null && !result.getImageUrl().trim().isEmpty() 
            ? "![뉴스 이미지](" + result.getImageUrl() + ")\n\n" : "";
        
        String description = result.getDescription();
        if (description != null && !description.trim().isEmpty() && isEnglishTitle(result.getTitle())) {
            description = translationService.translateTitle(description);
        }
        
        String summarySection = description != null && !description.trim().isEmpty()
            ? "## 📰 뉴스 요약\n\n" + description + "\n\n"
            : "## 📰 뉴스 요약\n\n이 뉴스는 우주와 천문학 관련 최신 소식을 다룹니다. 자세한 내용은 원문 링크를 통해 확인하세요.\n\n";
        
        String sourceInfo = "";
        if (result.getSourceName() != null) sourceInfo += "**출처:** " + result.getSourceName() + "\n";
        if (result.getPubDate() != null) sourceInfo += "**발행일:** " + result.getPubDate() + "\n\n";
        
        String hashtags = generateHashtags(result.getTitle(), result.getDescription());
        String aiAnalysis = translationService.generateAIAnalysis(result.getTitle(), result.getDescription());
        
        String disclaimer = "\n\n---\n\n⚠️ **면책 조항**: 본 콘텐츠는 AI 기반 자동 번역 및 요약으로 생성되었습니다. 정확한 정보는 반드시 원문을 확인하시기 바랍니다.";
        
        return String.format("""
            %s%s## 🤖 AI 분석
            
            %s
            
            ## 📄 상세 내용
            
            ⚠️ **원문 크롤링 제한**: 저작권 및 기술적 제약으로 원문 내용을 직접 가져올 수 없습니다.
            
            💡 **대신 제공**: AI 기반 분석과 요약을 통해 핵심 내용을 파악하실 수 있습니다.
            
            ## 🔗 원문 보기
            
            [📰 원문 기사 보기](%s)
            
            ---
            
            %s%s%s
            """, 
            imageSection, summarySection, aiAnalysis, result.getLink(), sourceInfo, 
            hashtags.isEmpty() ? "" : hashtags, disclaimer
        );
    }
    
    public String generateHashtags(String title, String description) {
        List<String> tags = new ArrayList<>();
        String content = (title + " " + (description != null ? description : "")).toLowerCase();
        
        String[][] keywordMappings = {
            {"nasa", "#NASA"}, {"spacex", "#SpaceX"}, {"space", "#우주"}, 
            {"mars", "#화성"}, {"moon", "#달"}, {"blackhole", "#블랙홀"}, 
            {"galaxy", "#은하"}, {"우주", "#우주"}, {"화성", "#화성"}, 
            {"달", "#달"}, {"블랙홀", "#블랙홀"}, {"은하", "#은하"}
        };
        
        for (String[] mapping : keywordMappings) {
            if (content.contains(mapping[0]) && !tags.contains(mapping[1]) && tags.size() < 10) {
                tags.add(mapping[1]);
            }
        }
        
        return String.join(" ", tags);
    }
    
    private boolean isEnglishTitle(String title) {
        if (title == null) return false;
        
        int englishCount = 0;
        int koreanCount = 0;
        
        for (char c : title.toCharArray()) {
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                englishCount++;
            } else if (c >= '가' && c <= '힣') {
                koreanCount++;
            }
        }
        
        return englishCount > koreanCount;
    }
}