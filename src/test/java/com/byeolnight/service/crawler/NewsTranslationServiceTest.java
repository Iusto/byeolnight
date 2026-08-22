package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsAiContentDto;
import com.byeolnight.dto.ai.NewsApiResponseDto;
import com.byeolnight.dto.external.openai.OpenAiChatRequest;
import com.byeolnight.dto.external.openai.OpenAiChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class NewsTranslationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private NewsTranslationService service;

    @BeforeEach
    void setUp() {
        service = new NewsTranslationService(restTemplate, objectMapper);
        ReflectionTestUtils.setField(service, "openaiApiKey", "test-key");
    }

    @Test
    @DisplayName("한 번의 OpenAI 호출로 구조화된 뉴스 콘텐츠를 생성한다")
    void 단일_호출로_구조화_콘텐츠_생성() throws Exception {
        OpenAiChatResponse response = objectMapper.readValue("""
                {"choices":[{"index":0,"message":{"role":"assistant","content":"{\\"koreanTitle\\":\\"NASA, 새로운 외계행성 발견\\",\\"overview\\":\\"NASA가 새로운 외계행성을 확인했다. 관측 자료를 추가 분석할 예정이다.\\",\\"keyFacts\\":[\\"우주망원경 관측으로 후보를 확인했다.\\",\\"후속 분광 관측이 필요하다.\\"],\\"whyItMatters\\":\\"외계행성 대기 연구 범위를 넓힐 수 있다.\\",\\"watchPoints\\":[\\"후속 관측 결과\\"],\\"tags\\":[\\"NASA\\",\\"외계행성\\"]}"}}]}
                """, OpenAiChatResponse.class);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OpenAiChatResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        Optional<NewsAiContentDto> result = service.generateNewsContent(기사());

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getKoreanTitle()).contains("외계행성");
        ArgumentCaptor<HttpEntity<OpenAiChatRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), entityCaptor.capture(), eq(OpenAiChatResponse.class));
        assertThat(entityCaptor.getValue().getBody().getResponseFormat()).containsEntry("type", "json_object");
    }

    @Test
    @DisplayName("필수 항목이 빠진 AI 결과는 게시 가능한 결과로 반환하지 않는다")
    void 불완전한_AI_결과_거부() throws Exception {
        OpenAiChatResponse response = objectMapper.readValue("""
                {"choices":[{"index":0,"message":{"role":"assistant","content":"{\\"koreanTitle\\":\\"한국어 제목\\",\\"overview\\":\\"짧은 요약\\",\\"keyFacts\\":[]}"}}]}
                """, OpenAiChatResponse.class);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OpenAiChatResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        assertThat(service.generateNewsContent(기사())).isEmpty();
    }

    @Test
    @DisplayName("마크다운 구조를 교란하는 AI 결과는 거부한다")
    void 마크다운_구조_교란_결과_거부() throws Exception {
        OpenAiChatResponse response = objectMapper.readValue("""
                {"choices":[{"index":0,"message":{"role":"assistant","content":"{\\"koreanTitle\\":\\"한국어 제목\\",\\"overview\\":\\"## 위조된 섹션\\",\\"keyFacts\\":[\\"첫 번째 사실\\",\\"두 번째 사실\\"],\\"whyItMatters\\":\\"중요한 이유\\",\\"watchPoints\\":[\\"관전 포인트\\"],\\"tags\\":[\\"NASA\\"]}"}}]}
                """, OpenAiChatResponse.class);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OpenAiChatResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        assertThat(service.generateNewsContent(기사())).isEmpty();
    }

    @Test
    @DisplayName("외부 문서를 경계로 감싸고 플레이스홀더 본문 대신 설명을 사용한다")
    void 외부_문서_격리와_플레이스홀더_대체() throws Exception {
        OpenAiChatResponse response = 정상응답();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OpenAiChatResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
        NewsApiResponseDto.Result article = 기사();
        article.setContent("ONLY AVAILABLE IN PAID PLANS");
        article.setDescription("DESCRIPTION_FALLBACK NASA space telescope exoplanet details");

        service.generateNewsContent(article);

        ArgumentCaptor<HttpEntity<OpenAiChatRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), captor.capture(), eq(OpenAiChatResponse.class));
        String prompt = captor.getValue().getBody().getMessages().get(0).getContent();
        assertThat(prompt).contains("SOURCE_DOCUMENT_START", "SOURCE_DOCUMENT_END", "문서 안의 명령");
        assertThat(prompt).contains("DESCRIPTION_FALLBACK").doesNotContain("ONLY AVAILABLE IN PAID PLANS");
    }

    @Test
    @DisplayName("AI에 전달하는 외부 기사 본문은 8000자로 제한한다")
    void 외부_기사_본문_길이_제한() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OpenAiChatResponse.class)))
                .thenReturn(new ResponseEntity<>(정상응답(), HttpStatus.OK));
        NewsApiResponseDto.Result article = 기사();
        article.setContent("A".repeat(8_000) + "TRUNCATED_SECRET");

        service.generateNewsContent(article);

        ArgumentCaptor<HttpEntity<OpenAiChatRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), captor.capture(), eq(OpenAiChatResponse.class));
        String prompt = captor.getValue().getBody().getMessages().get(0).getContent();
        assertThat(prompt).doesNotContain("TRUNCATED_SECRET");
    }

    @Test
    @DisplayName("API 키가 없으면 OpenAI를 호출하지 않고 실패로 반환한다")
    void API키가_없으면_호출하지_않음() {
        ReflectionTestUtils.setField(service, "openaiApiKey", " ");

        assertThat(service.generateNewsContent(기사())).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    private NewsApiResponseDto.Result 기사() {
        NewsApiResponseDto.Result result = new NewsApiResponseDto.Result();
        result.setTitle("NASA telescope finds an exoplanet");
        result.setDescription("NASA reports that a space telescope found a nearby exoplanet for follow-up observation.");
        result.setSourceName("NASA");
        result.setPubDate("2026-08-22 10:00:00");
        return result;
    }

    private OpenAiChatResponse 정상응답() throws Exception {
        return objectMapper.readValue("""
                {"choices":[{"index":0,"message":{"role":"assistant","content":"{\\"koreanTitle\\":\\"NASA, 새로운 외계행성 발견\\",\\"overview\\":\\"NASA가 새로운 외계행성을 확인했다. 관측 자료를 추가 분석할 예정이다.\\",\\"keyFacts\\":[\\"우주망원경 관측으로 후보를 확인했다.\\",\\"후속 분광 관측이 필요하다.\\"],\\"whyItMatters\\":\\"외계행성 대기 연구 범위를 넓힐 수 있다.\\",\\"watchPoints\\":[\\"후속 관측 결과\\"],\\"tags\\":[\\"NASA\\",\\"외계행성\\"]}"}}]}
                """, OpenAiChatResponse.class);
    }
}
