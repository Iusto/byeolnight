package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsAiContentDto;
import com.byeolnight.dto.ai.NewsApiResponseDto;
import com.byeolnight.entity.News;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.NewsRepository;
import com.byeolnight.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** AI 처리가 끝난 뉴스 한 건을 하나의 짧은 트랜잭션으로 저장한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceNewsPersistenceService {

    private static final String DEFAULT_IMAGE =
            "https://images.unsplash.com/photo-1446776877081-d282a0f896e2?w=800&h=600&fit=crop";

    private final NewsRepository newsRepository;
    private final PostRepository postRepository;
    private final NewsContentFormatter formatter;

    @Transactional
    public Post save(NewsApiResponseDto.Result source, NewsAiContentDto aiContent, User writer) {
        News news = News.builder()
                .title(aiContent.getKoreanTitle())
                .description(aiContent.getOverview())
                .imageUrl(source.getImageUrl() != null ? source.getImageUrl() : DEFAULT_IMAGE)
                .url(source.getLink())
                .publishedAt(parsePublishedAt(source.getPubDate()))
                .hashtags(formatter.formatHashtags(aiContent))
                .source(source.getSourceName() != null ? source.getSourceName() : "Unknown")
                .summary(aiContent.getWhyItMatters())
                .build();
        newsRepository.save(news);

        String title = aiContent.getKoreanTitle();
        if (title.length() > 100) {
            title = title.substring(0, 97) + "...";
        }
        Post post = Post.builder()
                .title(title)
                .content(formatter.formatNewsContent(source, aiContent))
                .category(Post.Category.NEWS)
                .writer(writer)
                .build();
        return postRepository.save(post);
    }

    private LocalDateTime parsePublishedAt(String publishedAt) {
        try {
            return LocalDateTime.parse(publishedAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception exception) {
                log.warn("뉴스 발행일 파싱 실패로 현재 시각을 사용합니다: {}", publishedAt);
                return LocalDateTime.now();
            }
        }
    }
}
