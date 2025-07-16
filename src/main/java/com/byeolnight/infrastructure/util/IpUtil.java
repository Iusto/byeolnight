package com.byeolnight.infrastructure.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 클라이언트의 실제 IP 주소를 추출하는 유틸리티 클래스
 * 프록시, 로드밸런서, CDN 등을 고려하여 실제 클라이언트 IP를 반환
 */
@Slf4j
public class IpUtil {

    private static final String[] IP_HEADERS = {
        "X-Client-IP",           // 프론트엔드에서 전송하는 실제 클라이언트 IP (최우선)
        "X-Forwarded-For",
        "X-Real-IP", 
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    };

    /**
     * HttpServletRequest에서 실제 클라이언트 IP 주소를 추출
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = null;
        
        // 각 헤더를 순서대로 확인
        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                // X-Forwarded-For의 경우 여러 IP가 콤마로 구분될 수 있음
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                log.debug("클라이언트 IP 추출 성공 - 헤더: {}, IP: {}", header, ip);
                return ip;
            }
        }
        
        // 헤더에서 찾지 못한 경우 기본 방법 사용
        ip = request.getRemoteAddr();
        log.debug("기본 방법으로 IP 추출 - IP: {}", ip);
        
        return ip != null ? ip : "unknown";
    }

    /**
     * IP 주소가 유효한지 검증
     */
    private static boolean isValidIp(String ip) {
        return ip != null && 
               !ip.isEmpty() && 
               !"unknown".equalsIgnoreCase(ip) &&
               !"0:0:0:0:0:0:0:1".equals(ip) &&
               !"127.0.0.1".equals(ip) &&
               !"localhost".equalsIgnoreCase(ip);
    }
}