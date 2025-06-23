    package com.byeolnight.controller.post;

    import com.byeolnight.dto.post.PostResponseDto;
    import com.byeolnight.infrastructure.common.CommonResponse;
    import com.byeolnight.service.post.PostService;
    import io.swagger.v3.oas.annotations.Operation;
    import io.swagger.v3.oas.annotations.Parameter;
    import io.swagger.v3.oas.annotations.Parameters;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    @RestController
    @RequestMapping("/api/public/posts")
    @RequiredArgsConstructor
    public class PublicPostController {

        private final PostService postService;

        @Operation(summary = "게시글 목록 조회", description = """
    [비회원 접근 가능]

    📌 정렬 조건 (sort: string, query)
    - recent (기본값): 최신순 정렬 (createdAt DESC)
    - popular: 최근 30일 기준 추천순 정렬

    📌 카테고리 필터 (category: string, query)
    - NEWS, DISCUSSION, IMAGE, EVENT, REVIEW 중 선택

    📌 페이징 (page, size)
    - 예: page=0, size=10
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

                // ✅ Pageable의 sort는 제거하고, page/size만 사용
                @Parameter(hidden = true) Pageable pageableRaw
        ) {
            // ✅ 강제 정렬 제거 (sort=recent 같은 잘못된 필드 정렬 차단)
            Pageable pageable = PageRequest.of(pageableRaw.getPageNumber(), pageableRaw.getPageSize());

            Page<PostResponseDto> posts = postService.getFilteredPosts(category, sort, pageable);
            return ResponseEntity.ok(CommonResponse.success(posts));
        }

        @Operation(summary = "게시글 단건 조회", description = """
        [비회원 접근 가능]
        - 게시글 ID 기반 단건 조회
        - 블라인드된 게시글은 `blinded: true`로 표시됨
        """)
        @GetMapping("/{id:\\d+}")
        public ResponseEntity<CommonResponse<PostResponseDto>> getPostById(@PathVariable Long id) {
            PostResponseDto response = postService.getPostById(id, null);
            return ResponseEntity.ok(CommonResponse.success(response));
        }
    }
