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
     * - 총 70개 도시 (일 3,360 API 호출, 무료 플랜 충분)
     */
    private final List<City> cities = List.of(
            // 서울
            new City("서울", 37.5665, 126.9780),

            // 경기도 (26개)
            new City("인천", 37.4563, 126.7052),
            new City("수원", 37.2636, 127.0286),
            new City("성남", 37.4200, 127.1267),
            new City("고양", 37.6584, 126.8320),
            new City("용인", 37.2411, 127.1776),
            new City("부천", 37.5034, 126.7660),
            new City("안산", 37.3219, 126.8309),
            new City("안양", 37.3943, 126.9568),
            new City("평택", 36.9921, 127.1129),
            new City("화성", 37.1995, 126.8310),
            new City("광명", 37.4786, 126.8644),
            new City("시흥", 37.3800, 126.8031),
            new City("의정부", 37.7381, 127.0337),
            new City("파주", 37.7599, 126.7800),
            new City("김포", 37.6153, 126.7156),
            new City("광주시", 37.4294, 127.2551),
            new City("하남", 37.5393, 127.2148),
            new City("구리", 37.5943, 127.1295),
            new City("남양주", 37.6360, 127.2165),
            new City("오산", 37.1498, 127.0698),
            new City("군포", 37.3616, 126.9351),
            new City("의왕", 37.3449, 126.9683),
            new City("이천", 37.2720, 127.4350),
            new City("안성", 37.0078, 127.2797),
            new City("양평", 37.4917, 127.4878),
            new City("가평", 37.8315, 127.5095),

            // 강원도 (9개) - 별 관측 명소 포함
            new City("춘천", 37.8813, 127.7300),
            new City("강릉", 37.7519, 128.8761),
            new City("원주", 37.3422, 127.9202),
            new City("속초", 38.2070, 128.5918),
            new City("동해", 37.5246, 129.1143),
            new City("평창", 37.3706, 128.3900),
            new City("정선", 37.3807, 128.6608),
            new City("태백", 37.1640, 128.9856),
            new City("홍천", 37.6972, 127.8885),

            // 충청도 (9개)
            new City("대전", 36.3504, 127.3845),
            new City("청주", 36.6424, 127.4890),
            new City("천안", 36.8151, 127.1139),
            new City("세종", 36.4800, 127.2890),
            new City("충주", 36.9910, 127.9259),
            new City("아산", 36.7898, 127.0018),
            new City("제천", 37.1325, 128.1910),
            new City("공주", 36.4465, 127.1190),
            new City("보령", 36.3333, 126.6127),

            // 전라도 (7개)
            new City("전주", 35.8242, 127.1479),
            new City("광주", 35.1595, 126.8526),
            new City("목포", 34.8118, 126.3922),
            new City("순천", 34.9506, 127.4872),
            new City("여수", 34.7604, 127.6622),
            new City("익산", 35.9483, 126.9577),
            new City("군산", 35.9676, 126.7368),

            // 경상도 (16개)
            new City("대구", 35.8714, 128.6014),
            new City("부산", 35.1796, 129.0756),
            new City("울산", 35.5384, 129.3114),
            new City("포항", 36.0190, 129.3435),
            new City("경주", 35.8562, 129.2247),
            new City("창원", 35.2280, 128.6811),
            new City("김해", 35.2285, 128.8894),
            new City("구미", 36.1195, 128.3446),
            new City("거제", 34.8806, 128.6211),
            new City("양산", 35.3350, 129.0372),
            new City("진주", 35.1798, 128.1076),
            new City("안동", 36.5684, 128.7294),
            new City("통영", 34.8544, 128.4330),
            new City("밀양", 35.5037, 128.7465),
            new City("영천", 35.9733, 128.9385),
            new City("사천", 35.0037, 128.0642),

            // 제주도 (2개)
            new City("제주", 33.4996, 126.5312),
            new City("서귀포", 33.2541, 126.5601)
    );

    /**
     * 도시 정보
     */
    public record City(String name, double latitude, double longitude) {
        public String getCacheKey() {
            // 좌표를 소수점 2자리로 반올림하여 캐시 키 생성 (약 1km 그리드)
            double roundedLat = Math.round(latitude * 100.0) / 100.0;
            double roundedLon = Math.round(longitude * 100.0) / 100.0;
            return String.format("wx:%.2f:%.2f", roundedLat, roundedLon);
        }
    }
}
