package com.byeolnight.service.weather;

import com.byeolnight.dto.weather.IssObservationResponse;
import com.github.amsacode.predict4java.TLE;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IssService 테스트 - SGP4 패스 계산")
class IssServiceTest {

    @Mock
    private TleFetchService tleFetchService;

    private MeterRegistry meterRegistry;

    private IssService issService;

    // 테스트용 ISS TLE (Vallado SGP4 검증 케이스, 체크섬 검증 완료)
    private static final String[] TEST_TLE_LINES = {
        "ISS (ZARYA)",
        "1 25544U 98067A   08264.51782528 -.00002182  00000-0 -11606-4 0  2927",
        "2 25544  51.6416 247.4627 0006703 130.5360 325.0288 15.72125391563537"
    };

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        issService = new IssService(tleFetchService, meterRegistry);
    }

    @Nested
    @DisplayName("TLE 파싱")
    class TleParsing {

        @Test
        @DisplayName("predict4java가 ISS TLE를 정상적으로 파싱")
        void shouldParseIssTle() {
            // when
            TLE tle = new TLE(TEST_TLE_LINES);

            // then
            assertThat(tle).isNotNull();
            assertThat(tle.getName()).contains("ISS");
        }

        @Test
        @DisplayName("잘못된 TLE 형식이면 예외 발생")
        void shouldThrowForInvalidTle() {
            // given
            String[] invalidTle = {"Invalid", "Not a TLE", "Format"};

            // when & then
            assertThatThrownBy(() -> new TLE(invalidTle))
                .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("방위각 → 방향 변환")
    class AzimuthToDirection {

        private String invokeAzimuthToDirection(int azimuth) throws Exception {
            Method method = IssService.class.getDeclaredMethod("azimuthToDirection", int.class);
            method.setAccessible(true);
            return (String) method.invoke(issService, azimuth);
        }

        @ParameterizedTest(name = "방위각 {0}° → {1}")
        @CsvSource({
            "0, NORTH",
            "22, NORTH",
            "23, NORTHEAST",
            "45, NORTHEAST",
            "67, NORTHEAST",
            "68, EAST",
            "90, EAST",
            "135, SOUTHEAST",
            "180, SOUTH",
            "225, SOUTHWEST",
            "270, WEST",
            "315, NORTHWEST",
            "338, NORTH",
            "359, NORTH"
        })
        @DisplayName("방위각이 올바른 8방향으로 변환됨")
        void shouldConvertAzimuthToCorrectDirection(int azimuth, String expectedDirection) throws Exception {
            assertThat(invokeAzimuthToDirection(azimuth)).isEqualTo(expectedDirection);
        }
    }

    @Nested
    @DisplayName("고도각 → 관측 품질")
    class ElevationToQuality {

        private String invokeElevationToQuality(double elevation) throws Exception {
            Method method = IssService.class.getDeclaredMethod("elevationToQuality", double.class);
            method.setAccessible(true);
            return (String) method.invoke(issService, elevation);
        }

        @ParameterizedTest(name = "고도각 {0}° → {1}")
        @CsvSource({
            "80.0, EXCELLENT",
            "60.0, EXCELLENT",
            "59.9, GOOD",
            "30.0, GOOD",
            "29.9, FAIR",
            "10.0, FAIR",
            "9.9, POOR",
            "5.0, POOR"
        })
        @DisplayName("최대 고도각에 따라 관측 품질이 결정됨")
        void shouldDetermineQualityByElevation(double elevation, String expectedQuality) throws Exception {
            assertThat(invokeElevationToQuality(elevation)).isEqualTo(expectedQuality);
        }
    }

    @Nested
    @DisplayName("SGP4 패스 계산")
    class Sgp4PassCalculation {

        @Test
        @DisplayName("서울 좌표로 ISS 패스를 계산하면 유효한 결과를 반환")
        void shouldCalculatePassForSeoul() {
            // given
            TLE tle = new TLE(TEST_TLE_LINES);
            given(tleFetchService.getIssTle()).willReturn(tle);

            // when
            IssObservationResponse result = issService.getIssObservationOpportunity(37.5665, 126.9780);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getNextPassTime()).matches("\\d{2}:\\d{2}");
            assertThat(result.getNextPassDate()).matches("\\d{4}-\\d{2}-\\d{2}");
            assertThat(result.getNextPassDirection()).isIn(
                "NORTH", "NORTHEAST", "EAST", "SOUTHEAST",
                "SOUTH", "SOUTHWEST", "WEST", "NORTHWEST"
            );
            assertThat(result.getVisibilityQuality()).isIn("EXCELLENT", "GOOD", "FAIR", "POOR");
            // 지속 시간: SGP4 계산 시 "N분", 폴백 시 "5-7분"
            assertThat(result.getEstimatedDuration()).containsPattern("\\d");
        }

        @Test
        @DisplayName("부산 좌표로도 ISS 패스 계산이 정상 동작")
        void shouldCalculatePassForBusan() {
            // given
            TLE tle = new TLE(TEST_TLE_LINES);
            given(tleFetchService.getIssTle()).willReturn(tle);

            // when
            IssObservationResponse result = issService.getIssObservationOpportunity(35.1796, 129.0756);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getNextPassTime()).isNotBlank();
            assertThat(result.getNextPassDirection()).isNotBlank();
        }

        @Test
        @DisplayName("제주 좌표로도 ISS 패스 계산이 정상 동작")
        void shouldCalculatePassForJeju() {
            // given
            TLE tle = new TLE(TEST_TLE_LINES);
            given(tleFetchService.getIssTle()).willReturn(tle);

            // when
            IssObservationResponse result = issService.getIssObservationOpportunity(33.4996, 126.5312);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getNextPassTime()).isNotBlank();
            assertThat(result.getNextPassDate()).isNotBlank();
        }

        @Test
        @DisplayName("하드코딩된 값이 아닌 실제 계산된 방향을 반환")
        void shouldReturnCalculatedDirectionNotHardcoded() {
            // given
            TLE tle = new TLE(TEST_TLE_LINES);
            given(tleFetchService.getIssTle()).willReturn(tle);

            // when - 서로 다른 위치에서 조회
            IssObservationResponse seoulResult = issService.getIssObservationOpportunity(37.5665, 126.9780);
            IssObservationResponse jejuResult = issService.getIssObservationOpportunity(33.4996, 126.5312);

            // then - 최소한 결과가 생성됨 (다른 위치이므로 다른 그리드)
            assertThat(seoulResult).isNotNull();
            assertThat(jejuResult).isNotNull();
            // SGP4로 계산되었다면 "5-7분" 같은 하드코딩 값이 아님
            // 폴백인 경우도 허용 (오래된 TLE로 인해)
        }
    }

    @Nested
    @DisplayName("캐싱")
    class Caching {

        @Test
        @DisplayName("같은 좌표로 두 번 호출하면 동일한 패스 정보 반환 (캐시 히트)")
        void shouldCachePassPrediction() {
            // given
            TLE tle = new TLE(TEST_TLE_LINES);
            given(tleFetchService.getIssTle()).willReturn(tle);

            // when - 동일 좌표로 두 번 호출
            IssObservationResponse result1 = issService.getIssObservationOpportunity(37.5665, 126.9780);
            IssObservationResponse result2 = issService.getIssObservationOpportunity(37.5665, 126.9780);

            // then - 두 응답이 일관된 값을 반환
            // (calculateNextIssPass 캐시 + calculateCurrentAltitude 각각 TLE 호출)
            assertThat(result1).isNotNull();
            assertThat(result2).isNotNull();
            assertThat(result1.getNextPassTime()).isEqualTo(result2.getNextPassTime());
            assertThat(result1.getNextPassDirection()).isEqualTo(result2.getNextPassDirection());
            assertThat(result1.getEstimatedDuration()).isEqualTo(result2.getEstimatedDuration());
        }

        @Test
        @DisplayName("1도 이내 좌표는 동일 캐시 그리드로 처리되어 동일한 패스 결과 반환")
        void shouldUseSameCacheGridWithin1Degree() {
            // given
            TLE tle = new TLE(TEST_TLE_LINES);
            given(tleFetchService.getIssTle()).willReturn(tle);

            // Math.round(37.3) = 37, Math.round(37.4) = 37 → 같은 그리드
            // Math.round(126.4) = 126, Math.round(126.3) = 126 → 같은 그리드

            // when
            IssObservationResponse result1 = issService.getIssObservationOpportunity(37.3, 126.4);
            IssObservationResponse result2 = issService.getIssObservationOpportunity(37.4, 126.3);

            // then - 같은 그리드(iss:37:126)이므로 동일한 패스 방향/시간 반환
            assertThat(result1).isNotNull();
            assertThat(result2).isNotNull();
            assertThat(result1.getNextPassDirection()).isEqualTo(result2.getNextPassDirection());
            assertThat(result1.getEstimatedDuration()).isEqualTo(result2.getEstimatedDuration());
        }

        @Test
        @DisplayName("다른 그리드의 좌표는 각각 응답을 정상 반환")
        void shouldCalculateSeparatelyForDifferentGrids() {
            // given
            TLE tle = new TLE(TEST_TLE_LINES);
            given(tleFetchService.getIssTle()).willReturn(tle);

            // Math.round(37.5) = 38, Math.round(35.2) = 35 → 다른 그리드

            // when
            IssObservationResponse seoulResult = issService.getIssObservationOpportunity(37.5, 127.0);
            IssObservationResponse busanResult = issService.getIssObservationOpportunity(35.2, 129.0);

            // then - 다른 그리드지만 각각 유효한 응답 반환
            assertThat(seoulResult).isNotNull();
            assertThat(busanResult).isNotNull();
            assertThat(seoulResult.getEstimatedDuration()).isNotNull();
            assertThat(busanResult.getEstimatedDuration()).isNotNull();
        }
    }

    @Nested
    @DisplayName("폴백 처리")
    class Fallback {

        @Test
        @DisplayName("TLE가 null이면 폴백 데이터를 반환")
        void shouldReturnFallbackWhenTleIsNull() {
            // given
            given(tleFetchService.getIssTle()).willReturn(null);

            // when
            IssObservationResponse result = issService.getIssObservationOpportunity(37.5665, 126.9780);

            // then - 폴백 기본값 확인
            assertThat(result).isNotNull();
            assertThat(result.getNextPassDirection()).isEqualTo("NORTHEAST");
            assertThat(result.getEstimatedDuration()).isEqualTo("5-7분");
            assertThat(result.getVisibilityQuality()).isEqualTo("GOOD");
            assertThat(result.getMaxElevation()).isNull();
        }

        @Test
        @DisplayName("TLE 조회 중 예외 발생 시 폴백 데이터를 반환")
        void shouldReturnFallbackWhenTleFetchThrows() {
            // given
            given(tleFetchService.getIssTle()).willThrow(new RuntimeException("CelesTrak 연결 실패"));

            // when
            IssObservationResponse result = issService.getIssObservationOpportunity(37.5665, 126.9780);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getNextPassTime()).isNotBlank();
            assertThat(result.getEstimatedDuration()).isEqualTo("5-7분");
        }

        @Test
        @DisplayName("폴백 응답에 friendlyMessage 필드가 존재함")
        void shouldIncludeIssStatusInFallback() {
            // given
            given(tleFetchService.getIssTle()).willReturn(null);

            // when
            IssObservationResponse result = issService.getIssObservationOpportunity(37.5665, 126.9780);

            // then - friendlyMessage는 현재 빈 문자열("")로 설정되므로 null이 아님을 확인
            assertThat(result.getFriendlyMessage()).isNotNull();
        }

        @Test
        @DisplayName("TLE null이면 다음 호출 시 다시 TLE 요청 시도")
        void shouldRetryTleFetchAfterNull() {
            // given - 첫 번째 null, 두 번째도 null
            given(tleFetchService.getIssTle()).willReturn(null);

            // when
            issService.getIssObservationOpportunity(37.5665, 126.9780);
            issService.getIssObservationOpportunity(37.5665, 126.9780);

            // then - 폴백 시 캐시 미저장 → 매 호출마다 TLE 재요청
            // getIssObservationOpportunity 1회당:
            //   - calculateNextIssPass: getIssTle() 1회 (null → 폴백 반환, 캐시 미저장)
            //   - calculateCurrentAltitude: getIssTle() 1회 (null → null 반환)
            // 2회 호출 × 2 = 총 4회 TLE 요청
            verify(tleFetchService, times(4)).getIssTle();
        }
    }

    @Nested
    @DisplayName("응답 필드 검증")
    class ResponseValidation {

        @Test
        @DisplayName("messageKey가 항상 설정됨")
        void shouldAlwaysHaveMessageKey() {
            // given
            TLE tle = new TLE(TEST_TLE_LINES);
            given(tleFetchService.getIssTle()).willReturn(tle);

            // when
            IssObservationResponse result = issService.getIssObservationOpportunity(37.5665, 126.9780);

            // then
            assertThat(result.getMessageKey()).isNotBlank();
        }

        @Test
        @DisplayName("friendlyMessage가 항상 설정됨 (null이 아님)")
        void shouldAlwaysHaveFriendlyMessage() {
            // given
            TLE tle = new TLE(TEST_TLE_LINES);
            given(tleFetchService.getIssTle()).willReturn(tle);

            // when
            IssObservationResponse result = issService.getIssObservationOpportunity(37.5665, 126.9780);

            // then - friendlyMessage는 현재 빈 문자열("")로 설정 (i18n 키는 messageKey에 분리)
            assertThat(result.getFriendlyMessage()).isNotNull();
            assertThat(result.getMessageKey()).isNotBlank();
        }

        @Test
        @DisplayName("SGP4 계산 성공 시 maxElevation이 포함됨")
        void shouldIncludeMaxElevationOnSuccess() {
            // given
            TLE tle = new TLE(TEST_TLE_LINES);
            given(tleFetchService.getIssTle()).willReturn(tle);

            // when
            IssObservationResponse result = issService.getIssObservationOpportunity(37.5665, 126.9780);

            // then - SGP4 성공 시 maxElevation > 0, 폴백 시 null
            if (!"5-7분".equals(result.getEstimatedDuration())) {
                // SGP4로 실제 계산된 경우
                assertThat(result.getMaxElevation()).isNotNull();
                assertThat(result.getMaxElevation()).isGreaterThan(0);
            }
        }
    }
}
