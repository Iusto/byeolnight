package com.byeolnight.service.comment;

import com.byeolnight.domain.entity.comment.Comment;
import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.CommentRepository;
import com.byeolnight.domain.repository.PostRepository;
import com.byeolnight.dto.comment.CommentRequestDto;
import com.byeolnight.infrastructure.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentService commentService;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = mock(User.class);
        post = mock(Post.class);
    }

    @Test
    void createComment_withoutParent_success() {
        // given
        CommentRequestDto dto = new CommentRequestDto();
        ReflectionTestUtils.setField(dto, "postId", 1L);
        ReflectionTestUtils.setField(dto, "content", "댓글 내용");

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(invocation -> {
                    Comment c = invocation.getArgument(0);
                    ReflectionTestUtils.setField(c, "id", 10L);
                    return c;
                });

        // when
        Long result = commentService.create(dto, user);

        // then
        assertThat(result).isEqualTo(10L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_withParent_success() {
        // given
        CommentRequestDto dto = new CommentRequestDto();
        ReflectionTestUtils.setField(dto, "postId", 1L);
        ReflectionTestUtils.setField(dto, "content", "대댓글 내용");
        ReflectionTestUtils.setField(dto, "parentId", 99L);

        Comment parent = mock(Comment.class);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(99L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(invocation -> {
                    Comment c = invocation.getArgument(0);
                    ReflectionTestUtils.setField(c, "id", 20L);
                    return c;
                });

        // when
        Long result = commentService.create(dto, user);

        // then
        assertThat(result).isEqualTo(20L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_postNotFound_shouldThrowException() {
        // given
        CommentRequestDto dto = new CommentRequestDto();
        ReflectionTestUtils.setField(dto, "postId", 999L);
        ReflectionTestUtils.setField(dto, "content", "댓글 내용");

        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        // expect
        assertThrows(NotFoundException.class, () -> commentService.create(dto, user));
    }

    @Test
    void createComment_parentNotFound_shouldThrowException() {
        // given
        CommentRequestDto dto = new CommentRequestDto();
        ReflectionTestUtils.setField(dto, "postId", 1L);
        ReflectionTestUtils.setField(dto, "content", "대댓글 내용");
        ReflectionTestUtils.setField(dto, "parentId", 123L);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(123L)).thenReturn(Optional.empty());

        // expect
        assertThrows(NotFoundException.class, () -> commentService.create(dto, user));
    }
}
