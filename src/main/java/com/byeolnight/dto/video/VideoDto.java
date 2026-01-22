package com.byeolnight.dto.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * YouTube 영상 정보 DTO
 * - VideoController의 공개 API 응답용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDto {

    private String videoId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String publishedAt;
    private String channelTitle;
}
