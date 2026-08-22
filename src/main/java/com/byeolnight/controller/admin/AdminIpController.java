package com.byeolnight.controller.admin;

import com.byeolnight.dto.admin.IpBlockRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 관리자 IP 차단 정책만 제공하는 API입니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "👮 관리자 API - IP 차단")
public class AdminIpController {

    private final StringRedisTemplate redisTemplate;

    @Operation(summary = "차단된 IP 목록 조회", description = "로그인 실패 등으로 인해 차단된 IP 주소 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/blocked-ips")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<List<String>>> getBlockedIps() {
        Set<String> keys = redisTemplate.keys("blocked:ip:*");
        if (keys == null || keys.isEmpty()) {
            return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(Collections.emptyList()));
        }
        List<String> ipList = keys.stream()
                .map(key -> key.replace("blocked:ip:", ""))
                .collect(Collectors.toList());
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(ipList));
    }

    @Operation(
            summary = "차단된 IP 해제",
            description = "관리자가 로그인 실패 누적으로 차단된 특정 IP 주소의 차단을 해제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "차단 해제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/blocked-ips")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> unblockIp(@RequestParam String ip) {
        redisTemplate.delete("blocked:ip:" + ip);
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success("IP 차단이 해제되었습니다."));
    }

    @Operation(summary = "특정 IP 수동 차단", description = "관리자가 특정 IP를 수동으로 차단합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "차단 성공"),
            @ApiResponse(responseCode = "400", description = "IP 형식 오류")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/blocked-ips")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> blockIpManually(@RequestBody IpBlockRequestDto request) {
        String ip = request.getIp();
        long duration = request.getDurationMinutes();

        if (!ip.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            return ResponseEntity.badRequest()
                    .body(com.byeolnight.infrastructure.common.CommonResponse.fail("잘못된 IP 형식입니다."));
        }

        redisTemplate.opsForValue().set("blocked:ip:" + ip, "true", duration, TimeUnit.MINUTES);
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success("IP가 차단되었습니다."));
    }

}
