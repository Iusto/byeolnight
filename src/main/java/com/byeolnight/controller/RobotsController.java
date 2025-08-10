package com.byeolnight.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RobotsController {

    @Value("${site.base-url}")
    private String baseUrl;

    @PreAuthorize("permitAll()")
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getRobotsTxt() {
        return "User-agent: *\n" +
               "Allow: /\n" +
               "Disallow: /admin/\n" +
               "Disallow: /api/admin/\n" +
               "Disallow: /api/member/\n" +
               "Disallow: /api/auth/\n" +
               "Disallow: /error\n\n" +
               "# 사이트맵 위치\n" +
               "Sitemap: " + baseUrl + "/sitemap.xml";
    }
}