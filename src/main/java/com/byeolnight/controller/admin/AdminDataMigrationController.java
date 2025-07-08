package com.byeolnight.controller.admin;

import com.byeolnight.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 데이터 마이그레이션 관련 관리자 전용 컨트롤러
 * 주의: 운영환경에서는 신중하게 사용해야 합니다.
 */
@RestController
@RequestMapping("/api/admin/migration")
@RequiredArgsConstructor
@Tag(name = "👮 관리자 API - 데이터 마이그레이션", description = "데이터 마이그레이션 및 유지보수 API")
public class AdminDataMigrationController {

    private final UserService userService;

    @Operation(summary = "전화번호 암호화 마이그레이션", description = "기존 전화번호를 암호화합니다. (주의: 한 번만 실행)")
    @PostMapping("/encrypt-phones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> migratePhoneEncryption() {
        try {
            userService.migratePhoneEncryption();
            return ResponseEntity.ok("전화번호 암호화 마이그레이션이 완료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("마이그레이션 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}