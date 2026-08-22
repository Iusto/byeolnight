package com.byeolnight.dto.cinema;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CinemaAiContentDto {
    private String koreanTitle;
    private String introduction;
    private String whySelected;
    private List<String> keyPoints;
    private String recommendedFor;
    private List<String> tags;
}
