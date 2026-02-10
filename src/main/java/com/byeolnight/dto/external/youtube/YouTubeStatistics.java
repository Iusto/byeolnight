package com.byeolnight.dto.external.youtube;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class YouTubeStatistics {
    private String viewCount;
    private String likeCount;
    private String commentCount;

    public long getViewCountAsLong() {
        if (viewCount == null) return 0;
        try {
            return Long.parseLong(viewCount);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public long getLikeCountAsLong() {
        if (likeCount == null) return 0;
        try {
            return Long.parseLong(likeCount);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
