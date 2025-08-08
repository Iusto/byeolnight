package com.byeolnight.service;

import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.comment.Comment;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.file.FileRepository;
import com.byeolnight.service.file.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostCleanupScheduler {

    private final PostRepository postRepository;
    private final FileRepository fileRepository;
    private final S3Service s3Service;
    private final CommentRepository commentRepository;

    @Scheduled(cron = "0 0 8 * * *") // 매일 8시
    @Transactional
    public void cleanupExpiredPosts() {
        log.info("만료된 게시글 정리 작업 시작");

        try {
            // 30일 이전 시점 계산
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            
            // 만료된 삭제 게시글 조회
            List<Post> expiredPosts = postRepository.findExpiredDeletedPosts(threshold);
            
            if (expiredPosts.isEmpty()) {
                log.info("정리할 만료된 게시글이 없습니다.");
                return;
            }

            log.info("정리 대상 게시글 수: {}", expiredPosts.size());

            int deletedCount = 0;
            for (Post post : expiredPosts) {
                try {
                    // 관련 파일들 S3에서 삭제
                    deleteRelatedFiles(post);
                    
                    // 게시글 물리적 삭제
                    postRepository.delete(post);
                    deletedCount++;
                    
                    log.debug("게시글 물리적 삭제 완료: ID={}, 제목={}", post.getId(), post.getTitle());
                    
                } catch (Exception e) {
                    log.error("게시글 삭제 실패: ID={}, 오류={}", post.getId(), e.getMessage());
                }
            }

            log.info("만료된 게시글 정리 완료: {}/{} 개 삭제", deletedCount, expiredPosts.size());
            
            // 댓글 정리 작업
            cleanupExpiredComments(threshold);

        } catch (Exception e) {
            log.error("게시글 정리 작업 중 오류 발생", e);
        }
    }
    
    private void cleanupExpiredComments(LocalDateTime threshold) {
        try {
            List<Comment> expiredComments =
                commentRepository.findExpiredDeletedComments(threshold);
            
            if (expiredComments.isEmpty()) {
                log.info("정리할 만료된 댓글이 없습니다.");
                return;
            }
            
            log.info("정리 대상 댓글 수: {}", expiredComments.size());
            
            int deletedCount = 0;
            for (Comment comment : expiredComments) {
                try {
                    commentRepository.delete(comment);
                    deletedCount++;
                    log.debug("댓글 물리적 삭제 완료: ID={}", comment.getId());
                } catch (Exception e) {
                    log.error("댓글 삭제 실패: ID={}, 오류={}", comment.getId(), e.getMessage());
                }
            }
            
            log.info("만료된 댓글 정리 완료: {}/{} 개 삭제", deletedCount, expiredComments.size());
            
        } catch (Exception e) {
            log.error("댓글 정리 작업 중 오류 발생", e);
        }
    }

    private void deleteRelatedFiles(Post post) {
        try {
            // 게시글과 연관된 파일들 조회 및 S3에서 삭제
            fileRepository.findAllByPost(post).forEach(file -> {
                try {
                    s3Service.deleteObject(file.getS3Key());
                    log.debug("S3 파일 삭제: {}", file.getS3Key());
                } catch (Exception e) {
                    log.warn("S3 파일 삭제 실패: {}, 오류: {}", file.getS3Key(), e.getMessage());
                }
            });
            
            // 파일 레코드 삭제
            fileRepository.deleteAllByPost(post);
            
        } catch (Exception e) {
            log.warn("관련 파일 삭제 중 오류: 게시글 ID={}, 오류={}", post.getId(), e.getMessage());
        }
    }
}