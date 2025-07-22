package com.byeolnight.service.comment;

import com.byeolnight.domain.entity.comment.Comment;
import com.byeolnight.domain.entity.comment.CommentReport;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.comment.CommentReportRepository;
import com.byeolnight.domain.repository.comment.CommentRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.service.user.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentReportService {

    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final UserRepository userRepository;
    private final PointService pointService;

    /**
     * 댓글 신고 - ID 기반 메서드
     */
    @Transactional
    public void reportCommentById(Long commentId, Long reporterId, String reason, String description) {
        if (reporterId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        
        try {
            // 신고자 존재 여부 먼저 확인
            if (!userRepository.existsById(reporterId)) {
                throw new NotFoundException("신고자 정보가 유효하지 않습니다. 사용자 ID: " + reporterId);
            }
            
            // 신고자 조회
            User reporter = userRepository.findById(reporterId)
                    .orElseThrow(() -> new NotFoundException("신고자 정보가 유효하지 않습니다."));
            
            // 댓글 조회
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new NotFoundException("댓글이 존재하지 않습니다."));
            
            // 중복 신고 방지
            if (commentReportRepository.existsByCommentAndUser(comment, reporter)) {
                throw new IllegalArgumentException("이미 신고한 댓글입니다.");
            }
            
            // 신고자 정보 로깅
            System.out.println("User ID: " + reporterId + ", Comment ID: " + commentId);
            
            // 신고 객체 생성 및 저장
            CommentReport report = CommentReport.of(reporter, comment, reason, description);
            
            // 신고 저장
            commentReportRepository.save(report);
            
            // 신고 수 증가
            comment.increaseReportCount();
            commentRepository.save(comment);
            
            // 신고 수가 5개 이상이면 자동 블라인드
            if (comment.getReportCount() >= 5) {
                comment.blind();
                commentRepository.save(comment);
            }
            
            // 성공 로그
            System.out.println("댓글 신고 처리 완료 - 신고 수: " + comment.getReportCount());
        } catch (Exception e) {
            e.printStackTrace(); // 상세 오류 로그 출력
            throw new RuntimeException(e.getMessage());
        }
    }
    
    /**
     * 관리자: 댓글 신고 처리
     */
    @Transactional
    public void processCommentReport(Long reportId, User admin, boolean approve, String rejectReason) {
        CommentReport report = commentReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("신고를 찾을 수 없습니다."));
        
        if (approve) {
            report.approve(admin);
            report.getComment().blind();
            // 페널티 적용
            pointService.applyPenalty(report.getComment().getWriter(), "댓글 신고 승인", reportId.toString());
        } else {
            report.reject(admin, rejectReason);
            report.getComment().decreaseReportCount();
        }
    }
    
    /**
     * 관리자: 대기 중인 댓글 신고 목록
     */
    @Transactional(readOnly = true)
    public List<CommentReport> getPendingCommentReports() {
        return commentReportRepository.findPendingReports();
    }
}