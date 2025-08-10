package com.byeolnight.service.comment;

import com.byeolnight.entity.comment.Comment;
import com.byeolnight.entity.comment.CommentReport;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.comment.CommentReportRepository;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.dto.comment.CommentReportDto;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.user.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentReportService {

    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final CertificateService certificateService;
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

        // 해당 댓글의 모든 신고 조회
        List<CommentReport> allReportsForComment = commentReportRepository.findByComment(report.getComment());
        
        if (approve) {
            // 댓글 블라인드 처리
            report.getComment().blind();
            
            // 댓글 작성자 페널티 적용
            pointService.applyPenalty(report.getComment().getWriter(), "댓글 신고 승인", reportId.toString());

            // 모든 미검토 신고를 승인 처리하고 신고자에게 포인트 지급
            for (CommentReport commentReport : allReportsForComment) {
                if (!commentReport.isReviewed()) {
                    commentReport.approve(admin);
                    
                    // 각 신고자에게 포인트 지급
                    pointService.awardValidReportPoints(commentReport.getUser(), commentReport.getComment().getId().toString());
                }
            }
        } else {
            // 모든 미검토 신고를 거부 처리하고 허위 신고자 페널티 적용
            int rejectedCount = 0;
            for (CommentReport commentReport : allReportsForComment) {
                if (!commentReport.isReviewed()) {
                    commentReport.reject(admin, rejectReason);
                    rejectedCount++;
                    
                    // 허위 신고 페널티 적용
                    pointService.applyPenalty(commentReport.getUser(), "허위 신고", commentReport.getId().toString());
                }
            }

            // 거부된 신고 수만큼 댓글 신고수 한번에 감소
            if (rejectedCount > 0) {
                report.getComment().decreaseReportCountBy(rejectedCount);
            }
        }
        
        // 변경된 신고 내역 저장 (한 번에 저장)
        commentReportRepository.saveAll(allReportsForComment);
    }
    
    /**
     * 관리자: 대기 중인 댓글 신고 목록
     */
    @Transactional(readOnly = true)
    public List<CommentReport> getPendingCommentReports() {
        return commentReportRepository.findPendingReports();
    }
    
    /**
     * 관리자: 신고된 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CommentReportDto> getReportedComments() {
        // 신고 수가 1 이상인 댓글 조회
        List<Comment> reportedComments = commentRepository.findByReportCountGreaterThanOrderByReportCountDesc(0);
        
        // 댓글별 신고 내역 매핑
        Map<Comment, List<CommentReport>> commentReportsMap = new HashMap<>();
        
        for (Comment comment : reportedComments) {
            List<CommentReport> reports = commentReportRepository.findByComment(comment);
            commentReportsMap.put(comment, reports);
        }
        
        // DTO 변환
        return CommentReportDto.fromCommentReports(commentReportsMap);
    }
}