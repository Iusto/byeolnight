package com.byeolnight.controller.post;

import com.byeolnight.dto.file.FileDto;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.service.post.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member/posts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MemberPostController {

    private final PostService postService;

    @Operation(summary = "게시글 생성", description = """
        [회원만 가능]
        - 제목, 내용, 카테고리 필수 입력
        - 카테고리는 enum 값 중 선택: NEWS, DISCUSSION, IMAGE, EVENT, REVIEW
        - 이미지 업로드 가능 (선택, presigned URL 업로드 후 메타정보 전달)
        """)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<Long>> create(
            @RequestPart("requestDto") @Valid PostRequestDto requestDto,
            @RequestPart(value = "files", required = false) List<FileDto> files,
            @AuthenticationPrincipal User user) {
        Long postId = postService.createPost(requestDto, user, files);
        return ResponseEntity.ok(CommonResponse.success(postId));
    }

    @Operation(summary = "게시글 수정", description = """
        [회원만 가능]
        - 본인이 작성한 게시글만 수정 가능
        - 이미지 수정 포함 시 새로 업로드된 이미지 메타정보로 덮어쓰기
        """)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<Void>> update(
            @PathVariable Long id,
            @RequestPart("requestDto") @Valid PostRequestDto requestDto,
            @RequestPart(value = "files", required = false) List<FileDto> files,
            @AuthenticationPrincipal User user) {
        postService.updatePost(id, requestDto, user, files);
        return ResponseEntity.ok(CommonResponse.success());
    }

    @Operation(summary = "게시글 삭제", description = """
        [회원만 가능]
        - 본인이 작성한 게시글만 삭제 가능
        - Soft delete 방식으로 처리됨
        """)
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable Long id,
                                                       @AuthenticationPrincipal User user) {
        postService.deletePost(id, user);
        return ResponseEntity.ok(CommonResponse.success());
    }

    @Operation(summary = "게시글 추천", description = """
        [회원만 가능]
        - 중복 추천 불가
        - 추천 수는 인기 게시글 선정 기준에 활용됨
        """)
    @PostMapping("/{postId}/like")
    public ResponseEntity<CommonResponse<String>> likePost(@PathVariable Long postId,
                                                           @AuthenticationPrincipal User user) {
        postService.likePost(user.getId(), postId);
        return ResponseEntity.ok(CommonResponse.success("추천 완료"));
    }
}
