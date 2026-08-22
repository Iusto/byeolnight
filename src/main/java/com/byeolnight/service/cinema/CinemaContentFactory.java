package com.byeolnight.service.cinema;

import com.byeolnight.dto.cinema.CinemaAiContentDto;
import com.byeolnight.dto.cinema.CinemaVideoData;
import com.byeolnight.dto.external.youtube.YouTubeSnippet;
import com.byeolnight.dto.external.youtube.YouTubeVideoDetailItem;

import java.util.stream.Collectors;

/** 선택된 영상과 AI 결과를 저장 가능한 별빛시네마 콘텐츠로 조립한다. */
final class CinemaContentFactory {

    private CinemaContentFactory() {
    }

    static CinemaVideoData create(YouTubeVideoDetailItem video, CinemaAiContentDto ai) {
        YouTubeSnippet snippet = video.getSnippet();
        String hashtags = ai.getTags().stream()
                .map(tag -> "#" + tag.replace("#", ""))
                .collect(Collectors.joining(" "));
        String url = "https://www.youtube.com/watch?v=" + video.getId();
        return new CinemaVideoData(
                ai.getKoreanTitle(),
                ai.getIntroduction(),
                video.getId(),
                url,
                snippet.getChannelTitle(),
                CinemaVideoPolicy.publishedAt(video),
                ai.getWhySelected(),
                hashtags,
                formatContent(ai, video, url, hashtags));
    }

    private static String formatContent(
            CinemaAiContentDto ai,
            YouTubeVideoDetailItem video,
            String url,
            String hashtags
    ) {
        String points = ai.getKeyPoints().stream()
                .map(point -> "- " + point)
                .collect(Collectors.joining("\n"));
        return """
                ## 선정 이유
                %s

                ## 영상 소개
                %s

                ## 핵심 포인트
                %s

                ## 추천 대상
                %s

                ## 출처
                채널: %s
                발행일: %s
                원본 영상: %s

                %s

                > 이 소개는 YouTube가 제공한 제목과 설명을 바탕으로 AI가 생성했습니다.
                """.formatted(ai.getWhySelected(), ai.getIntroduction(), points, ai.getRecommendedFor(),
                video.getSnippet().getChannelTitle(), CinemaVideoPolicy.publishedAt(video).toLocalDate(), url, hashtags);
    }
}
