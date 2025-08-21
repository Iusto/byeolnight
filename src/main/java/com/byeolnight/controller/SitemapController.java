package com.byeolnight.controller;

import com.byeolnight.entity.post.Post;
import com.byeolnight.repository.post.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;

@RestController
public class SitemapController {

    private final PostRepository postRepository;
    
    @Value("${site.base-url:https://byeolnight.com}")
    private String baseUrl; // application.properties에서 설정 가능
    
    private static final int PAGE_SIZE = 1000; // 한 페이지당 URL 수

    @Autowired
    public SitemapController(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * 메인 사이트맵 인덱스 - 여러 사이트맵을 관리
     */
    @PreAuthorize("permitAll()")
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getSitemapIndex() {
        StringBuilder sitemap = new StringBuilder();
        
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        // 메인 페이지와 주요 페이지들을 위한 사이트맵
        sitemap.append("  <sitemap>\n");
        sitemap.append("    <loc>").append(baseUrl).append("/sitemap-main.xml</loc>\n");
        sitemap.append("  </sitemap>\n");
        
        // 게시글 페이지들을 위한 사이트맵 (페이징 처리)
        long totalPosts = postRepository.count();
        int totalPages = (int) Math.ceil((double) totalPosts / PAGE_SIZE);
        
        for (int i = 0; i < totalPages; i++) {
            sitemap.append("  <sitemap>\n");
            sitemap.append("    <loc>").append(baseUrl).append("/sitemap-posts-").append(i).append(".xml</loc>\n");
            sitemap.append("  </sitemap>\n");
        }
        
        sitemap.append("</sitemapindex>");
        return sitemap.toString();
    }
    
    /**
     * 메인 페이지와 주요 페이지들을 위한 사이트맵
     */
    @PreAuthorize("permitAll()")
    @GetMapping(value = "/sitemap-main.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getMainSitemap() {
        StringBuilder sitemap = new StringBuilder();
        
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        // 메인 페이지
        sitemap.append("  <url>\n");
        sitemap.append("    <loc>").append(baseUrl).append("</loc>\n");
        sitemap.append("    <changefreq>daily</changefreq>\n");
        sitemap.append("    <priority>1.0</priority>\n");
        sitemap.append("  </url>\n");
        
        // 주요 페이지들
        addUrl(sitemap, "/news", "weekly", "0.8");
        addUrl(sitemap, "/community", "daily", "0.9");
        addUrl(sitemap, "/cinema", "weekly", "0.8");
        addUrl(sitemap, "/discussion", "daily", "0.8");
        addUrl(sitemap, "/shop", "monthly", "0.7");
        
        sitemap.append("</urlset>");
        return sitemap.toString();
    }
    
    /**
     * 게시글 페이지들을 위한 사이트맵 (페이징 처리)
     */
    @PreAuthorize("permitAll()")
    @GetMapping(value = "/sitemap-posts-{page}.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getPostsSitemap(@PathVariable int page) {
        StringBuilder sitemap = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        try {
            Pageable pageable = PageRequest.of(page, PAGE_SIZE);
            Page<Post> postPage = postRepository.findByBlindedFalseAndIsDeletedFalse(pageable);
            
            sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
            
            // 게시글이 있는 경우에만 URL 추가
            if (postPage.hasContent()) {
                for (Post post : postPage.getContent()) {
                    if (post.getId() != null && post.getUpdatedAt() != null) {
                        sitemap.append("  <url>\n");
                        sitemap.append("    <loc>").append(baseUrl).append("/posts/").append(post.getId()).append("</loc>\n");
                        sitemap.append("    <lastmod>").append(post.getUpdatedAt().format(formatter)).append("</lastmod>\n");
                        sitemap.append("    <changefreq>monthly</changefreq>\n");
                        sitemap.append("    <priority>0.6</priority>\n");
                        sitemap.append("  </url>\n");
                    }
                }
            }
            
            sitemap.append("</urlset>");
        } catch (Exception e) {
            // 오류 발생 시 빈 사이트맵 반환
            sitemap = new StringBuilder();
            sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
            sitemap.append("</urlset>");
        }
        
        return sitemap.toString();
    }

    private void addUrl(StringBuilder sitemap, String path, String changeFreq, String priority) {
        sitemap.append("  <url>\n");
        sitemap.append("    <loc>").append(baseUrl).append(path).append("</loc>\n");
        sitemap.append("    <changefreq>").append(changeFreq).append("</changefreq>\n");
        sitemap.append("    <priority>").append(priority).append("</priority>\n");
        sitemap.append("  </url>\n");
    }
}