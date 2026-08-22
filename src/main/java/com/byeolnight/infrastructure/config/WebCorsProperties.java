package com.byeolnight.infrastructure.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * HTTP와 WebSocket이 함께 사용하는 신뢰 프론트엔드 Origin 목록이다.
 * 환경별 주소가 추가되면 개별 설정 클래스가 아니라 이 프로퍼티만 변경한다.
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.web.cors")
public class WebCorsProperties {

    @NotEmpty
    private List<String> allowedOrigins;
}
