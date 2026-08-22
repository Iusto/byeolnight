package com.byeolnight.service.weather;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObservationScoreService 테스트")
class ObservationScoreServiceTest {

    private final ObservationScoreService scoreService = new ObservationScoreService();

    @Test
    @DisplayName("맑고 가시거리가 길며 달빛이 적으면 최고 점수")
    void shouldReturnMaximumScoreForIdealConditions() {
        // when
        ObservationScoreService.ObservationScore result = scoreService.calculate(0, 10, "🌑");

        // then
        assertThat(result.totalScore()).isEqualTo(100);
        assertThat(result.cloudScore()).isEqualTo(55);
        assertThat(result.visibilityScore()).isEqualTo(30);
        assertThat(result.moonScore()).isEqualTo(15);
        assertThat(result.quality()).isEqualTo("EXCELLENT");
    }

    @Test
    @DisplayName("구름과 짧은 가시거리, 보름달은 관측 점수를 낮춘다")
    void shouldReturnPoorScoreForBadConditions() {
        // when
        ObservationScoreService.ObservationScore result = scoreService.calculate(100, 0, "🌕");

        // then
        assertThat(result.totalScore()).isEqualTo(2);
        assertThat(result.quality()).isEqualTo("POOR");
    }

    @Test
    @DisplayName("외부 API의 비정상 범위 값은 허용 범위로 보정")
    void shouldClampOutOfRangeInputs() {
        // when
        ObservationScoreService.ObservationScore result = scoreService.calculate(-20, 20, "UNKNOWN");

        // then
        assertThat(result.cloudScore()).isEqualTo(55);
        assertThat(result.visibilityScore()).isEqualTo(30);
        assertThat(result.moonScore()).isEqualTo(7);
        assertThat(result.totalScore()).isEqualTo(92);
    }

    @Test
    @DisplayName("점수 구간에 따라 관측 품질을 구분")
    void shouldClassifyScoreBands() {
        assertThat(scoreService.calculate(0, 10, "🌕").quality()).isEqualTo("EXCELLENT");
        assertThat(scoreService.calculate(30, 10, "🌕").quality()).isEqualTo("GOOD");
        assertThat(scoreService.calculate(50, 5, "🌕").quality()).isEqualTo("FAIR");
        assertThat(scoreService.calculate(90, 1, "🌕").quality()).isEqualTo("POOR");
    }
}
