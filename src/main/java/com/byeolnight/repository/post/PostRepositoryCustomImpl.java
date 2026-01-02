package com.byeolnight.repository.post;

import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.post.Post.Category;
import com.byeolnight.entity.user.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.byeolnight.entity.post.QPost.post;
import static com.byeolnight.entity.user.QUser.user;

@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> searchPosts(String keyword, Category category, String searchType, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        
        builder.and(post.isDeleted.eq(false))
               .and(post.writer.isNotNull())
               .and(post.writer.status.ne(User.UserStatus.WITHDRAWN));
        
        if (category != null) {
            builder.and(post.category.eq(category));
        }
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            switch (searchType) {
                case "title" -> builder.and(post.title.containsIgnoreCase(keyword));
                case "content" -> builder.and(post.content.containsIgnoreCase(keyword));
                case "writer" -> builder.and(post.writer.nickname.containsIgnoreCase(keyword));
                default -> builder.and(post.title.containsIgnoreCase(keyword)
                        .or(post.content.containsIgnoreCase(keyword)));
            }
        }
        
        List<Post> content = queryFactory
                .selectFrom(post)
                .join(post.writer, user).fetchJoin()
                .where(builder)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        
        long total = queryFactory
                .selectFrom(post)
                .join(post.writer, user)
                .where(builder)
                .fetchCount();
        
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Post> findHotPosts(Category category, LocalDateTime threshold, int likeThreshold, int limit, boolean includeBlinded) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(post.isDeleted.eq(false))
               .and(post.likeCount.goe(likeThreshold))
               .and(post.writer.isNotNull())
               .and(post.writer.status.ne(User.UserStatus.WITHDRAWN));

        // 블라인드 포함 여부 결정 (관리자가 아니면 블라인드 제외)
        if (!includeBlinded) {
            builder.and(post.blinded.eq(false));
        }

        if (category != null) {
            builder.and(post.category.eq(category));
        }
        if (threshold != null) {
            builder.and(post.createdAt.goe(threshold));
        }

        return queryFactory
                .selectFrom(post)
                .join(post.writer, user).fetchJoin()
                .where(builder)
                .orderBy(post.likeCount.desc(), post.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<PostCategoryStats> getUserPostStatsByCategory(Long userId) {
        return queryFactory
                .select(Projections.constructor(PostCategoryStats.class,
                        post.category,
                        post.count()
                ))
                .from(post)
                .where(post.writer.id.eq(userId)
                        .and(post.isDeleted.eq(false)))
                .groupBy(post.category)
                .orderBy(post.count().desc())
                .fetch();
    }

    @Override
    public Page<Post> findPostsForAdmin(PostAdminSearchCondition condition, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        
        if (condition != null) {
            if (condition.isDeleted() != null) {
                builder.and(post.isDeleted.eq(condition.isDeleted()));
            }
            if (condition.isBlinded() != null) {
                builder.and(post.blinded.eq(condition.isBlinded()));
            }
            if (condition.category() != null) {
                builder.and(post.category.eq(condition.category()));
            }
            if (condition.keyword() != null && !condition.keyword().trim().isEmpty()) {
                builder.and(post.title.containsIgnoreCase(condition.keyword())
                        .or(post.content.containsIgnoreCase(condition.keyword())));
            }
            if (condition.startDate() != null) {
                builder.and(post.createdAt.goe(condition.startDate()));
            }
            if (condition.endDate() != null) {
                builder.and(post.createdAt.loe(condition.endDate()));
            }
        }
        
        List<Post> content = queryFactory
                .selectFrom(post)
                .leftJoin(post.writer, user).fetchJoin()
                .where(builder)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        
        long total = queryFactory
                .selectFrom(post)
                .where(builder)
                .fetchCount();
        
        return new PageImpl<>(content, pageable, total);
    }
}