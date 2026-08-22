package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsAiContentDto;
import com.byeolnight.dto.ai.NewsApiResponseDto;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.config.NewsCollectionProperties;
import com.byeolnight.repository.NewsRepository;
import com.byeolnight.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 뉴스 후보의 검증·AI 변환·저장 순서를 조정하는 애플리케이션 서비스다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceNewsService {

    private static final String NEWS_BOT_EMAIL = "newsbot@byeolnight.com";

    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final NewsCollectionProperties newsConfig;
    private final NewsContentValidator validator;
    private final NewsTranslationService translationService;
    private final NewsDataClient newsDataClient;
    private final SpaceNewsPersistenceService persistenceService;

    /** 외부 API와 AI 호출은 트랜잭션 밖에서 수행하고, 확정된 기사만 짧게 저장한다. */
    public void collectAndSaveSpaceNews() {
        log.info("우주 뉴스 수집을 시작합니다.");
        NewsApiResponseDto response = newsDataClient.fetchSpaceNews();
        if (response == null || response.getResults() == null) {
            log.warn("수집할 뉴스 응답이 없습니다.");
            return;
        }

        User newsBot = userRepository.findByEmail(NEWS_BOT_EMAIL)
                .orElseThrow(() -> new IllegalStateException("뉴스봇 사용자를 찾을 수 없습니다."));
        int maxPosts = newsConfig.getCollection().getMaxPosts();
        int savedCount = 0;
        int duplicateCount = 0;
        int filteredCount = 0;
        int aiFailureCount = 0;
        List<String> collectedTitles = new ArrayList<>();

        for (NewsApiResponseDto.Result article : response.getResults()) {
            if (savedCount >= maxPosts) {
                break;
            }
            if (newsRepository.existsByUrl(article.getLink())
                    || validator.isSimilarToBatch(article, collectedTitles)) {
                duplicateCount++;
                continue;
            }
            if (!validator.isHighQualityNews(article)) {
                filteredCount++;
                continue;
            }

            Optional<NewsAiContentDto> generated = translationService.generateNewsContent(article);
            if (generated.isEmpty()) {
                aiFailureCount++;
                log.warn("AI 뉴스 콘텐츠 생성 실패로 저장하지 않습니다: {}", article.getTitle());
                continue;
            }

            Post savedPost = persistenceService.save(article, generated.get(), newsBot);
            savedCount++;
            collectedTitles.add(article.getTitle());
            log.info("뉴스 게시글 저장 완료: {}", savedPost.getTitle());
        }

        log.info("우주 뉴스 수집 완료: 후보 {}건, 저장 {}건, 중복 {}건, 품질 제외 {}건, AI 실패 {}건",
                response.getResults().size(), savedCount, duplicateCount, filteredCount, aiFailureCount);
    }

    /** 기존 관리자 API 호환을 위한 NewsData 조회 진입점이다. */
    public NewsApiResponseDto fetchKoreanSpaceNews() {
        return newsDataClient.fetchSpaceNews();
    }

    public long getTodayNewsCount() {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        return newsRepository.countByCreatedAtAfter(todayStart);
    }
}
