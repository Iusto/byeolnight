package com.byeolnight.service.comment;

import com.byeolnight.dto.comment.CommentRequestDto;
import com.byeolnight.entity.comment.Comment;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.comment.CommentLikeRepository;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.repository.post.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("댓글 생성 성공")
    void create_Success() {
        User user = User.builder().email("test@test.com").nickname("tester").build();
        Post post = Post.builder().title("test").build();
        CommentRequestDto dto = new CommentRequestDto(1L, "test comment", null);
        Comment savedComment = Comment.builder().id(1L).content("test comment").build();
        
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(commentRepository.save(any(Comment.class))).willReturn(savedComment);

        Long result = commentService.create(dto, user);

        assertThat(result).isEqualTo(1L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 생성 실패 - 게시글 없음")
    void create_PostNotFound() {
        User user = User.builder().email("test@test.com").build();
        CommentRequestDto dto = new CommentRequestDto(999L, "test", null);
        
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(dto, user))
                .hasMessage("게시글이 존재하지 않습니다.");
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void update_Success() {
        User user = User.builder().email("test@test.com").build();
        Comment comment = Comment.builder().writer(user).content("old").build();
        CommentRequestDto dto = new CommentRequestDto(1L, "new content", null);
        
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        commentService.update(1L, dto, user);

        verify(commentRepository).findById(1L);
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void delete_Success() {
        User user = User.builder().email("test@test.com").build();
        Comment comment = Comment.builder().writer(user).content("test").build();
        
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        commentService.delete(1L, user);

        verify(commentRepository).findById(1L);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 권한 없음")
    void delete_Unauthorized() {
        User owner = User.builder().email("owner@test.com").build();
        User other = User.builder().email("other@test.com").build();
        Comment comment = Comment.builder().writer(owner).content("test").build();
        
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(1L, other))
                .hasMessage("삭제 권한이 없습니다.");
    }
}
