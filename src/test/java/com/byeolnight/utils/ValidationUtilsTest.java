package com.byeolnight.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Utils 테스트")
class UtilsTest {

    @Test
    @DisplayName("기본 테스트")
    void 기본_테스트() {
        assertThat(true).isTrue();
    }
}