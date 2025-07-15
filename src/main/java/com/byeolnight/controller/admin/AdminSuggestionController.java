package com.byeolnight.controller.admin;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/suggestions")
@RequiredArgsConstructor
@Tag(name = "관리자 - 건의사항", description = "관리자 건의사항 관리 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSuggestionController {

    private final SuggestionService suggestionService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    @Operation(summary = "전체 건의사항 조회", description = "관리자가 모든 건의사항을 조회합니다.")
    public ResponseEntity<ApiResponse<SuggestionDto.ListResponse>> getAllSuggestions(
            @RequestParam(required = false) Suggestion.SuggestionCategory category,
            @RequestParam(required = false) Suggestion.SuggestionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        SuggestionDto.ListResponse response = suggestionService.getAllSuggestionsForAdmin(category, status, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/admin-response")
    @Operation(summary = "관리자 답변 등록", description = "건의사항에 관리자 답변을 등록합니다.")
    public ResponseEntity<ApiResponse<SuggestionDto.Response>> addAdminResponse(
            @PathVariable Long id,
            @Valid @RequestBody SuggestionDto.AdminResponseRequest request,
            HttpServletRequest httpRequest
    ) {
        Long adminId = jwtTokenProvider.getUserIdFromRequest(httpRequest);
        SuggestionDto.Response response = suggestionService.addAdminResponse(id, adminId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "건의사항 상태 변경", description = "건의사항의 상태를 변경합니다.")
    public ResponseEntity<ApiResponse<SuggestionDto.Response>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody SuggestionDto.StatusUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long adminId = jwtTokenProvider.getUserIdFromRequest(httpRequest);
        SuggestionDto.Response response = suggestionService.updateStatus(id, adminId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}