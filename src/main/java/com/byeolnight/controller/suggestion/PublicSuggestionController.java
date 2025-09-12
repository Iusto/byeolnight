package com.byeolnight.controller.suggestion;

import com.byeolnight.entity.Suggestion;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.dto.suggestion.SuggestionDto;
import com.byeolnight.service.suggestion.SuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/suggestions")
@RequiredArgsConstructor
@Tag(name = "🌍 공개 API - 건의게시판", description = "비회원도 접근 가능한 건의사항 조회 API")
public class PublicSuggestionController {

    private final SuggestionService suggestionService;

    @GetMapping
    @Operation(summary = "건의사항 목록 조회", description = """
    [비회원 접근 가능]
    카테고리와 상태별로 공개 건의사항 목록을 조회합니다.
    
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
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        // 공개 건의사항만 조회
        SuggestionDto.ListResponse response = suggestionService.getSuggestions(category, status, pageable);
        
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "건의사항 상세 조회", description = """
    [비회원 접근 가능]
    공개 건의사항의 상세 정보를 조회합니다.
    비공개 건의사항은 조회할 수 없습니다.
    """)
    @Parameter(name = "id", description = "건의사항 ID", example = "1")
    public ResponseEntity<CommonResponse<SuggestionDto.Response>> getSuggestion(
            @PathVariable @Positive Long id
    ) {
        SuggestionDto.Response response = suggestionService.getPublicSuggestion(id);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}