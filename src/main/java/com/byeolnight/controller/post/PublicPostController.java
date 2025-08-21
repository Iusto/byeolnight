package com.byeolnight.controller.post;

import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.post.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/public/posts")
@RequiredArgsConstructor
@Tag(name = "🌍 공개 API - 게시글", description = "비회원도 접근 가능한 게시글 조회 API")
public class PublicPostController {

    private final PostService postService;

    @Operation(summary = "게시글 목록 조회", description = """
    [비회원 접근 가능]
    
    📌 정렬 조건 (`sort`: string, query)
    - `recent` (기본값): 최신순 정렬 + 최근 30일 내 인기글 5개 우선 노출 + 최신글 25개
    - `popular`: 최근 30일 기준 추천수 5 이상 게시글을 최신순 정렬로 최대 30개 반환
    
    📌 카테고리 필터 (`category`: string, query)
    - 다음 중 선택: `NEWS`, `DISCUSSION`, `IMAGE`, `EVENT`, `REVIEW`, `FREE`, `NOTICE`
    
    📌 페이징 (`page`, `size`)
    - 예: `page=0`, `size=10`
    ⚠️ Spring Pageable의 sort 파라미터는 무시됩니다. 사용하지 마세요.
    """)
    @GetMapping
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
            @Parameter(name = "size", description = "페이지 크기", example = "10"),
            @Parameter(name = "category", description = "게시글 카테고리 (예: NEWS)", example = "NEWS"),
            @Parameter(name = "sort", description = "정렬 방식: recent | popular", example = "recent")
    })
    public ResponseEntity<CommonResponse<Page<PostResponseDto>>> getPosts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String search,
            @Parameter(hidden = true) Pageable pageableRaw
    ) {
        System.out.println("API 요청 파라미터 - category: " + category + ", sort: " + sort + ", searchType: " + searchType + ", search: " + search);
        
        Pageable pageable = PageRequest.of(pageableRaw.getPageNumber(), pageableRaw.getPageSize());
        Page<PostResponseDto> posts = postService.getFilteredPosts(category, sort, searchType, search, pageable);
        
        System.out.println("반환할 게시글 수: " + posts.getTotalElements());
        return ResponseEntity.ok(CommonResponse.success(posts));
    }

    @Operation(summary = "게시글 단건 조회", description = """
    [비회원 접근 가능]
    - 게시글 ID 기반 단건 조회
    - 삭제된 게시글은 접근 불가
    - 블라인드된 게시글은 관리자만 접근 가능
    """)
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<CommonResponse<PostResponseDto>> getPostById(
            @PathVariable Long id,
            @Parameter(hidden = true) @org.springframework.security.core.annotation.AuthenticationPrincipal 
            com.byeolnight.entity.user.User currentUser) {
        
        PostResponseDto response = postService.getPostById(id, currentUser);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @GetMapping("/hot")
    @Operation(summary = "전체 카테고리 인기 게시글 조회", description = "최근 30일 기준 추천수 5 이상 게시글을 추천순으로 최대 6개 반환합니다.")
    @Parameter(name = "size", description = "조회할 게시글 수", example = "6")
    public ResponseEntity<CommonResponse<List<PostResponseDto>>> getTopHotPosts(@RequestParam(defaultValue = "6") int size) {
        List<PostResponseDto> hotPosts = postService.getTopHotPostsAcrossAllCategories(size);
        
        // 날짜 디버그
        if (!hotPosts.isEmpty()) {
            PostResponseDto firstPost = hotPosts.get(0);
            System.out.println("\n=== 인기 게시글 날짜 디버그 ===");
            System.out.println("ID: " + firstPost.getId());
            System.out.println("Title: " + firstPost.getTitle());
            System.out.println("CreatedAt: " + firstPost.getCreatedAt());
            System.out.println("UpdatedAt: " + firstPost.getUpdatedAt());
            System.out.println("TimeZone: " + java.util.TimeZone.getDefault().getID());
            System.out.println("=== 디버그 종료 ===\n");
        }
        
        return ResponseEntity.ok(CommonResponse.success(hotPosts));
    }
}
