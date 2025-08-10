package com.byeolnight.service.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PostService 통합 테스트")
class PostServiceIntegrationTest {

    @Test
    @DisplayName("기본 테스트")
    void 기본_테스트() {
        assertThat(true).isTrue();
    }
}