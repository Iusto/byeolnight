package com.byeolnight.dto.external.youtube;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class YouTubeVideoListResponse {
    private List<YouTubeVideoItem> items;
}
