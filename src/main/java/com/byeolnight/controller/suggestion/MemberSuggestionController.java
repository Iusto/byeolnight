package com.byeolnight.controller.suggestion;

import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.dto.suggestion.SuggestionDto;
import com.byeolnight.service.suggestion.SuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/suggestions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "📝 건의게시판 API - 회원", description = "회원 전용 건의사항 작성, 수정, 삭제 API")
public class MemberSuggestionController {

    private final SuggestionService suggestionService;

    @GetMapping("/{id}")
    @Operation(summary = "건의사항 상세 조회", description = "특정 건의사항의 상세 정보를 조회합니다. (로그인 필수, 비공개 건의는 작성자와 관리자만 조회 가능)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (비공개 건의)"),
            @ApiResponse(responseCode = "404", description = "건의사항 없음")
    })
    public ResponseEntity<CommonResponse<SuggestionDto.Response>> getSuggestion(
            @Parameter(description = "건의사항 ID", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        SuggestionDto.Response response = suggestionService.getSuggestion(id, user.getId());
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "건의사항 작성", description = "새로운 건의사항을 작성합니다. (로그인 필수)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "작성 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<CommonResponse<SuggestionDto.Response>> createSuggestion(
            @Valid @RequestBody SuggestionDto.CreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        SuggestionDto.Response response = suggestionService.createSuggestion(user.getId(), request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "건의사항 수정", description = "기존 건의사항을 수정합니다.")
    public ResponseEntity<CommonResponse<SuggestionDto.Response>> updateSuggestion(
            @PathVariable Long id,
            @Valid @RequestBody SuggestionDto.UpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        SuggestionDto.Response response = suggestionService.updateSuggestion(id, user.getId(), request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "건의사항 삭제", description = "건의사항을 삭제합니다.")
    public ResponseEntity<CommonResponse<Void>> deleteSuggestion(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        suggestionService.deleteSuggestion(id, user.getId());
        return ResponseEntity.ok(CommonResponse.success());
    }

    @GetMapping("/my")
    @Operation(summary = "내 건의사항 조회", description = "내가 작성한 건의사항 목록을 조회합니다. (로그인 필수)")
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
            @Parameter(name = "size", description = "페이지 크기", example = "10")
    })
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<CommonResponse<SuggestionDto.ListResponse>> getMySuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        SuggestionDto.ListResponse response = suggestionService.getMySuggestions(user.getId(), pageable);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PostMapping("/{id}/admin-response")
    @Operation(summary = "관리자 답변 등록", description = "건의사항에 관리자 답변을 등록하고 상태를 변경합니다. (관리자 권한 필수)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "답변 등록 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @ApiResponse(responseCode = "404", description = "건의사항 없음")
    })
    public ResponseEntity<CommonResponse<SuggestionDto.Response>> addAdminResponse(
            @Parameter(description = "건의사항 ID", example = "1") @PathVariable Long id,
            @RequestBody @Valid SuggestionDto.AdminResponseRequest request,
            @AuthenticationPrincipal User user
    ) {
        SuggestionDto.Response response = suggestionService.addAdminResponse(id, user.getId(), request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}