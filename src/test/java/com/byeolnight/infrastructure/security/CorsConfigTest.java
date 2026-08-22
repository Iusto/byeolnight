package com.byeolnight.infrastructure.security;

import com.byeolnight.infrastructure.config.WebCorsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    @DisplayName("운영 및 로컬 프론트엔드만 자격 증명 CORS 요청을 허용한다")
    void allowsOnlyTrustedFrontendOrigins() {
        WebCorsProperties properties = new WebCorsProperties();
        properties.setAllowedOrigins(List.of("http://localhost:5173", "https://byeolnight.com"));
        CorsConfigurationSource source = new CorsConfig(properties).corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/posts");
        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.checkOrigin("https://byeolnight.com")).isEqualTo("https://byeolnight.com");
        assertThat(cors.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(cors.checkOrigin("https://malicious.example")).isNull();
        assertThat(cors.getAllowedMethods()).contains("PATCH", "OPTIONS");
        assertThat(cors.getAllowCredentials()).isTrue();
    }
}
