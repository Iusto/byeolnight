package com.byeolnight.dto.external.youtube;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class YouTubeVideoItem {
    private YouTubeVideoId id;
    private YouTubeSnippet snippet;

    @Setter
    private YouTubeStatistics statistics;
}
