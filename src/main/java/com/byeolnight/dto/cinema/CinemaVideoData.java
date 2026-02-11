package com.byeolnight.dto.cinema;

import java.time.LocalDateTime;

public record CinemaVideoData(
    String title,
    String description,
    String videoId,
    String videoUrl,
    String channelTitle,
    LocalDateTime publishedAt,
    String summary,
    String hashtags,
    String content
) {}
