package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsAiContentDto;
import com.byeolnight.dto.ai.NewsApiResponseDto;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.NewsRepository;
import com.byeolnight.repository.post.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceNewsPersistenceServiceTest {

    @Mock NewsRepository newsRepository;
    @Mock PostRepository postRepository;
    @Mock NewsContentFormatter formatter;
    @Mock User writer;

    @Test
    void savesCollectedNewsAsPublicPost() {
        SpaceNewsPersistenceService service =
                new SpaceNewsPersistenceService(newsRepository, postRepository, formatter);
        NewsApiResponseDto.Result source = new NewsApiResponseDto.Result();
        source.setLink("https://example.com/space-news");
        source.setSourceName("NASA");
        source.setPubDate("2026-10-01 12:00:00");
        NewsAiContentDto content = new NewsAiContentDto();
        content.setKoreanTitle("새로운 우주 뉴스");
        content.setOverview("뉴스 개요");
        content.setWhyItMatters("뉴스의 의미");
        when(formatter.formatHashtags(content)).thenReturn("#우주");
        when(formatter.formatNewsContent(source, content)).thenReturn("공개할 뉴스 본문");
        when(postRepository.save(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Post saved = service.save(source, content, writer);

        assertThat(saved.getCategory()).isEqualTo(Post.Category.NEWS);
        assertThat(saved.isBlinded()).isFalse();
        assertThat(saved.getBlindType()).isNull();
        assertThat(saved.getBlindedAt()).isNull();
    }
}
