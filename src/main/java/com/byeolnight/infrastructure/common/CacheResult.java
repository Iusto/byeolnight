package com.byeolnight.infrastructure.common;

public record CacheResult<T>(T data, boolean cacheHit) {}
