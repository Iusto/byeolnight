package com.byeolnight.service.weather;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 밤하늘 관측 점수를 계산한다.
 *
 * <p>점수는 새로운 외부 API 없이 현재 제공되는 구름량, 가시거리, 달 위상만 사용한다.
 * 구름과 가시거리는 맨눈 관측 전반에, 달빛은 성운·은하 같은 딥스카이 관측에 미치는
 * 영향을 반영한다. 각 항목 점수를 함께 반환해 사용자에게 산출 근거를 공개한다.</p>
 */
@Service
public class ObservationScoreService {

    private static final int CLOUD_MAX_SCORE = 55;
    private static final int VISIBILITY_MAX_SCORE = 30;
    private static final int MOON_MAX_SCORE = 15;

    private static final Map<String, Integer> MOON_SCORES = Map.of(
            "🌑", 15,
            "🌒", 12,
            "🌓", 8,
            "🌔", 5,
            "🌕", 2,
            "🌖", 5,
            "🌗", 8,
            "🌘", 12
    );

    public ObservationScore calculate(double cloudCover, double visibilityKm, String moonPhase) {
        int cloudScore = (int) Math.round(CLOUD_MAX_SCORE * (1 - clamp(cloudCover, 0, 100) / 100));
        int visibilityScore = (int) Math.round(VISIBILITY_MAX_SCORE * clamp(visibilityKm, 0, 10) / 10);
        int moonScore = MOON_SCORES.getOrDefault(moonPhase, MOON_MAX_SCORE / 2);
        int totalScore = (int) clamp(cloudScore + visibilityScore + moonScore, 0, 100);

        return new ObservationScore(
                totalScore,
                cloudScore,
                visibilityScore,
                moonScore,
                qualityFor(totalScore)
        );
    }

    private String qualityFor(int score) {
        if (score >= 85) return "EXCELLENT";
        if (score >= 70) return "GOOD";
        if (score >= 45) return "FAIR";
        return "POOR";
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** 점수 총합과 항목별 기여도를 함께 전달하는 불변 계산 결과다. */
    public record ObservationScore(
            int totalScore,
            int cloudScore,
            int visibilityScore,
            int moonScore,
            String quality
    ) {
    }
}
