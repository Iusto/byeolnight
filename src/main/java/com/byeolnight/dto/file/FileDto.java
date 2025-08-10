package com.byeolnight.dto.file;


public record FileDto(
        String originalName,
        String s3Key,
        String url
) {}
