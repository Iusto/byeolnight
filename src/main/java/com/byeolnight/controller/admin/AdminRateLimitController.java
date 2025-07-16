package com.byeolnight.controller.admin;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.security.SmsRateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/rate-limit")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "👮 관리자 Rate Limit API", description = "Rate Limiting 관리 API")
public class AdminRateLimitController {
    
    private final SmsRateLimitService smsRateLimitService;
    
    @PostMapping("/sms/clear-phone/{phone}")
    @Operation(summary = "전화번호 SMS 제한 해제", description = "특정 전화번호의 SMS 인증 제한을 해제합니다.")
    public ResponseEntity<CommonResponse<String>> clearPhoneLimit(@PathVariable String phone) {
        try {
            // 전화번호별 모든 제한 해제
            smsRateLimitService.clearPhoneLimit(phone);
            log.info("관리자가 전화번호 SMS 제한 해제 - Phone: {}", phone);
            return ResponseEntity.ok(CommonResponse.success("전화번호 SMS 제한이 해제되었습니다."));
        } catch (Exception e) {
            log.error("전화번호 SMS 제한 해제 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(CommonResponse.fail("제한 해제 중 오류가 발생했습니다."));
        }
    }
    
    @PostMapping("/sms/clear-ip/{ip}")
    @Operation(summary = "IP SMS 제한 해제", description = "특정 IP의 SMS 인증 제한을 해제합니다.")
    public ResponseEntity<CommonResponse<String>> clearIpLimit(@PathVariable String ip) {
        try {
            // IP별 모든 제한 해제
            smsRateLimitService.clearIpLimit(ip);
            log.info("관리자가 IP SMS 제한 해제 - IP: {}", ip);
            return ResponseEntity.ok(CommonResponse.success("IP SMS 제한이 해제되었습니다."));
        } catch (Exception e) {
            log.error("IP SMS 제한 해제 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(CommonResponse.fail("제한 해제 중 오류가 발생했습니다."));
        }
    }
}