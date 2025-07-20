package com.byeolnight.controller.file;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.service.file.FileUploadRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import com.byeolnight.service.file.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "📁 파일 API", description = "AWS S3 파일 업로드 및 관리 API")
public class FileController {
    
    // 클라이언트 ID와 SSE Emitter를 저장하는 맵
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final S3Service s3Service;
    private final FileUploadRateLimitService rateLimitService;
    private final HttpServletRequest request;

    @Operation(summary = "S3 Presigned URL 생성", description = "파일 업로드를 위한 S3 Presigned URL을 생성합니다.")
    @PostMapping("/presigned-url")
    public ResponseEntity<CommonResponse<Map<String, String>>> getPresignedUrl(
            @RequestParam("filename") String filename,
            @RequestParam(value = "contentType", required = false) String contentType,
            HttpServletRequest request) {
        
        if (filename == null || filename.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(CommonResponse.error("파일명이 필요합니다."));
        }
        
        // 파일 확장자 검사
        String extension = "";
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = filename.substring(lastDotIndex + 1).toLowerCase();
        } else {
            return ResponseEntity.badRequest().body(CommonResponse.error("파일 확장자가 필요합니다."));
        }
        
        // 이미지 파일 확장자 검사
        if (!extension.matches("jpg|jpeg|png|gif|bmp|webp|svg")) {
            return ResponseEntity.badRequest().body(CommonResponse.error("지원되지 않는 이미지 형식입니다. 지원 형식: jpg, jpeg, png, gif, bmp, webp, svg"));
        }
        
        // Rate Limiting 검사
        String clientIp = IpUtil.getClientIp(request);
        
        // Presigned URL 생성 제한 확인
        if (!rateLimitService.isPresignedUrlAllowed(clientIp)) {
            return ResponseEntity.status(429).body(CommonResponse.error("Presigned URL 생성 한도를 초과했습니다. 잠시 후 다시 시도해주세요."));
        }
        
