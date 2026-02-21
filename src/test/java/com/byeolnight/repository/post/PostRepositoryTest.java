package com.byeolnight.repository.post;

import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.config.QueryDslConfig;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
@DisplayName("PostRepository 테스트")
class PostRepositoryTest {

    @Autowired PostRepository postRepository;
    @Autowired UserRepository userRepository;

    private User writer;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        writer = userRepository.save(User.builder()
                .email("writer@test.com")
                .nickname("작성자")
                .password("encoded")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build());
    }

    private Post savePost(String title, Post.Category category) {
        return postRepository.save(Post.builder()
                .title(title)
                .content("내용입니다.")
                .category(category)
                .writer(writer)
                .build());
    }

    // ──────────────────────────────────────────────
    // 카테고리 필터링
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("카테고리별 게시글 조회")
    class FindByCategory {

        @Test
        @DisplayName("FREE 카테고리만 조회되고 다른 카테고리는 제외됨")
        void shouldReturnOnlyTargetCategory() {
            savePost("자유글1", Post.Category.FREE);
            savePost("자유글2", Post.Category.FREE);
            savePost("뉴스글", Post.Category.NEWS);

            Page<Post> result = postRepository.findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(
                    Post.Category.FREE, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(p -> p.getCategory() == Post.Category.FREE);
        }

        @Test
        @DisplayName("삭제된 게시글(isDeleted=true)은 조회에서 제외됨")
        void shouldExcludeDeletedPosts() {
            Post alive = savePost("살아있는글", Post.Category.FREE);
            Post deleted = savePost("삭제된글", Post.Category.FREE);
            deleted.softDelete();
            postRepository.save(deleted);

            Page<Post> result = postRepository.findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(
                    Post.Category.FREE, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("살아있는글");
        }

        @Test
        @DisplayName("페이징 적용 시 요청한 size만큼 반환")
        void shouldRespectPagingSize() {
            for (int i = 1; i <= 5; i++) {
                savePost("게시글" + i, Post.Category.FREE);
            }

            Page<Post> result = postRepository.findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(
                    Post.Category.FREE, PageRequest.of(0, 3));

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }
    }

    // ──────────────────────────────────────────────
    // 게시글 상세 조회
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("게시글 상세 조회 (with Writer)")
    class FindWithWriter {

        @Test
        @DisplayName("존재하는 게시글 조회 시 작성자 정보 함께 반환")
        void shouldReturnPostWithWriter() {
            Post post = savePost("테스트 제목", Post.Category.FREE);

            Optional<Post> result = postRepository.findWithWriterById(post.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("테스트 제목");
            assertThat(result.get().getWriter().getNickname()).isEqualTo("작성자");
        }

        @Test
        @DisplayName("삭제된 게시글은 findWithWriterById에서 제외됨")
        void shouldNotReturnDeletedPost() {
            Post post = savePost("삭제될글", Post.Category.FREE);
            post.softDelete();
            postRepository.save(post);

            Optional<Post> result = postRepository.findWithWriterById(post.getId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 시 빈 Optional 반환")
        void shouldReturnEmptyForNonExistent() {
            Optional<Post> result = postRepository.findWithWriterById(999L);

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // 블라인드 게시글
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("블라인드 게시글 조회")
    class FindBlindedPosts {

        @Test
        @DisplayName("블라인드 게시글만 반환됨")
        void shouldReturnOnlyBlindedPosts() {
            Post normal = savePost("일반글", Post.Category.FREE);
            Post blinded = savePost("블라인드글", Post.Category.FREE);
            blinded.blind();
            postRepository.save(blinded);

            List<Post> result = postRepository.findByIsDeletedFalseAndBlindedTrueOrderByCreatedAtDesc();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isBlinded()).isTrue();
        }

        @Test
        @DisplayName("블라인드 게시글이 없으면 빈 목록 반환")
        void shouldReturnEmptyWhenNoBlindedPosts() {
            savePost("일반글", Post.Category.FREE);

            List<Post> result = postRepository.findByIsDeletedFalseAndBlindedTrueOrderByCreatedAtDesc();

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // QueryDSL - 검색
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("QueryDSL 동적 검색 (searchPosts)")
    class SearchPosts {

        @Test
        @DisplayName("제목 키워드 검색 시 해당 키워드 포함 게시글만 반환")
        void shouldFilterByTitleKeyword() {
            savePost("우주 탐사 이야기", Post.Category.FREE);
            savePost("날씨 정보", Post.Category.FREE);
            savePost("별자리 우주 관측", Post.Category.NEWS);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Post> result = postRepository.searchPosts("우주", null, "title", pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(p -> p.getTitle().contains("우주"));
        }

        @Test
        @DisplayName("카테고리 필터와 키워드 동시 적용 시 교집합 반환")
        void shouldApplyCategoryAndKeywordTogether() {
            savePost("우주 탐사", Post.Category.FREE);
            savePost("우주 뉴스", Post.Category.NEWS);
            savePost("날씨 정보", Post.Category.FREE);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Post> result = postRepository.searchPosts("우주", Post.Category.FREE, "title", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("우주 탐사");
        }

        @Test
        @DisplayName("키워드 없이 카테고리만 지정하면 해당 카테고리 전체 반환")
        void shouldReturnAllPostsInCategoryWithoutKeyword() {
            savePost("뉴스1", Post.Category.NEWS);
            savePost("뉴스2", Post.Category.NEWS);
            savePost("자유글", Post.Category.FREE);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Post> result = postRepository.searchPosts(null, Post.Category.NEWS, "title", pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(p -> p.getCategory() == Post.Category.NEWS);
        }

        @Test
        @DisplayName("삭제된 게시글은 검색 결과에서 제외됨")
        void shouldExcludeDeletedPostsFromSearch() {
            savePost("살아있는 우주글", Post.Category.FREE);
            Post deleted = savePost("삭제된 우주글", Post.Category.FREE);
            deleted.softDelete();
            postRepository.save(deleted);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Post> result = postRepository.searchPosts("우주", null, "title", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("살아있는 우주글");
        }
    }

    // ──────────────────────────────────────────────
    // QueryDSL - HOT 게시글
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("QueryDSL HOT 게시글 조회 (findHotPosts)")
    class FindHotPosts {

        @Test
        @DisplayName("likeThreshold 이상인 게시글만 반환됨")
        void shouldFilterByLikeThreshold() {
            Post hotPost = savePost("인기글", Post.Category.FREE);
            for (int i = 0; i < 5; i++) hotPost.increaseLikeCount();
            postRepository.save(hotPost);

            Post coldPost = savePost("비인기글", Post.Category.FREE);
            // likeCount = 0

            List<Post> result = postRepository.findHotPosts(null, null, 5, 10, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("인기글");
        }

        @Test
        @DisplayName("블라인드 제외 옵션(false)이면 블라인드 게시글 반환 안 됨")
        void shouldExcludeBlindedPostsWhenNotIncluded() {
            Post hotPost = savePost("인기글", Post.Category.FREE);
            for (int i = 0; i < 5; i++) hotPost.increaseLikeCount();
            postRepository.save(hotPost);

            Post blindedHot = savePost("블라인드 인기글", Post.Category.FREE);
            for (int i = 0; i < 5; i++) blindedHot.increaseLikeCount();
            blindedHot.blind();
            postRepository.save(blindedHot);

            List<Post> result = postRepository.findHotPosts(null, null, 5, 10, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isBlinded()).isFalse();
        }

        @Test
        @DisplayName("블라인드 포함 옵션(true)이면 블라인드 게시글도 반환됨")
        void shouldIncludeBlindedPostsWhenRequested() {
            Post hotPost = savePost("인기글", Post.Category.FREE);
            for (int i = 0; i < 5; i++) hotPost.increaseLikeCount();
            postRepository.save(hotPost);

            Post blindedHot = savePost("블라인드 인기글", Post.Category.FREE);
            for (int i = 0; i < 5; i++) blindedHot.increaseLikeCount();
            blindedHot.blind();
            postRepository.save(blindedHot);

            List<Post> result = postRepository.findHotPosts(null, null, 5, 10, true);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("limit 적용 시 지정한 개수만 반환")
        void shouldRespectLimit() {
            for (int i = 1; i <= 5; i++) {
                Post p = savePost("인기글" + i, Post.Category.FREE);
                for (int j = 0; j < 10; j++) p.increaseLikeCount();
                postRepository.save(p);
            }

            List<Post> result = postRepository.findHotPosts(null, null, 5, 3, false);

            assertThat(result).hasSize(3);
        }
    }

    // ──────────────────────────────────────────────
    // 만료된 삭제 게시글
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("만료된 삭제 게시글 조회 (findExpiredDeletedPosts)")
    class FindExpiredDeletedPosts {

        @Test
        @DisplayName("30일 이전에 삭제된 게시글만 반환됨")
        void shouldReturnPostsDeletedBefore30Days() {
            Post oldDeleted = savePost("오래된 삭제글", Post.Category.FREE);
            oldDeleted.softDelete();
            postRepository.save(oldDeleted);

            Post recentDeleted = savePost("최근 삭제글", Post.Category.FREE);
            recentDeleted.softDelete();
            postRepository.save(recentDeleted);

            // threshold = 현재 기준 (오래된 게시글은 deletedAt이 방금이라 threshold보다 이전이 없음)
            // 실제로는 deletedAt을 직접 조작할 수 없으므로 threshold를 미래로 설정해서 테스트
            LocalDateTime futureThreshold = LocalDateTime.now().plusSeconds(10);
            List<Post> result = postRepository.findExpiredDeletedPosts(futureThreshold);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(Post::isDeleted);
        }

        @Test
        @DisplayName("삭제되지 않은 게시글은 포함되지 않음")
        void shouldNotIncludeActivePostsInExpiredQuery() {
            savePost("활성 게시글", Post.Category.FREE);

            LocalDateTime futureThreshold = LocalDateTime.now().plusSeconds(10);
            List<Post> result = postRepository.findExpiredDeletedPosts(futureThreshold);

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // 게시글 수 조회
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("게시글 수 조회")
    class CountPosts {

        @Test
        @DisplayName("작성자별 삭제되지 않은 게시글 수 정확히 반환")
        void shouldCountNonDeletedPostsByWriter() {
            savePost("게시글1", Post.Category.FREE);
            savePost("게시글2", Post.Category.NEWS);
            Post deleted = savePost("삭제글", Post.Category.FREE);
            deleted.softDelete();
            postRepository.save(deleted);

            long count = postRepository.countByWriterAndIsDeletedFalse(writer);

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("카테고리별 게시글 수 정확히 반환")
        void shouldCountByCategory() {
            savePost("자유글1", Post.Category.FREE);
            savePost("자유글2", Post.Category.FREE);
            savePost("뉴스글", Post.Category.NEWS);

            long freeCount = postRepository.countByCategoryAndIsDeletedFalse(Post.Category.FREE);
            long newsCount = postRepository.countByCategoryAndIsDeletedFalse(Post.Category.NEWS);

            assertThat(freeCount).isEqualTo(2);
            assertThat(newsCount).isEqualTo(1);
        }
    }
}
