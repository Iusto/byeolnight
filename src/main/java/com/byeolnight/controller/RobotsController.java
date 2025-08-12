package com.byeolnight.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "🤖 공개 API - SEO", description = "검색엔진 최적화 및 로봇 제어 API")
public class RobotsController {

    @Value("${site.base-url}")
    private String baseUrl;

    @Operation(summary = "robots.txt 제공", description = """
    검색엔진 크롤러를 위한 robots.txt 파일을 제공합니다.
    
    🚫 차단 경로:
    - /admin/ : 관리자 페이지
    - /api/admin/ : 관리자 API
    - /api/member/ : 회원 전용 API
    - /api/auth/ : 인증 API
    - /error : 오류 페이지
    
    🗺️ 사이트맵: /sitemap.xml
    """)
    @ApiResponse(responseCode = "200", description = "robots.txt 내용 반환")
    @PreAuthorize("permitAll()")
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getRobotsTxt() {
        return "# 네이버 크롤러 명시적 허용\n" +
               "User-agent: Yeti\n" +
               "Allow: /\n\n" +
               "# 구글 크롤러 허용\n" +
               "User-agent: Googlebot\n" +
               "Allow: /\n\n" +
               "# 모든 크롤러 기본 설정\n" +
               "User-agent: *\n" +
               "Allow: /\n" +
               "Disallow: /admin/\n" +
               "Disallow: /api/admin/\n" +
               "Disallow: /api/member/\n" +
               "Disallow: /api/auth/\n" +
               "Disallow: /error\n\n" +
               "# 크롤링 속도 제한 (1초 간격)\n" +
               "Crawl-delay: 1\n\n" +
               "# 사이트맵 위치\n" +
               "Sitemap: " + baseUrl + "/sitemap.xml";
    }
}