        try {
            Map<String, String> result = s3Service.generatePresignedUrl(filename, contentType);
            return ResponseEntity.ok(CommonResponse.success(result));
        } catch (Exception e) {
            log.error("Presigned URL 생성 오류", e);
            return ResponseEntity.badRequest().body(CommonResponse.error(e.getMessage()));
        }
    }

    /**
     * 이미지 검증 결과를 실시간으로 받기 위한 SSE 연결 엔드포인트
     */
    @Operation(summary = "이미지 검증 결과 실시간 수신", description = "Server-Sent Events를 통해 이미지 검증 결과를 실시간으로 받습니다.")
    @GetMapping(value = "/validation-events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToValidationEvents(@RequestParam("clientId") String clientId) {
        // 기존 연결이 있으면 제거
        if (emitters.containsKey(clientId)) {
            SseEmitter oldEmitter = emitters.remove(clientId);
            oldEmitter.complete();
        }
        
        // 새 SSE Emitter 생성 (30분 타임아웃)
        SseEmitter emitter = new SseEmitter(1800000L);
        
        // 연결 완료/오류/타임아웃 시 연결 제거
        emitter.onCompletion(() -> {
            log.debug("SSE 연결 완료: {}", clientId);
            emitters.remove(clientId);
        });
        
        emitter.onTimeout(() -> {
            log.debug("SSE 연결 타임아웃: {}", clientId);
            emitter.complete();
            emitters.remove(clientId);
        });
        
        emitter.onError(e -> {
            log.error("SSE 연결 오류: {}", clientId, e);
            emitter.complete();
            emitters.remove(clientId);
        });
        
        // 연결 성공 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data(Map.of(
                        "status", "connected",
                        "clientId", clientId,
                        "message", "이미지 검증 결과 수신 준비 완료"
                    )));
        } catch (IOException e) {
            log.error("SSE 초기 이벤트 전송 실패", e);
            return new SseEmitter();
        }
        
        // 연결 저장
        emitters.put(clientId, emitter);
        log.info("SSE 연결 생성 완료: {} (현재 연결 수: {})", clientId, emitters.size());
        
        return emitter;
    }
    
    /**
     * 클라이언트에게 검증 결과 이벤트 전송
     */
    private void sendValidationResult(String clientId, Map<String, Object> result) {
        if (emitters.containsKey(clientId)) {
            try {
                SseEmitter emitter = emitters.get(clientId);
                emitter.send(SseEmitter.event()
                        .name("validationResult")
                        .data(result));
                log.debug("검증 결과 전송 성공: {} -> {}", clientId, result);
            } catch (IOException e) {
                log.error("검증 결과 전송 실패: {}", clientId, e);
                // 오류 발생 시 연결 제거
                SseEmitter emitter = emitters.remove(clientId);
                emitter.completeWithError(e);
            }
        } else {
            log.warn("클라이언트 연결 없음: {}", clientId);
        }
    }
    
    @Operation(summary = "업로드된 이미지 검증", description = "이미 업로드된 이미지를 Google Vision API로 검증합니다. 부적절한 이미지는 자동으로 삭제됩니다.")
    @PostMapping("/validate-image")
    public ResponseEntity<CommonResponse<Map<String, Object>>> validateImage(
            @RequestParam("imageUrl") String imageUrl,
            @RequestParam(value = "clientId", required = false) String clientId) {
        
        try {
            // 비동기로 검증 처리하고 결과를 콜백으로 받음
            Consumer<Map<String, Object>> validationCallback = null;
            
            // 클라이언트 ID가 있는 경우 콜백 설정
            if (clientId != null && !clientId.isEmpty()) {
                validationCallback = result -> {
                    // 검증 결과를 SSE로 클라이언트에게 전송
                    sendValidationResult(clientId, result);
                };
            }
            
            // 스레드 풀을 사용하여 비동기로 검증 처리 (백그라운드에서 실행)
            s3Service.validateImageByUrl(imageUrl, validationCallback);
            
            // 항상 성공 응답 반환 (검증은 백그라운드에서 진행)
            Map<String, Object> result = Map.of(
                "isValid", true,
                "message", "이미지 검증이 백그라운드에서 진행 중입니다.",
                "url", imageUrl
            );
            
            return ResponseEntity.ok(CommonResponse.success(result));
        } catch (Exception e) {
            log.error("이미지 검증 오류", e);
            // 오류 발생해도 성공 응답 반환 (클라이언트 UI 블로킹 방지)
            Map<String, Object> result = Map.of(
                "isValid", true,
                "message", "이미지 검증 중 오류가 발생했지만 프론트엔드 처리를 위해 성공으로 처리합니다.",
                "url", imageUrl
            );
            return ResponseEntity.ok(CommonResponse.success(result));
        }
    }
    
    /**
     * 이미지 검증 상태 조회 API
     */
    @Operation(summary = "이미지 검증 상태 조회", description = "스레드 풀 상태를 포함한 이미지 검증 시스템 상태를 조회합니다.")
    @GetMapping("/validation-status")
    public ResponseEntity<CommonResponse<Map<String, Object>>> getValidationStatus() {
        try {
            // 스레드 풀 상태 정보 가져오기
            ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) 
                org.springframework.web.context.support.WebApplicationContextUtils
                .getRequiredWebApplicationContext(request.getServletContext())
                .getBean("imageValidationExecutor");
            
            Map<String, Object> status = new HashMap<>();
            status.put("activeConnections", emitters.size());
            status.put("poolSize", executor.getPoolSize());
            status.put("activeThreads", executor.getActiveCount());
            status.put("queuedTasks", executor.getThreadPoolExecutor().getQueue().size());
            status.put("completedTasks", executor.getThreadPoolExecutor().getCompletedTaskCount());
            
            return ResponseEntity.ok(CommonResponse.success(status));
        } catch (Exception e) {
            log.error("검증 상태 조회 오류", e);
            return ResponseEntity.internalServerError().body(CommonResponse.error("검증 상태 조회 중 오류가 발생했습니다."));
        }
    }
}