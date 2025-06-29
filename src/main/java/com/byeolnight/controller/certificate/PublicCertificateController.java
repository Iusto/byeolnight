package com.byeolnight.controller.certificate;

import com.byeolnight.dto.certificate.CertificateResponseDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/certificates")
@RequiredArgsConstructor
public class PublicCertificateController {

    private final CertificateService certificateService;
    private final UserService userService;

    @Operation(summary = "사용자 대표 인증서 조회", description = "닉네임으로 사용자의 대표 인증서를 조회합니다.")
    @GetMapping("/representative/{nickname}")
    public ResponseEntity<CommonResponse<CertificateResponseDto>> getRepresentativeCertificateByNickname(
            @PathVariable String nickname) {
        
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