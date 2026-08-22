package com.byeolnight.service.post;

import com.byeolnight.dto.post.PostAdminDto;
import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.entity.comment.Comment;
import com.byeolnight.entity.log.DeleteLog;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.service.assembler.PostResponseAssembler;
import com.byeolnight.service.log.DeleteLogService;
import com.byeolnight.service.user.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 게시글 블라인드·복구·분류 이동 등 관리자 전용 정책을 담당한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostAdminService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PointService pointService;
    private final DeleteLogService deleteLogService;
    private final PostResponseAssembler postResponseAssembler;

    @Transactional(readOnly = true)
    public List<PostResponseDto> getBlindedPostsList() {
        List<Post> posts = postRepository.findByIsDeletedFalseAndBlindedTrueOrderByCreatedAtDesc();
        Map<Long, Long> counts = batchCommentCounts(posts.stream().map(Post::getId).toList());
        return postResponseAssembler.toDtoList(posts, Map.of(), counts, Set.of());
    }

    @Transactional
    public void blindPostByAdmin(Long postId, Long adminId) {
        Post post = getPost(postId);
        deleteLogService.logDeletion(postId, DeleteLog.TargetType.POST, DeleteLog.ActionType.BLIND,
                adminId, "관리자 직접 블라인드", post.getTitle() + ": " + post.getContent());
        post.blindByAdmin(adminId);

        User writer = post.getWriter();
        // 운영용 계정의 자동 콘텐츠에는 일반 사용자 제재를 적용하지 않는다.
        if (writer != null && writer.getRole() != User.Role.ADMIN) {
            pointService.applyPenalty(writer, "관리자 블라인드 처리", postId.toString());
        }
        log.info("관리자 게시글 블라인드 처리: postId={}", postId);
    }

    @Transactional
    public void unblindPost(Long postId) {
        getPost(postId).unblind();
    }

    @Transactional(readOnly = true)
    public List<PostAdminDto> getDeletedPosts() {
        List<Post> posts = postRepository.findByIsDeletedTrueOrderByCreatedAtDesc();
        Map<Long, Long> counts = batchCommentCounts(posts.stream().map(Post::getId).toList());
        List<PostAdminDto> result = new ArrayList<>();
        for (Post post : posts) {
            result.add(PostAdminDto.builder()
                    .id(post.getId()).title(post.getTitle()).content(post.getContent())
                    .writer(post.getWriter().getNickname()).category(post.getCategory().name())
                    .blinded(post.isBlinded()).deleted(post.isDeleted())
                    .viewCount(post.getViewCount()).likeCount(post.getLikeCount())
                    .commentCount(counts.getOrDefault(post.getId(), 0L).intValue())
                    .createdAt(post.getCreatedAt()).deletedAt(post.getDeletedAt()).build());
        }
        return result;
    }

    @Transactional
    public void restorePost(Long postId) {
        getPost(postId).restore();
    }

    @Transactional
    public void restoreComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다."));
        comment.restore();
    }

    @Transactional
    public void movePostsCategory(List<Long> postIds, String targetCategory) {
        Post.Category category;
        try {
            category = Post.Category.valueOf(targetCategory.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("올바르지 않은 카테고리입니다.");
        }
        postRepository.findAllById(postIds)
                .forEach(post -> post.update(post.getTitle(), post.getContent(), category));
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
    }

    private Map<Long, Long> batchCommentCounts(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        return commentRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
