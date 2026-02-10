package com.byeolnight.dto.external.youtube;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class YouTubeSnippet {
    private String title;
    private String description;
    private String channelTitle;
    private String publishedAt;
    private YouTubeThumbnails thumbnails;
}
