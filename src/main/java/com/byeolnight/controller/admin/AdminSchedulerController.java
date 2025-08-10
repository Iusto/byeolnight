package com.byeolnight.controller.admin;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.crawler.SpaceNewsScheduler;
import com.byeolnight.service.discussion.DiscussionTopicScheduler;
import com.byeolnight.service.PostCleanupScheduler;
import com.byeolnight.service.message.MessageCleanupService;
import com.byeolnight.service.user.WithdrawnUserCleanupService;
import com.byeolnight.repository.MessageRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.entity.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/scheduler")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminSchedulerController {

    private final SpaceNewsScheduler spaceNewsScheduler;
    private final DiscussionTopicScheduler discussionTopicScheduler;
    private final PostCleanupScheduler postCleanupScheduler;
    private final MessageCleanupService messageCleanupService;
    private final WithdrawnUserCleanupService withdrawnUserCleanupService;
    private final MessageRepository messageRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @PostMapping("/news/manual")
    public CommonResponse<String> manualNewsCollection() {
        try {
            log.info("관리자 수동 뉴스 수집 시작");
            spaceNewsScheduler.scheduleSpaceNewsCollection();
            return CommonResponse.success("뉴스 수집이 완료되었습니다.");
        } catch (Exception e) {
            log.error("수동 뉴스 수집 실패", e);
            return CommonResponse.error("뉴스 수집 실패: " + e.getMessage());
        }
    }

    @PostMapping("/discussion/manual")
    public CommonResponse<String> manualDiscussionGeneration() {
        try {
            log.info("관리자 수동 토론 주제 생성 시작");
            discussionTopicScheduler.generateDailyDiscussionTopic();
            return CommonResponse.success("토론 주제 생성이 완료되었습니다.");
        } catch (Exception e) {
            log.error("수동 토론 주제 생성 실패", e);
            return CommonResponse.error("토론 주제 생성 실패: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public CommonResponse<Map<String, Object>> getSchedulerStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // 쪽지 정리 대상 개수
            int messagesToDelete = messageRepository.findMessagesEligibleForPermanentDeletion().size();
            status.put("messagesToDelete", messagesToDelete);
            
            // 게시글 정리 대상 개수
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            int postsToDelete = postRepository.findExpiredDeletedPosts(threshold).size();
            status.put("postsToDelete", postsToDelete);
            
            // 탈퇴 회원 정리 대상 개수
            LocalDateTime fiveYearsAgo = LocalDateTime.now().minusYears(5);
            int usersToCleanup = userRepository.findByStatusAndWithdrawnAtBefore(
                User.UserStatus.WITHDRAWN, fiveYearsAgo).size();
            status.put("usersToCleanup", usersToCleanup);
            
            return CommonResponse.success(status);
        } catch (Exception e) {
            log.error("스케줄러 상태 조회 실패", e);
            return CommonResponse.error("상태 조회 실패: " + e.getMessage());
        }
    }

    @PostMapping("/message-cleanup/manual")
    public CommonResponse<String> manualMessageCleanup() {
        try {
            log.info("관리자 수동 쪽지 정리 시작");
            messageCleanupService.cleanupOldMessages();
            return CommonResponse.success("쪽지 정리가 완료되었습니다.");
        } catch (Exception e) {
            log.error("수동 쪽지 정리 실패", e);
            return CommonResponse.error("쪽지 정리 실패: " + e.getMessage());
        }
    }

    @PostMapping("/post-cleanup/manual")
    public CommonResponse<String> manualPostCleanup() {
        try {
            log.info("관리자 수동 게시글 정리 시작");
            postCleanupScheduler.cleanupExpiredPosts();
            return CommonResponse.success("게시글 정리가 완료되었습니다.");
        } catch (Exception e) {
            log.error("수동 게시글 정리 실패", e);
            return CommonResponse.error("게시글 정리 실패: " + e.getMessage());
        }
    }

    @PostMapping("/user-cleanup/manual")
    public CommonResponse<String> manualUserCleanup() {
        try {
            log.info("관리자 수동 탈퇴 회원 정리 시작");
            withdrawnUserCleanupService.cleanupWithdrawnUsers();
            return CommonResponse.success("탈퇴 회원 정리가 완료되었습니다.");
        } catch (Exception e) {
            log.error("수동 탈퇴 회원 정리 실패", e);
            return CommonResponse.error("탈퇴 회원 정리 실패: " + e.getMessage());
        }
    }
}