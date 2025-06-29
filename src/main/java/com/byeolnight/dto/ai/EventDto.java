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
    
    private String title;           // 이벤트 제목
    private String content;         // 이벤트 내용
    private String programName;     // 프로그램명
    private String location;        // 장소 (정확한 주소)
    private String eventDate;       // 일정 (운영 시간 및 요일)
    private String registrationUrl; // 신청방법 (온라인 예약 링크)
    private String contact;         // 연락처 (예약 및 문의 전화번호)
    private String fee;            // 참가비 (요금 정보)
    private String observatoryType; // 천문대 유형 (한국천문연구원/공립과학관/지역천문대)
    private String[] tags;         // 태그
    private String source;         // 출처
    private String url;            // 원문 URL
}