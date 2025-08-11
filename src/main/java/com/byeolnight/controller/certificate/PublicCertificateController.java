package com.byeolnight.controller.certificate;

import com.byeolnight.dto.certificate.CertificateResponseDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/certificates")
@RequiredArgsConstructor
@Tag(name = "🌍 공개 API - 인증서", description = "비회원도 접근 가능한 인증서 조회 API")
public class PublicCertificateController {

    private final CertificateService certificateService;
    private final UserService userService;

    @Operation(summary = "사용자 대표 인증서 조회", description = """
    닉네임으로 사용자의 대표 인증서를 조회합니다.
    
    🏆 대표 인증서는 닉네임 옆에 표시되는 인증서입니다.
    👥 비회원도 접근 가능한 공개 API입니다.
    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (인증서 없으면 null 반환)"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/representative/{nickname}")
    public ResponseEntity<CommonResponse<CertificateResponseDto>> getRepresentativeCertificateByNickname(
            @Parameter(description = "사용자 닉네임", example = "우주탐험가") @PathVariable String nickname) {
        
        var user = userService.findByNickname(nickname);
        if (user.isEmpty()) {
            return ResponseEntity.ok(CommonResponse.error("사용자를 찾을 수 없습니다."));
        }
        
        var representative = certificateService.getRepresentativeCertificate(user.get());
        if (representative != null) {
            return ResponseEntity.ok(CommonResponse.success(CertificateResponseDto.from(representative)));
        }
        
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}