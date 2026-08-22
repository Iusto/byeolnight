package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsAiContentDto;
import com.byeolnight.dto.ai.NewsApiResponseDto;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class NewsContentFormatter {

    public String formatNewsContent(NewsApiResponseDto.Result result, NewsAiContentDto aiContent) {
        String imageSection = result.getImageUrl() != null && !result.getImageUrl().trim().isEmpty() 
            ? "![뉴스 이미지](" + result.getImageUrl() + ")\n\n" : "";

        String facts = toBulletList(aiContent.getKeyFacts());
        String watchPoints = toBulletList(aiContent.getWatchPoints());
        String sourceName = result.getSourceName() != null ? result.getSourceName() : "출처 미상";
        String publishedAt = result.getPubDate() != null ? result.getPubDate() : "발행일 미상";
        String hashtags = formatHashtags(aiContent);

        return String.format("""
            %s## 한눈에 보기

            %s

            ## 핵심 사실

            %s

            ## 왜 중요한가

            %s

            ## 앞으로 볼 점

            %s

            ---

            - **출처:** %s
            - **발행일:** %s
            - [원문 기사 읽기](%s)

            %s

            > 이 콘텐츠는 AI가 원문 제공 정보를 바탕으로 요약했습니다. 정확한 내용은 원문을 확인해 주세요.
            """,
            imageSection, aiContent.getOverview(), facts, aiContent.getWhyItMatters(), watchPoints,
            sourceName, publishedAt, result.getLink(), hashtags
        );
    }

    public String formatHashtags(NewsAiContentDto aiContent) {
        return aiContent.getTags().stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(tag -> tag.startsWith("#") ? tag : "#" + tag.replace(" ", ""))
                .distinct()
                .limit(5)
                .collect(Collectors.joining(" "));
    }

    private String toBulletList(java.util.List<String> values) {
        return values.stream().map(value -> "- " + value).collect(Collectors.joining("\n"));
    }
}
