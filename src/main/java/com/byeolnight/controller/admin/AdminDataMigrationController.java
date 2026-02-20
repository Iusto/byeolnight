package com.byeolnight.controller.admin;

import com.byeolnight.service.shop.StellaShopService;
import com.byeolnight.repository.post.PostRepository;
import org.springframework.transaction.annotation.Transactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 데이터 마이그레이션 관련 관리자 전용 컨트롤러
 * 주의: 운영환경에서는 신중하게 사용해야 합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/migration")
@RequiredArgsConstructor
@Tag(name = "👮 관리자 API - 데이터 마이그레이션", description = "데이터 마이그레이션 및 유지보수 API")
public class AdminDataMigrationController {

    private final StellaShopService stellaShopService;
    private final PostRepository postRepository;

    @Operation(summary = "스텔라 아이콘 초기화", description = "전체 44개 스텔라 아이콘을 초기화합니다.")
    @PostMapping("/init-stella-icons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> initializeStellaIcons() {
        try {
            stellaShopService.initializeDefaultIcons();
            return ResponseEntity.ok("스텔라 아이콘 초기화가 완료되었습니다. (44개 아이콘)");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("아이콘 초기화 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(summary = "고아 게시글 정리", description = "작성자가 삭제된 게시글을 소프트 삭제 처리합니다.")
    @PostMapping("/cleanup-orphan-posts")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<String> cleanupOrphanPosts() {
        try {
            long count = postRepository.countPostsWithDeletedWriter();
            if (count == 0) {
                return ResponseEntity.ok("정리할 고아 게시글이 없습니다.");
            }
            
            postRepository.softDeletePostsWithDeletedWriter();
            return ResponseEntity.ok(String.format("고아 게시글 %d개를 소프트 삭제 처리했습니다.", count));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("고아 게시글 정리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}