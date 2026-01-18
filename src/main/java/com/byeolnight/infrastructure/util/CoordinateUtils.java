package com.byeolnight.infrastructure.util;

/**
 * 좌표 반올림 유틸리티
 * - 0.01 단위로 반올림하여 약 1km 그리드 생성
 * - 같은 그리드 내 사용자들이 캐시 공유
 */
public class CoordinateUtils {

    private static final double GRID_SIZE = 0.01;  // 약 1km 그리드

    /**
     * 좌표를 0.01 단위로 반올림
     * 약 1km 범위의 사용자들이 같은 캐시 키 공유
     */
    public static double roundCoordinate(double coordinate) {
        return Math.round(coordinate / GRID_SIZE) * GRID_SIZE;
    }

    /**
     * 캐시 키 생성
     * 예: wx:37.57:126.98
     */
    public static String generateCacheKey(double lat, double lon) {
        double roundedLat = roundCoordinate(lat);
        double roundedLon = roundCoordinate(lon);
        return String.format("wx:%.2f:%.2f", roundedLat, roundedLon);
    }

    /**
     * 좌표 유효성 검증
     * 위도: -90 ~ 90
     * 경도: -180 ~ 180
     */
    public static boolean isValidCoordinate(double lat, double lon) {
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }
}