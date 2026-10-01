package com.byeolnight.dto.crawler;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NewsCollectionResultDto {

    public enum Status {
        COLLECTED,
        ALREADY_COLLECTED_TODAY,
        SOURCE_API_FAILED,
        NO_ARTICLES_SAVED,
        FAILED
    }

    private Status status;
    private String message;
    private int candidateCount;
    private int savedCount;
    private int duplicateCount;
    private int filteredCount;
    private int aiFailureCount;
    private String errorType;
    private LocalDateTime executedAt;

    public boolean isSuccessful() {
        return status == Status.COLLECTED || status == Status.ALREADY_COLLECTED_TODAY;
    }
}
