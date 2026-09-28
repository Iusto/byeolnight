package com.byeolnight.entity.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostModerationTest {

    @Test
    @DisplayName("자동 생성 게시물은 검수 대기 상태로 숨기고 검수 후 공개할 수 있다")
    void pendingReviewLifecycle() {
        Post post = Post.builder()
                .title("자동 생성 콘텐츠")
                .content("출처와 내용을 확인해야 하는 콘텐츠")
                .category(Post.Category.NEWS)
                .build();

        post.markPendingReview();

        assertThat(post.isBlinded()).isTrue();
        assertThat(post.getBlindType()).isEqualTo(Post.BlindType.PENDING_REVIEW);
        assertThat(post.getBlindedAt()).isNotNull();
        assertThat(post.getBlindedByAdminId()).isNull();

        post.unblind();

        assertThat(post.isBlinded()).isFalse();
        assertThat(post.getBlindType()).isNull();
        assertThat(post.getBlindedAt()).isNull();
    }
}
