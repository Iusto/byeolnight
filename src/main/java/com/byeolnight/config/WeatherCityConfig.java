package com.byeolnight.config;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 별관측 날씨 수집 대상 도시 설정
 */
@Component
@Getter
public class WeatherCityConfig {

    /**
     * 날씨 데이터를 수집할 주요 도시 목록
     * - 30분마다 자동 수집하여 캐싱
     */
    private final List<City> cities = List.of(
            // 수도권
            new City("서울", 37.5665, 126.9780),
            new City("인천", 37.4563, 126.7052),
            new City("수원", 37.2636, 127.0286),

            // 강원
            new City("춘천", 37.8813, 127.7300),
            new City("강릉", 37.7519, 128.8761),

            // 충청
            new City("대전", 36.3504, 127.3845),
            new City("청주", 36.6424, 127.4890),
            new City("천안", 36.8151, 127.1139),

            // 전라
            new City("전주", 35.8242, 127.1479),
            new City("광주", 35.1595, 126.8526),
            new City("목포", 34.8118, 126.3922),

            // 경상
            new City("대구", 35.8714, 128.6014),
            new City("부산", 35.1796, 129.0756),
            new City("울산", 35.5384, 129.3114),
            new City("포항", 36.0190, 129.3435),
            new City("경주", 35.8562, 129.2247),

            // 제주
            new City("제주", 33.4996, 126.5312),
            new City("서귀포", 33.2541, 126.5601)
    );

    /**
     * 도시 정보
     */
    public record City(String name, double latitude, double longitude) {
        public String getCacheKey() {
            // 좌표를 소수점 1자리로 반올림하여 캐시 키 생성
            double roundedLat = Math.round(latitude * 10.0) / 10.0;
            double roundedLon = Math.round(longitude * 10.0) / 10.0;
            return String.format("weather:%.1f:%.1f", roundedLat, roundedLon);
        }
    }
}
