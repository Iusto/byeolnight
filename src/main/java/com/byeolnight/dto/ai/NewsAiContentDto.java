package com.byeolnight.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class NewsAiContentDto {

    private String koreanTitle;
    private String overview;
    private List<String> keyFacts;
    private String whyItMatters;
    private List<String> watchPoints;
    private List<String> tags;
}
