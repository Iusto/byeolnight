package com.byeolnight.controller.admin;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.file.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 관리자 파일 관리 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
public class AdminFileController {

    private final S3Service s3Service;

    /**
     * 고아 이미지 개수 조회
     */
    @GetMapping("/orphan-count")
    @PreAuthorize("hasRole('ADMIN')")
    public CommonResponse<Integer> getOrphanImageCount() {
        log.info("관리자 고아 이미지 개수 조회 요청");
        
        int orphanCount = s3Service.getOrphanImageCount();
        
        if (orphanCount == -1) {
            return CommonResponse.error("S3 ListBucket 권한이 부족합니다. IAM 정책을 확인해주세요.");
        }
        
        return CommonResponse.success(orphanCount, 
            orphanCount + "개의 오래된 파일이 있습니다.");
    }

    /**
     * 고아 이미지 정리
     */
    @PostMapping("/cleanup-orphans")
    @PreAuthorize("hasRole('ADMIN')")
    public CommonResponse<Integer> cleanupOrphanImages() {
        log.info("관리자 고아 이미지 정리 요청");
        
        int deletedCount = s3Service.cleanupOrphanImages();
        
        return CommonResponse.success(deletedCount, 
            deletedCount + "개의 고아 이미지를 정리했습니다.");
    }
    
    /**
     * S3 연결 상태 확인
     */
    @GetMapping("/s3-status")
    @PreAuthorize("hasRole('ADMIN')")
    public CommonResponse<Map<String, Object>> getS3Status() {
        log.info("관리자 S3 상태 확인 요청");
        
        Map<String, Object> status = s3Service.getS3Status();
        
        return CommonResponse.success(status, "S3 상태 정보를 조회했습니다.");
    }
}