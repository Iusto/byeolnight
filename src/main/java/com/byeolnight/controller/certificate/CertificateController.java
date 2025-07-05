package com.byeolnight.controller.certificate;

import com.byeolnight.domain.entity.certificate.Certificate;
import com.byeolnight.domain.entity.user.User;
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

    @Operation(summary = "내 인증서 목록 조회", description = "사용자가 보유한 모든 인증서를 조회합니다.")
    @GetMapping
    public ResponseEntity<CommonResponse<List<CertificateResponseDto>>> getMyCertificates(
            @AuthenticationPrincipal User user) {
        
        List<CertificateResponseDto> certificates = certificateService.getUserCertificates(user)
                .stream()
                .map(CertificateResponseDto::from)
                .toList();
        
        return ResponseEntity.ok(CommonResponse.success(certificates));
    }

    @Operation(summary = "대표 인증서 설정", description = "사용자의 대표 인증서를 설정합니다.")
    @PutMapping("/representative/{certificateType}")
    public ResponseEntity<CommonResponse<String>> setRepresentativeCertificate(
            @AuthenticationPrincipal User user,
            @PathVariable String certificateType) {
        
        try {
            Certificate.CertificateType type = Certificate.CertificateType.valueOf(certificateType);
            certificateService.setRepresentativeCertificate(user, type);
            return ResponseEntity.ok(CommonResponse.success("대표 인증서가 설정되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("잘못된 인증서 타입입니다."));
        }
    }

    @Operation(summary = "대표 인증서 조회", description = "사용자의 현재 대표 인증서를 조회합니다.")
    @GetMapping("/representative")
    public ResponseEntity<CommonResponse<CertificateResponseDto>> getRepresentativeCertificate(
            @AuthenticationPrincipal User user) {
        
        var representative = certificateService.getRepresentativeCertificate(user);
        if (representative != null) {
            return ResponseEntity.ok(CommonResponse.success(CertificateResponseDto.from(representative)));
        }
        
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "전체 인증서 목록 조회", description = "보유/미보유 인증서를 모두 조회합니다. (획득 방법 포함)")
    @GetMapping("/all")
    public ResponseEntity<CommonResponse<List<AllCertificateResponseDto>>> getAllCertificates(
            @AuthenticationPrincipal User user) {
        
        List<AllCertificateResponseDto> certificates = certificateService.getAllCertificatesWithStatus(user)
                .stream()
                .map(AllCertificateResponseDto::from)
                .toList();
        
        return ResponseEntity.ok(CommonResponse.success(certificates));
    }
}