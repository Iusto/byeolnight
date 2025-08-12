package com.byeolnight.controller.suggestion;

import com.byeolnight.entity.Suggestion;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.dto.suggestion.SuggestionDto;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.service.suggestion.SuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
@Tag(name = "📝 공개 API - 건의게시판", description = "사이트 개선 및 기능 요청 건의게시판 API")
public class SuggestionController {

    private final SuggestionService suggestionService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    @Operation(summary = "건의사항 목록 조회", description = """
    카테고리와 상태별로 건의사항 목록을 조회합니다.
    
    📊 카테고리:
    - BUG_REPORT: 버그 신고
    - FEATURE_REQUEST: 기능 요청
    - IMPROVEMENT: 개선 제안
    - OTHER: 기타
    
    📊 상태:
    - PENDING: 대기 중
    - IN_PROGRESS: 처리 중
    - COMPLETED: 완료
    - REJECTED: 거부
    """)
    @Parameters({
            @Parameter(name = "category", description = "건의 카테고리", example = "FEATURE_REQUEST"),
            @Parameter(name = "status", description = "처리 상태", example = "PENDING"),
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
            @Parameter(name = "size", description = "페이지 크기", example = "10"),
            @Parameter(name = "sort", description = "정렬 기준 (createdAt, title, status)", example = "createdAt"),
            @Parameter(name = "direction", description = "정렬 방향 (asc, desc)", example = "desc")
    })
    public ResponseEntity<CommonResponse<SuggestionDto.ListResponse>> getSuggestions(
            @RequestParam(required = false) Suggestion.SuggestionCategory category,
            @RequestParam(required = false) Suggestion.SuggestionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest httpRequest
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Long userId = null;
        boolean isAdmin = false;
        try {
            userId = jwtTokenProvider.getUserIdFromRequest(httpRequest);
            // 관리자 여부 확인
            isAdmin = suggestionService.isAdmin(userId);
        } catch (Exception e) {
            // 비로그인 사용자도 공개 건의사항은 볼 수 있음
        }
        
        // 모든 사용자가 모든 건의사항을 볼 수 있도록 수정 (비공개는 제목만 마스킹)
        SuggestionDto.ListResponse response = suggestionService.getAllSuggestionsForAdmin(category, status, pageable);
        
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "건의사항 상세 조회", description = "특정 건의사항의 상세 정보를 조회합니다. (비공개 건의는 작성자와 관리자만 조회 가능)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (비공개 건의)"),
            @ApiResponse(responseCode = "404", description = "건의사항 없음")
    })
    public ResponseEntity<CommonResponse<SuggestionDto.Response>> getSuggestion(
            @Parameter(description = "건의사항 ID", example = "1") @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        Long userId = null;
        try {
            userId = jwtTokenProvider.getUserIdFromRequest(httpRequest);
        } catch (Exception e) {
            // 비로그인 사용자도 공개 건의사항은 볼 수 있음
        }
        
        SuggestionDto.Response response = userId != null ? 
            suggestionService.getSuggestion(id, userId) : 
            suggestionService.getSuggestion(id);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "건의사항 작성", description = "새로운 건의사항을 작성합니다. (로그인 필수)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "작성 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<CommonResponse<SuggestionDto.Response>> createSuggestion(
            @Valid @RequestBody SuggestionDto.CreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body(CommonResponse.error("로그인이 필요합니다."));
        }
        SuggestionDto.Response response = suggestionService.createSuggestion(user.getId(), request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "건의사항 수정", description = "기존 건의사항을 수정합니다.")
    public ResponseEntity<CommonResponse<SuggestionDto.Response>> updateSuggestion(
            @PathVariable Long id,
            @Valid @RequestBody SuggestionDto.UpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body(CommonResponse.error("로그인이 필요합니다."));
        }
        SuggestionDto.Response response = suggestionService.updateSuggestion(id, user.getId(), request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "건의사항 삭제", description = "건의사항을 삭제합니다.")
    public ResponseEntity<CommonResponse<Void>> deleteSuggestion(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body(CommonResponse.error("로그인이 필요합니다."));
        }
        suggestionService.deleteSuggestion(id, user.getId());
        return ResponseEntity.ok(CommonResponse.success());
    }

    @GetMapping("/my")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "내 건의사항 조회", description = "내가 작성한 건의사항 목록을 조회합니다. (로그인 필수)")
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
            @Parameter(name = "size", description = "페이지 크기", example = "10")
    })
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<CommonResponse<SuggestionDto.ListResponse>> getMySuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body(CommonResponse.error("로그인이 필요합니다."));
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        SuggestionDto.ListResponse response = suggestionService.getMySuggestions(user.getId(), pageable);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PostMapping("/{id}/admin-response")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "관리자 답변 등록", description = "건의사항에 관리자 답변을 등록하고 상태를 변경합니다. (관리자 권한 필수)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "답변 등록 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @ApiResponse(responseCode = "404", description = "건의사항 없음")
    })
    public ResponseEntity<CommonResponse<SuggestionDto.Response>> addAdminResponse(
            @Parameter(description = "건의사항 ID", example = "1") @PathVariable Long id,
            @RequestBody @Valid SuggestionDto.AdminResponseRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body(CommonResponse.error("로그인이 필요합니다."));
        }
        SuggestionDto.Response response = suggestionService.addAdminResponse(id, user.getId(), request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

}