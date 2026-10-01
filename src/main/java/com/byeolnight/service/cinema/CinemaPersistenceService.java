package com.byeolnight.service.cinema;

import com.byeolnight.dto.cinema.CinemaVideoData;
import com.byeolnight.entity.Cinema;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.CinemaRepository;
import com.byeolnight.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AI와 외부 API 처리가 끝난 시네마 결과를 하나의 짧은 트랜잭션으로 저장한다. */
@Service
@RequiredArgsConstructor
public class CinemaPersistenceService {

    private final CinemaRepository cinemaRepository;
    private final PostRepository postRepository;

    @Transactional
    public void save(CinemaVideoData data, User writer) {
        cinemaRepository.save(toCinema(data));
        postRepository.save(toPost(data, writer));
        // DB 제약조건 오류를 이 트랜잭션 안에서 즉시 감지한다.
        postRepository.flush();
    }

    private Cinema toCinema(CinemaVideoData data) {
        return Cinema.builder()
                .title(data.title())
                .description(data.description())
                .videoId(data.videoId())
                .videoUrl(data.videoUrl())
                .channelTitle(data.channelTitle())
                .publishedAt(data.publishedAt())
                .summary(data.summary())
                .hashtags(data.hashtags())
                .build();
    }

    private Post toPost(CinemaVideoData data, User writer) {
        return Post.builder()
                .title(data.title())
                .content(data.content())
                .category(Post.Category.STARLIGHT_CINEMA)
                .writer(writer)
                .build();
    }
}
