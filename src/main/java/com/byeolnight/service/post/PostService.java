package com.byeolnight.service.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.PostRepository;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.infrastructure.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    @Transactional
    public Long createPost(PostRequestDto dto, User user) {
        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .writer(user)
                .build();
        return postRepository.save(post).getId();
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPostById(Long id) {
        Post post = postRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        return PostResponseDto.from(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getAllPosts(Pageable pageable) {
        return postRepository.findAllByIsDeletedFalse(pageable)
                .map(PostResponseDto::from);
    }

    @Transactional
    public void updatePost(Long id, PostRequestDto dto, User user) {
        Post post = postRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        if (!post.getWriter().equals(user)) {
            throw new IllegalArgumentException("게시글 수정 권한이 없습니다.");
        }
        post.update(dto.getTitle(), dto.getContent(), dto.getCategory());
    }

    @Transactional
    public void softDeletePost(Long id, User user) {
        Post post = postRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        if (!post.getWriter().equals(user)) {
            throw new IllegalArgumentException("게시글 삭제 권한이 없습니다.");
        }
        post.softDelete();
    }
}