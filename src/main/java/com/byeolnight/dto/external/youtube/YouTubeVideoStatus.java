package com.byeolnight.dto.external.youtube;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class YouTubeVideoStatus {
    private String privacyStatus;
    private Boolean embeddable;
    private String uploadStatus;
}
