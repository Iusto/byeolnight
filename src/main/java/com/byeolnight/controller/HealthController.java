package com.byeolnight.controller;

import com.byeolnight.dto.health.CorsTestDto;
import com.byeolnight.dto.health.HealthStatusDto;
import com.byeolnight.dto.health.NetworkTestDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@Tag(name = "🏥 Health Check API", description = "서버 상태 확인 및 네트워크 연결 테스트 API")
public class HealthController {

    @Operation(summary = "서버 상태 확인", description = "서버가 정상적으로 작동하는지 확인합니다. 네트워크 연결 테스트에도 사용됩니다.")
    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<CommonResponse<HealthStatusDto>> health() {
        HealthStatusDto healthInfo = HealthStatusDto.builder()
            .status("UP")
            .timestamp(LocalDateTime.now())
            .service("byeolnight-api")
            .version("1.0.0")
            .build();
        
        return ResponseEntity.ok(CommonResponse.success(healthInfo, "서버가 정상적으로 작동 중입니다."));
    }

    @Operation(summary = "서버 상태 확인 (HEAD)", description = "HEAD 요청으로 서버 상태를 확인합니다. 네트워크 지연시간 측정에 사용됩니다.")
    @RequestMapping(value = "/health", method = RequestMethod.HEAD)
    public ResponseEntity<Void> healthHead() {
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "CORS 테스트", description = "CORS 설정이 올바르게 작동하는지 테스트합니다.")
    @GetMapping("/cors-test")
    @ResponseBody
    public ResponseEntity<CommonResponse<CorsTestDto>> corsTest() {
        CorsTestDto corsInfo = CorsTestDto.builder()
            .cors("enabled")
            .allowCredentials(true)
            .allowedOrigins("*")
            .allowedMethods("GET, POST, PUT, DELETE, OPTIONS")
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.ok(CommonResponse.success(corsInfo, "CORS 설정이 정상적으로 작동합니다."));
    }

    @Operation(summary = "네트워크 연결 테스트", description = "클라이언트와 서버 간의 네트워크 연결을 테스트합니다.")
    @PostMapping("/network-test")
    @ResponseBody
    public ResponseEntity<CommonResponse<NetworkTestDto>> networkTest(@RequestBody(required = false) Map<String, Object> testData) {
        NetworkTestDto networkInfo = NetworkTestDto.builder()
            .connection("successful")
            .timestamp(LocalDateTime.now())
            .receivedData(testData != null ? testData.size() : 0)
            .echo(testData)
            .build();
        
        return ResponseEntity.ok(CommonResponse.success(networkInfo, "네트워크 연결이 정상적으로 작동합니다."));
    }
}