package com.byeolnight.dto.admin;

import com.byeolnight.dto.crawler.NewsCollectionResultDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NewsStatusDto {
    private long todayNews;
    private boolean systemHealthy;
    private String statusMessage;
    private String warning;
    private NewsCollectionResultDto lastExecution;
}
