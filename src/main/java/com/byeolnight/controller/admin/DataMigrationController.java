package com.byeolnight.controller.admin;

import com.byeolnight.service.user.UserService;
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
public class DataMigrationController {

    private final UserService userService;

    /**
     * 전화번호 암호화 마이그레이션 실행
     * 주의: 이 API는 한 번만 실행해야 합니다.
     */
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