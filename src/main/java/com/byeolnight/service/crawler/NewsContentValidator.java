package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsApiResponseDto;
import com.byeolnight.entity.News;
import com.byeolnight.repository.NewsRepository;
import com.byeolnight.infrastructure.config.NewsCollectionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsContentValidator {
    
    private final NewsRepository newsRepository;
    private final NewsCollectionProperties newsConfig;
    
    private static final String[] SPACE_KEYWORDS = {
        // 한국어 키워드
        "우주", "로켓", "위성", "화성", "달", "태양", "지구", "목성", "토성", "블랙홀", "은하", "별", "항성", "혜성", "소행성", "망원경", "천문", "항공우주", "우주선", "우주정거장", "우주비행사", "우주탐사", "화성탐사", "달탐사",
        // 영어 키워드 (소문자)
        "nasa", "spacex", "space", "mars", "moon", "astronomy", "telescope", "satellite", "rocket", "planet", "solar", "lunar", "jupiter", "saturn", "venus", "mercury", "neptune", "uranus", "pluto", "galaxy", "nebula", "star", "comet", "asteroid", "meteor", "orbit", "spacecraft", "astronaut", "cosmology", "astrophysics", "observatory", "constellation", "eclipse", "aurora", "supernova", "quasar", "pulsar", "exoplanet", "milky way", "andromeda", "hubble", "webb", "iss", "international space station", "falcon", "dragon", "starship", "artemis", "apollo", "voyager", "cassini", "juno", "perseverance", "curiosity", "ingenuity", "parker solar probe", "james webb", "kepler", "spitzer", "chandra", "esa", "roscosmos", "jaxa", "isro", "cnsa"
    };
    private static final String[] EXCLUDE_KEYWORDS = {
        "trump", "biden", "정치", "선거", "경제", "주식", "스포츠", "축구", "농구",
        "sports", "football", "basketball", "soccer", "fashion", "food", "recipe", "cooking"
    };
    private static final String[] NON_SPACE_CONTENT_KEYWORDS = {
        // 게임
        "gaming", "gameplay", "video game", "mmo", "mmorpg", "esports", "steam", "playstation", "xbox", "nintendo",
        "게임", "게이밍", "플레이스테이션", "닌텐도", "이스포츠",
        // 가상자산·투기성 콘텐츠
        "bitcoin", "cryptocurrency", "crypto", "blockchain", "nft", "token sale", "memecoin",
        "비트코인", "암호화폐", "가상자산", "블록체인", "코인", "토큰 판매",
        // 연예·영상물
        "entertainment", "celebrity", "movie", "film review", "tv series", "streaming series", "box office",
        "연예", "영화", "드라마", "예능", "배우", "가수", "흥행"
    };
    private static final String[] GAME_CONTEXT_KEYWORDS = {
            "player", "playable", "developer", "game studio", "early access", "console", "game release", "game beta"
    };
    private static final String[] TRUSTED_SOURCES = {"nasa", "esa", "spacex", "science", "nature", "space", "astronomy", "reuters", "ap", "bbc", "cnn", "연합뉴스", "ytn", "kbs", "mbc", "sbs", "한국항공우주연구원", "kari", "과학기술정보통신부"};
    
    public boolean isHighQualityNews(NewsApiResponseDto.Result result) {
        return hasMinimumLength(result) && 
               isSpaceRelated(result) && 
               isReliableSource(result) && 
               !isSimilarToExisting(result);
    }
    
    private boolean hasMinimumLength(NewsApiResponseDto.Result result) {
        String title = result.getTitle() != null ? result.getTitle() : "";
        String articleText = getArticleText(result);
        
        return title.length() >= newsConfig.getQuality().getMinTitleLength()
                && articleText.length() >= newsConfig.getQuality().getMinDescriptionLength();
    }
    
    private boolean isSpaceRelated(NewsApiResponseDto.Result result) {
        String title = result.getTitle() != null ? result.getTitle() : "";
        String content = (title + " " + getArticleText(result)).toLowerCase();

        if (isGameContent(content)) {
            log.info("명백한 게임 콘텐츠로 제외: {}", result.getTitle());
            return false;
        }

        for (String keyword : NON_SPACE_CONTENT_KEYWORDS) {
            if (containsKeyword(content, keyword)) {
                log.info("명백한 비우주 콘텐츠 키워드로 제외: {} ({})", result.getTitle(), keyword);
                return false;
            }
        }
        
        // 비우주 키워드 체크
        for (String exclude : EXCLUDE_KEYWORDS) {
            if (containsKeyword(content, exclude)) return false;
        }
        
        int keywordCount = 0;
        for (String keyword : SPACE_KEYWORDS) {
            if (containsKeyword(content, keyword.toLowerCase())) keywordCount++;
        }
        
        return keywordCount >= newsConfig.getQuality().getMinSpaceKeywords();
    }

    private boolean containsKeyword(String content, String keyword) {
        if (keyword.chars().allMatch(character -> character < 128)
                && keyword.chars().noneMatch(Character::isWhitespace)) {
            return content.matches("(?s).*\\b" + java.util.regex.Pattern.quote(keyword) + "\\b.*");
        }
        return content.contains(keyword);
    }

    private boolean isGameContent(String content) {
        String gameContext = content
                .replace("game-changing", "")
                .replace("game changer", "");
        if (!containsKeyword(gameContext, "game")) {
            return false;
        }
        return java.util.Arrays.stream(GAME_CONTEXT_KEYWORDS)
                .anyMatch(keyword -> containsKeyword(gameContext, keyword));
    }

    private String getArticleText(NewsApiResponseDto.Result result) {
        String description = result.getDescription() != null ? result.getDescription() : "";
        String content = result.getContent() != null ? result.getContent() : "";
        return content.length() > description.length() ? content : description;
    }
    
    private boolean isReliableSource(NewsApiResponseDto.Result result) {
        String sourceName = result.getSourceName();
        if (sourceName == null) return true; // null인 경우 통과 (다른 조건으로 필터링)
        
        String sourceNameLower = sourceName.toLowerCase();
        
        // 신뢰할 수 있는 출처 체크
        for (String trusted : TRUSTED_SOURCES) {
            if (sourceNameLower.contains(trusted)) return true;
        }
        
        // 의심스러운 출처만 제외 (나머지는 통과)
        String[] untrusted = {"blog", "personal", "unknown", "anonymous", "fake", "rumor"};
        for (String pattern : untrusted) {
            if (sourceNameLower.contains(pattern)) return false;
        }
        
        return true; // 기본적으로 통과
    }
    
    private boolean isSimilarToExisting(NewsApiResponseDto.Result result) {
        int checkDays = Math.max(0, newsConfig.getCollection().getSimilarityCheckDays());
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(checkDays);
        List<News> recentNews = newsRepository.findByPublishedAtAfter(cutoffDate);
        
        String newTitle = normalizeTitle(result.getTitle());
        
        return recentNews.stream()
                .anyMatch(news -> calculateSimilarity(newTitle, normalizeTitle(news.getTitle()))
                        >= newsConfig.getCollection().getSimilarityThreshold());
    }

    public boolean isSimilarToBatch(NewsApiResponseDto.Result result, List<String> collectedTitles) {
        String newTitle = normalizeTitle(result.getTitle());
        double threshold = newsConfig.getCollection().getSimilarityThreshold();
        return collectedTitles.stream()
                .map(this::normalizeTitle)
                .anyMatch(title -> calculateSimilarity(newTitle, title) >= threshold);
    }
    
    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase()
                .replaceAll("[^\\w\\s가-힣]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    private double calculateSimilarity(String title1, String title2) {
        String[] words1 = title1.split("\\s+");
        String[] words2 = title2.split("\\s+");
        
        int commonWords = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) && word1.length() > 2) {
                    commonWords++;
                    break;
                }
            }
        }
        
        return (double) commonWords / Math.max(words1.length, words2.length);
    }
}
