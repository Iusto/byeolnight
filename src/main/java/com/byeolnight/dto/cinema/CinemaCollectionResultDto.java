package com.byeolnight.dto.cinema;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CinemaCollectionResultDto {
    public enum Status {
        CREATED, ALREADY_CREATED_TODAY, NO_VALID_CANDIDATE, YOUTUBE_API_FAILED,
        YOUTUBE_API_KEY_MISSING, OPENAI_API_KEY_MISSING, OPENAI_AUTH_FAILED,
        OPENAI_QUOTA_OR_RATE_LIMIT, OPENAI_API_FAILED, AI_GENERATION_FAILED,
        RECENT_TOPIC_DUPLICATE, SAVE_FAILED
    }

    private Status status;
    private String message;
    private int searchedCandidates;
    private int validCandidates;
    private String selectedVideoId;
    private String selectedTitle;
    private Integer selectedScore;
    private LocalDateTime executedAt;

    public boolean isCreated() {
        return status == Status.CREATED;
    }
}
