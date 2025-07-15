package com.byeolnight.controller.suggestion;

import com.byeolnight.domain.entity.Suggestion;
import com.byeolnight.dto.ApiResponse;
import com.byeolnight.dto.suggestion.SuggestionDto;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.service.suggestion.SuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
@Tag(name = "건의게시판", description = "건의게시판 관련 API")
public class SuggestionController {

    private final SuggestionService suggestionService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    @Operation(summary = "건의사항 목록 조회", description = "카테고리와 상태별로 건의사항 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<SuggestionDto.ListResponse>> getSuggestions(
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
        try {
            userId = jwtTokenProvider.getUserIdFromRequest(httpRequest);
        } catch (Exception e) {
            // 비로그인 사용자도 공개 건의사항은 볼 수 있음
        }
        
        SuggestionDto.ListResponse response = suggestionService.getSuggestions(category, status, pageable, userId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "건의사항 상세 조회", description = "특정 건의사항의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<SuggestionDto.Response>> getSuggestion(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        Long userId = null;
        try {
            userId = jwtTokenProvider.getUserIdFromRequest(httpRequest);
        } catch (Exception e) {
            // 비로그인 사용자도 공개 건의사항은 볼 수 있음
        }
        
        SuggestionDto.Response response = suggestionService.getSuggestion(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "건의사항 작성", description = "새로운 건의사항을 작성합니다.")
    public ResponseEntity<ApiResponse<SuggestionDto.Response>> createSuggestion(
            @Valid @RequestBody SuggestionDto.CreateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = jwtTokenProvider.getUserIdFromRequest(httpRequest);
        SuggestionDto.Response response = suggestionService.createSuggestion(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "건의사항 수정", description = "기존 건의사항을 수정합니다.")
    public ResponseEntity<ApiResponse<SuggestionDto.Response>> updateSuggestion(
            @PathVariable Long id,
            @Valid @RequestBody SuggestionDto.UpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = jwtTokenProvider.getUserIdFromRequest(httpRequest);
        SuggestionDto.Response response = suggestionService.updateSuggestion(id, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "건의사항 삭제", description = "건의사항을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteSuggestion(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        Long userId = jwtTokenProvider.getUserIdFromRequest(httpRequest);
        suggestionService.deleteSuggestion(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/my")
    @Operation(summary = "내 건의사항 조회", description = "내가 작성한 건의사항 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<SuggestionDto.ListResponse>> getMySuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest
    ) {
        Long userId = jwtTokenProvider.getUserIdFromRequest(httpRequest);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        SuggestionDto.ListResponse response = suggestionService.getMySuggestions(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/admin-response")
    @Operation(summary = "관리자 답변 추가", description = "건의사항에 관리자 답변을 추가합니다.")
    public ResponseEntity<ApiResponse<SuggestionDto.Response>> addAdminResponse(
            @PathVariable Long id,
            @RequestBody SuggestionDto.AdminResponseRequest request,
            HttpServletRequest httpRequest
    ) {
        Long adminId = jwtTokenProvider.getUserIdFromRequest(httpRequest);
        SuggestionDto.Response response = suggestionService.addAdminResponse(id, adminId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "건의사항 상태 변경", description = "관리자가 건의사항 상태를 변경합니다.")
    public ResponseEntity<ApiResponse<SuggestionDto.Response>> updateStatus(
            @PathVariable Long id,
            @RequestBody SuggestionDto.StatusUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long adminId = jwtTokenProvider.getUserIdFromRequest(httpRequest);
        SuggestionDto.Response response = suggestionService.updateStatus(id, adminId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}