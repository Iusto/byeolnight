package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsAiContentDto;
import com.byeolnight.dto.ai.NewsApiResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NewsContentFormatterTest {

    private final NewsContentFormatter formatter = new NewsContentFormatter();

    @Test
    @DisplayName("뉴스 본문을 중복 없는 고정 마크다운 섹션으로 구성한다")
    void 고정_마크다운_섹션_생성() {
        NewsApiResponseDto.Result source = new NewsApiResponseDto.Result();
        source.setLink("https://example.com/space-news");
        source.setSourceName("NASA");
        source.setPubDate("2026-08-22 10:00:00");

        NewsAiContentDto content = new NewsAiContentDto();
        content.setKoreanTitle("NASA, 새로운 외계행성 발견");
        content.setOverview("NASA가 새로운 외계행성을 확인했다.");
        content.setKeyFacts(List.of("우주망원경으로 관측했다.", "후속 관측이 필요하다."));
        content.setWhyItMatters("외계행성 연구 범위를 넓힐 수 있다.");
        content.setWatchPoints(List.of("후속 분광 관측 결과"));
        content.setTags(List.of("NASA", "외계 행성"));

        String result = formatter.formatNewsContent(source, content);

        assertThat(result).contains("## 한눈에 보기", "## 핵심 사실", "## 왜 중요한가", "## 앞으로 볼 점");
        assertThat(result).contains("- 우주망원경으로 관측했다.", "[원문 기사 읽기](https://example.com/space-news)");
        assertThat(result).contains("#NASA #외계행성");
        assertThat(result).doesNotContain("AI 분석", "상세 내용", "원문 크롤링 제한");
    }
}
