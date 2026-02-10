package com.byeolnight.dto.external.youtube;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class YouTubeThumbnails {
    @JsonProperty("default")
    private YouTubeThumbnail defaultThumbnail;

    private YouTubeThumbnail medium;
    private YouTubeThumbnail high;

    public String getBestUrl() {
        if (high != null && high.getUrl() != null) return high.getUrl();
        if (medium != null && medium.getUrl() != null) return medium.getUrl();
        if (defaultThumbnail != null && defaultThumbnail.getUrl() != null) return defaultThumbnail.getUrl();
        return null;
    }
}
