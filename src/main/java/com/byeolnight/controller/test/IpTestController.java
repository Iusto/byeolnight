package com.byeolnight.controller.test;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.util.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
@Tag(name = "🧪 테스트 API", description = "IP 추출 테스트용 API")
public class IpTestController {

    @Operation(summary = "IP 추출 테스트", description = "클라이언트의 실제 IP 주소를 추출하여 반환합니다.")
    @GetMapping("/ip")
    public ResponseEntity<CommonResponse<Map<String, String>>> testIpExtraction(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        
        // IpUtil을 사용한 IP 추출
        String extractedIp = IpUtil.getClientIp(request);
        result.put("extractedIp", extractedIp);
        
        // 기본 방법으로 IP 추출
        String remoteAddr = request.getRemoteAddr();
        result.put("remoteAddr", remoteAddr);
        
        // 각종 헤더 정보
        result.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
        result.put("X-Real-IP", request.getHeader("X-Real-IP"));
        result.put("Proxy-Client-IP", request.getHeader("Proxy-Client-IP"));
        result.put("WL-Proxy-Client-IP", request.getHeader("WL-Proxy-Client-IP"));
        result.put("HTTP_X_FORWARDED_FOR", request.getHeader("HTTP_X_FORWARDED_FOR"));
        result.put("HTTP_CLIENT_IP", request.getHeader("HTTP_CLIENT_IP"));
        
        return ResponseEntity.ok(CommonResponse.success(result));
    }
}