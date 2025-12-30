package com.byeolnight.infrastructure.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 좌표 반올림 유틸리티 테스트
 * - 0.2 단위로 반올림하여 약 20km 그리드 생성
 */
class CoordinateUtilsTest {

    @Test
    @DisplayName("좌표 반올림 - 37.4979는 37.4로 반올림")
    void roundCoordinate_37_4979() {
        // Given
        double coordinate = 37.4979;

        // When
        double result = CoordinateUtils.roundCoordinate(coordinate);

        // Then
        assertThat(result).isEqualTo(37.4);
    }

    @Test
    @DisplayName("좌표 반올림 - 37.4은 37.4로 반올림 (같은 그리드)")
    void roundCoordinate_37_4() {
        // Given
        double coordinate = 37.4;

        // When
        double result = CoordinateUtils.roundCoordinate(coordinate);

        // Then
        assertThat(result).isEqualTo(37.4);
    }

    @Test
    @DisplayName("좌표 반올림 - 37.6001은 37.6으로 반올림 (다른 그리드)")
    void roundCoordinate_37_6001() {
        // Given
        double coordinate = 37.6001;

        // When
        double result = CoordinateUtils.roundCoordinate(coordinate);

        // Then
        assertThat(result).isEqualTo(37.6);
    }

    @Test
    @DisplayName("캐시 키 생성 - 강남 좌표 → wx:37.4:127.0")
    void generateCacheKey_Gangnam() {
        // Given
        double lat = 37.4979;
        double lon = 127.0276;

        // When
        String cacheKey = CoordinateUtils.generateCacheKey(lat, lon);

        // Then
        assertThat(cacheKey).isEqualTo("wx:37.4:127.0");
    }

    @Test
    @DisplayName("좌표 유효성 검증 - 정상 좌표")
    void isValidCoordinate_Valid() {
        // Given
        double lat = 37.5665;
        double lon = 126.9780;

        // When
        boolean result = CoordinateUtils.isValidCoordinate(lat, lon);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("좌표 유효성 검증 - 위도 범위 초과 (100)")
    void isValidCoordinate_InvalidLat() {
        // Given
        double lat = 100.0;
        double lon = 126.9780;

        // When
        boolean result = CoordinateUtils.isValidCoordinate(lat, lon);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("좌표 유효성 검증 - 경도 범위 초과 (200)")
    void isValidCoordinate_InvalidLon() {
        // Given
        double lat = 37.5665;
        double lon = 200.0;

        // When
        boolean result = CoordinateUtils.isValidCoordinate(lat, lon);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("같은 그리드 내 좌표들이 같은 캐시 키 생성")
    void sameGridSameCacheKey() {
        // Given - 0.2 그리드 내의 두 좌표 (둘 다 37.4, 127.0으로 반올림)
        double coord1Lat = 37.4979;  // → 37.4
        double coord1Lon = 127.0276; // → 127.0
        double coord2Lat = 37.48;    // → 37.4
        double coord2Lon = 127.05;   // → 127.0

        // When
        String key1 = CoordinateUtils.generateCacheKey(coord1Lat, coord1Lon);
        String key2 = CoordinateUtils.generateCacheKey(coord2Lat, coord2Lon);

        // Then - 둘 다 wx:37.4:127.0
        assertThat(key1).isEqualTo(key2);
        assertThat(key1).isEqualTo("wx:37.4:127.0");
    }
}