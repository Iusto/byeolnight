package com.byeolnight.infrastructure.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 별빛시네마 자동 큐레이션 설정. */
@Data
@EqualsAndHashCode(callSuper = true)
@Component
@ConfigurationProperties(prefix = "cinema")
public class CinemaCollectionProperties extends BaseCollectionProperties {
    private Collection collection = new Collection();
    private Quality quality = new Quality();
    private Youtube youtube = new Youtube();

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Collection extends BaseCollectionProperties.Collection {
        private int similarityCheckDays = 30;
        private int retryCount = 3;
        private int queriesPerRun = 2;
        private int maxAiAttempts = 5;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Quality extends BaseCollectionProperties.Quality {
        private int maxResults = 20;
        private String videoDuration = "medium";
        private String videoDefinition = "high";
        private int minDurationSeconds = 180;
        private int maxDurationSeconds = 7_200;
        private long minViewCount = 1_000;
    }

    @Data
    public static class Youtube {
        /** 신뢰 채널은 표시명이 아닌 변경되지 않는 채널 ID로만 판별한다. */
        private String[] trustedChannelIds = {};
        private int publishedAfterYears = 2;
    }
}
