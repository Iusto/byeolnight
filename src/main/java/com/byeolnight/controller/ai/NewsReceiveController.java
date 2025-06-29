package com.byeolnight.controller.ai;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.dto.ai.NewsDto;
import com.byeolnight.service.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/ai")
public class NewsReceiveController {

    private final PostService postService;
    private final UserRepository userRepository;

    @PostMapping("/news")
    public ResponseEntity<String> receiveNews(@RequestBody NewsDto news) {
        User systemUser = userRepository.findByEmail("system@byeolnight.com")
                .orElseThrow(() -> new RuntimeException("시스템 사용자를 찾을 수 없습니다."));

        postService.createNewsPost(news.getTitle(), news.getContent(), Post.Category.NEWS, systemUser);
        return ResponseEntity.ok("뉴스 등록 완료");
    }
}
