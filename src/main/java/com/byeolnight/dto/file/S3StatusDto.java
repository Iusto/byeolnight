package com.byeolnight.dto.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class S3StatusDto {

    private final String bucketName;
    private final String configuredRegion;
    private final ConnectionStatus connectionStatus;
    private final boolean bucketExists;
    private final boolean regionMatch;
    private final String actualRegion;
    private final String error;
    private final String suggestion;
    private final String warning;
    private final String info;

    public enum ConnectionStatus {
        SUCCESS,
        ERROR
    }
}
