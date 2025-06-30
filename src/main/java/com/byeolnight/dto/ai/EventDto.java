package com.byeolnight.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    
    private String title;           // 전시회 제목
    private String content;         // 전시회 내용 설명
    private String exhibitionName;  // 전시회명
    private String location;        // 전시 장소 (박물관, 과학관 등)
    private String eventDate;       // 전시 기간
    private String registrationUrl; // 예약/티켓 구매 링크
    private String contact;         // 문의 전화번호
    private String fee;            // 관람료
    private String exhibitionType;  // 전시회 유형 (상설전시/기획전시/특별전시)
    private String organizer;       // 주최기관
    private String[] tags;         // 태그
    private String source;         // 출처
    private String url;            // 원문 URL
}