package com.byeolnight.controller.admin;

import com.byeolnight.service.weather.AstronomyService;
import com.byeolnight.dto.admin.SchedulerStatusDto;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/scheduler")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
@Tag(name = "⏰ 관리자 - 스케줄러", description = "관리자 스케줄러 및 자동화 작업 관리 API")
public class AdminSchedulerController {

    private final SpaceNewsScheduler spaceNewsScheduler;
    private final DiscussionTopicScheduler discussionTopicScheduler;
    private final PostCleanupScheduler postCleanupScheduler;
    private final MessageCleanupService messageCleanupService;
    private final WithdrawnUserCleanupService withdrawnUserCleanupService;
    private final MessageRepository messageRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AstronomyService astronomyService;

    @PostMapping("/news/manual")
    @Operation(
        summary = "수동 뉴스 수집",
        description = "관리자가 수동으로 우주/과학 뉴스를 수집합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "뉴스 수집 성공"),
        @ApiResponse(responseCode = "500", description = "뉴스 수집 실패")
    })
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
    @Operation(
        summary = "수동 토론 주제 생성",
        description = "관리자가 수동으로 AI 기반 일일 토론 주제를 생성합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "토론 주제 생성 성공"),
        @ApiResponse(responseCode = "500", description = "토론 주제 생성 실패")
    })
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

    @PostMapping("/astronomy/manual")
    @Operation(
        summary = "수동 천체 이벤트 수집",
        description = "관리자가 수동으로 실제 천체 이벤트 데이터를 수집합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "천체 이벤트 수집 성공"),
        @ApiResponse(responseCode = "500", description = "천체 이벤트 수집 실패")
    })
    public CommonResponse<String> manualAstronomyCollection() {
        try {
            log.info("관리자 수동 천체 이벤트 수집 시작");
            astronomyService.fetchDailyAstronomyEvents();
            return CommonResponse.success("천체 이벤트 수집이 완료되었습니다.");
        } catch (Exception e) {
            log.error("수동 천체 이벤트 수집 실패", e);
            return CommonResponse.error("천체 이벤트 수집 실패: " + e.getMessage());
        }
    }


    @GetMapping("/status")
    @Operation(
        summary = "스케줄러 상태 조회",
        description = "스케줄러 작업들의 대상 데이터 개수를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "스케줄러 상태 조회 성공")
    public CommonResponse<SchedulerStatusDto> getSchedulerStatus() {
        try {
            int messagesToDelete = messageRepository.findMessagesEligibleForPermanentDeletion().size();
            
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            int postsToDelete = postRepository.findExpiredDeletedPosts(threshold).size();

            LocalDateTime twoYearsAgo = LocalDateTime.now().minusYears(2);
            int usersToCleanup = userRepository.findByWithdrawnAtBeforeAndStatusIn(
                twoYearsAgo, java.util.List.of(User.UserStatus.WITHDRAWN, User.UserStatus.BANNED)).size();
            
            SchedulerStatusDto status = SchedulerStatusDto.builder()
                .messagesToDelete(messagesToDelete)
                .postsToDelete(postsToDelete)
                .usersToCleanup(usersToCleanup)
                .build();
            
            return CommonResponse.success(status);
        } catch (Exception e) {
            log.error("스케줄러 상태 조회 실패", e);
            return CommonResponse.error("상태 조회 실패: " + e.getMessage());
        }
    }

    @PostMapping("/message-cleanup/manual")
    @Operation(
        summary = "수동 쪽지 정리",
        description = "관리자가 수동으로 오래된 쪽지들을 영구 삭제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "쪽지 정리 성공"),
        @ApiResponse(responseCode = "500", description = "쪽지 정리 실패")
    })
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
    @Operation(
        summary = "수동 게시글 정리",
        description = "관리자가 수동으로 삭제된 지 30일 지난 게시글들을 영구 삭제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "게시글 정리 성공"),
        @ApiResponse(responseCode = "500", description = "게시글 정리 실패")
    })
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
    @Operation(
        summary = "수동 탈퇴 회원 정리",
        description = "관리자가 수동으로 탈퇴한 지 2년 지난 회원 데이터를 완전 삭제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "탈퇴 회원 정리 성공"),
        @ApiResponse(responseCode = "500", description = "탈퇴 회원 정리 실패")
    })
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