package com.byeolnight.dto.external.youtube;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class YouTubeVideoDetailItem {
    private String id;
    private YouTubeSnippet snippet;
    private YouTubeStatistics statistics;
    private YouTubeContentDetails contentDetails;
    private YouTubeVideoStatus status;
}
