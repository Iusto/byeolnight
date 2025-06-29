package com.byeolnight.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsDto {
    
    private String title;       // 뉴스 제목
    private String content;     // 뉴스 내용
    private String source;      // 뉴스 출처
    private String url;         // 원본 뉴스 URL
    private String publishedAt; // 발행 시간
    private String category;    // 뉴스 카테고리 (예: 천문학, 우주과학 등)
    private String[] tags;      // 태그 배열
}