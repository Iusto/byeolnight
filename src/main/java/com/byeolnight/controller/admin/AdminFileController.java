package com.byeolnight.controller.admin;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.file.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import com.byeolnight.dto.file.S3StatusDto;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
@Tag(name = "📁 관리자 - 파일 관리", description = "관리자 파일 및 S3 관리 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFileController {

    private final S3Service s3Service;

    @GetMapping("/orphan-count")
    @Operation(
        summary = "고아 이미지 개수 조회",
        description = "S3에 있지만 DB에서 참조되지 않는 고아 이미지의 개수를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "고아 이미지 개수 조회 성공"),
        @ApiResponse(responseCode = "500", description = "S3 권한 부족 또는 서버 오류")
    })
    public CommonResponse<Integer> getOrphanImageCount() {
        log.info("관리자 고아 이미지 개수 조회 요청");
        
        int orphanCount = s3Service.getOrphanImageCount();
        
        if (orphanCount == -1) {
            return CommonResponse.error("S3 ListBucket 권한이 부족합니다. IAM 정책을 확인해주세요.");
        }
        
        return CommonResponse.success(orphanCount, 
            orphanCount + "개의 오래된 파일이 있습니다.");
    }

    @PostMapping("/cleanup-orphans")
    @Operation(
        summary = "고아 이미지 정리",
        description = "S3에 있지만 DB에서 참조되지 않는 고아 이미지들을 삭제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "고아 이미지 정리 성공"),
        @ApiResponse(responseCode = "500", description = "S3 권한 부족 또는 서버 오류")
    })
    public CommonResponse<Integer> cleanupOrphanImages() {
        log.info("관리자 고아 이미지 정리 요청");
        
        int deletedCount = s3Service.cleanupOrphanImages();
        
        return CommonResponse.success(deletedCount, 
            deletedCount + "개의 고아 이미지를 정리했습니다.");
    }
    
    @GetMapping("/s3-status")
    @Operation(
        summary = "S3 연결 상태 확인",
        description = "S3 버킷 연결 상태와 권한을 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "S3 상태 조회 성공"),
        @ApiResponse(responseCode = "500", description = "S3 연결 오류")
    })
    public CommonResponse<S3StatusDto> getS3Status() {
        log.info("관리자 S3 상태 확인 요청");

        S3StatusDto status = s3Service.getS3Status();

        return CommonResponse.success(status, "S3 상태 정보를 조회했습니다.");
    }

    @PostMapping("/cleanup-legacy-orphans")
    @Operation(
        summary = "[레거시] S3 스캔 방식 고아 이미지 정리",
        description = """
        File 테이블 기반 정리로 전환하기 전, S3에만 있고 File 테이블에 없는 파일들을 정리합니다.

        ⚠️ 일회성 실행 후 /cleanup-orphans 로 전환하세요.

        동작 방식:
        - S3 전체 스캔 + Post/Comment content LIKE 검색
        - 7일 이상 지난 미사용 파일 삭제
        """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "레거시 고아 이미지 정리 성공"),
        @ApiResponse(responseCode = "500", description = "S3 권한 부족 또는 서버 오류")
    })
    public CommonResponse<Integer> cleanupLegacyOrphanImages() {
        log.info("관리자 레거시 고아 이미지 정리 요청");

        int deletedCount = s3Service.cleanupLegacyOrphanImages();

        return CommonResponse.success(deletedCount,
            deletedCount + "개의 레거시 고아 이미지를 정리했습니다.");
    }
}