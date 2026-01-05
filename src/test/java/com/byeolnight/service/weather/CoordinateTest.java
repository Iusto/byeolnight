package com.byeolnight.service.weather;

import com.byeolnight.infrastructure.util.CoordinateUtils;

public class CoordinateTest {
    public static void main(String[] args) {
        double lat = 33.4996;
        double lon = 126.5312;
        
        double roundedLat = CoordinateUtils.roundCoordinate(lat);
        double roundedLon = CoordinateUtils.roundCoordinate(lon);
        
        System.out.println("Original: " + lat + ", " + lon);
        System.out.println("Rounded: " + roundedLat + ", " + roundedLon);
        System.out.println("Cache Key: " + CoordinateUtils.generateCacheKey(lat, lon));
    }
}
