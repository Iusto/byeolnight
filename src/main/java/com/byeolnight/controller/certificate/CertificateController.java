package com.byeolnight.controller.certificate;

import com.byeolnight.entity.certificate.Certificate;
import com.byeolnight.entity.user.User;
import com.byeolnight.dto.certificate.AllCertificateResponseDto;
import com.byeolnight.dto.certificate.CertificateResponseDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.certificate.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member/certificates")
@RequiredArgsConstructor
@Tag(name = "🏆 인증서 API", description = "사용자 인증서 관리 API")
public class CertificateController {

    private final CertificateService certificateService;

    @Operation(summary = "내 인증서 목록 조회", description = """
    사용자가 보유한 모든 인증서를 조회합니다.
    
    🏆 인증서 종류:
    - STAR_EXPLORER: 별빛탐험가 (게시글 10개 작성)
    - ASTRONAUT_REGISTRATION: 우주인등록 (회원가입)
    - GALAXY_COMMUNICATOR: 은하통신병 (채팅 100회)
    - COSMIC_SOCIALIZER: 우주사교가 (좋아요 50개)
    - STELLAR_ATTENDEE: 스텔라출석왕 (연속 30일 출석)
    - POINT_COLLECTOR: 포인트콜렉터 (1000포인트 누적)
    - DISCUSSION_MASTER: 토론마스터 (토론 참여 20회)
    - COMMUNITY_LEADER: 커뮤니티리더 (종합 활동 점수)
    """)
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<CommonResponse<List<CertificateResponseDto>>> getMyCertificates(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        
        List<CertificateResponseDto> certificates = certificateService.getUserCertificates(user)
                .stream()
                .map(CertificateResponseDto::from)
                .toList();
        
        return ResponseEntity.ok(CommonResponse.success(certificates));
    }

    @Operation(summary = "대표 인증서 설정", description = "사용자의 대표 인증서를 설정합니다. (닉네임 옆에 표시됨)")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대표 인증서 설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 인증서 타입 또는 보유하지 않은 인증서"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/representative/{certificateType}")
    public ResponseEntity<CommonResponse<String>> setRepresentativeCertificate(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "인증서 타입", example = "STAR_EXPLORER") @PathVariable String certificateType) {
        
        try {
            Certificate.CertificateType type = Certificate.CertificateType.valueOf(certificateType);
            certificateService.setRepresentativeCertificate(user, type);
            return ResponseEntity.ok(CommonResponse.success("대표 인증서가 설정되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("잘못된 인증서 타입입니다."));
        }
    }

    @Operation(summary = "대표 인증서 조회", description = "사용자의 현재 대표 인증서를 조회합니다. (설정되지 않았으면 null 반환)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/representative")
    public ResponseEntity<CommonResponse<CertificateResponseDto>> getRepresentativeCertificate(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        
        var representative = certificateService.getRepresentativeCertificate(user);
        if (representative != null) {
            return ResponseEntity.ok(CommonResponse.success(CertificateResponseDto.from(representative)));
        }
        
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "전체 인증서 목록 조회", description = """
    보유/미보유 인증서를 모두 조회합니다.
    
    📊 각 인증서에 대해 다음 정보 제공:
    - 보유 여부 (earned)
    - 획득 조건 (requirements)
    - 현재 진행률 (progress)
    - 획득 일시 (earnedAt)
    """)
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all")
    public ResponseEntity<CommonResponse<List<AllCertificateResponseDto>>> getAllCertificates(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        
        List<AllCertificateResponseDto> certificates = certificateService.getAllCertificatesWithStatus(user)
                .stream()
                .map(AllCertificateResponseDto::from)
                .toList();
        
        return ResponseEntity.ok(CommonResponse.success(certificates));
    }
}