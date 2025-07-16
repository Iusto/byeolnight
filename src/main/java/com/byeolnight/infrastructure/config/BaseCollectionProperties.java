package com.byeolnight.infrastructure.config;

import lombok.Data;

/**
 * 컬렉션 시스템 공통 설정 기본 클래스
 * 
 * 역할:
 * - 뉴스/영화 수집 시스템의 공통 설정 구조 정의
 * - 중복 코드 제거 및 일관된 설정 관리
 * - 새로운 컬렉션 시스템 추가 시 확장 용이
 */
@Data
public abstract class BaseCollectionProperties {
    
    protected Collection collection = new Collection();
    protected Quality quality = new Quality();
    
    @Data
    public static class Collection {
        protected int maxPosts = 1;                    // 최대 저장 개수
        protected double similarityThreshold = 0.7;   // 유사도 임계값
        protected int similarityCheckDays = 7;        // 유사도 체크 기간 (일)
    }
    
    @Data
    public static class Quality {
        protected int minTitleLength = 10;            // 최소 제목 길이
        protected int minDescriptionLength = 50;      // 최소 설명 길이
    }
}