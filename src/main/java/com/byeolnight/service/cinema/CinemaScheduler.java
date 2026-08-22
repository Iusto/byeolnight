package com.byeolnight.service.cinema;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 정해진 시각에 동일한 시네마 수집 유스케이스를 호출하는 스케줄 진입점이다. */
@Component
@RequiredArgsConstructor
public class CinemaScheduler {

    private final CinemaService cinemaService;
    private final UserRepository userRepository;

    @Value("${app.system.users.newsbot.email:newsbot@byeolnight.com}")
    private String newsbotEmail;

    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Seoul")
    public void createDailyCinemaPost() {
        cinemaService.collectAndSaveSpaceVideo(getSystemUser());
    }

    @Scheduled(cron = "0 15 20 * * *", zone = "Asia/Seoul")
    public void retryDailyCinemaPost() {
        cinemaService.collectAndSaveSpaceVideo(getSystemUser());
    }

    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
    public void finalRetryDailyCinemaPost() {
        cinemaService.collectAndSaveSpaceVideo(getSystemUser());
    }

    private User getSystemUser() {
        return userRepository.findByEmail(newsbotEmail)
                .orElseThrow(() -> new IllegalStateException("시스템 사용자를 찾을 수 없습니다."));
    }
}
