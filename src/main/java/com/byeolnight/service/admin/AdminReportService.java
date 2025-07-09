package com.byeolnight.service.admin;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.PostReport;
import com.byeolnight.domain.repository.post.PostReportRepository;
import com.byeolnight.dto.admin.ReportedPostDetailDto;
import com.byeolnight.service.user.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    private final PostReportRepository postReportRepository;
    private final PointService pointService;
    private final com.byeolnight.domain.repository.user.UserRepository userRepository;
    private final com.byeolnight.service.certificate.CertificateService certificateService;

    /**
     * 신고된 게시글 목록 조회
     */
    public Page<ReportedPostDetailDto> getReportedPosts(String search, String searchType, Pageable pageable) {
        List<PostReport> reports;
        
        if (search != null && !search.trim().isEmpty()) {
            switch (searchType) {
                case "title" -> reports = postReportRepository.findByPostTitleContainingIgnoreCase(search.trim());
                case "writer" -> reports = postReportRepository.findByPostWriterNicknameContainingIgnoreCase(search.trim());
                default -> reports = postReportRepository.findAllByOrderByCreatedAtDesc();
            }
        } else {
            reports = postReportRepository.findAllByOrderByCreatedAtDesc();
        }

        // 게시글별로 그룹화 (삭제된 게시글 제외)
        Map<Post, List<PostReport>> groupedReports = reports.stream()
                .filter(report -> {
                    try {
                        // 게시글이 삭제되었는지 확인
                        Post post = report.getPost();
                        return post != null && !post.isDeleted();
                    } catch (Exception e) {
                        // 엔티티를 찾을 수 없는 경우 (삭제된 게시글)
                        return false;
                    }
                })
                .collect(Collectors.groupingBy(PostReport::getPost));

        List<ReportedPostDetailDto> reportedPosts = groupedReports.entrySet().stream()
                .map(entry -> {
                    Post post = entry.getKey();
                    List<PostReport> postReports = entry.getValue();
                    
                    List<ReportedPostDetailDto.ReportDetail> reportDetails = postReports.stream()
                            .map(report -> ReportedPostDetailDto.ReportDetail.builder()
                                    .reportId(report.getId())
                                    .reporterNickname(report.getUser().getNickname())
                                    .reason(report.getReason())
                                    .description(report.getDescription())
                                    .reviewed(report.isReviewed())
                                    .accepted(report.isAccepted())
                                    .reportedAt(report.getCreatedAt())
                                    .build())
                            .collect(Collectors.toList());

                    try {
                        List<String> reasons = postReports.stream()
                                .map(PostReport::getReason)
                                .collect(Collectors.toList());
                        
                        return ReportedPostDetailDto.builder()
                                .id(post.getId())
                                .postId(post.getId())
                                .title(post.getTitle())
                                .content(post.getContent().length() > 100 ? 
                                        post.getContent().substring(0, 100) + "..." : post.getContent())
                                .writer(post.getWriter() != null ? post.getWriter().getNickname() : "알 수 없음")
                                .category(post.getCategory().name())
                                .createdAt(post.getCreatedAt())
                                .blinded(post.isBlinded())
                                .reportCount(postReports.size())
                                .totalReportCount(postReports.size())
                                .reportReasons(reasons)
                                .reportDetails(reportDetails)
                                .build();
                    } catch (Exception e) {
                        // 삭제된 게시글에 대한 예외 처리
                        return null;
                    }
                })
                .filter(dto -> dto != null) // null 제거
                .sorted((a, b) -> b.getTotalReportCount() - a.getTotalReportCount()) // 신고 수 많은 순
                .collect(Collectors.toList());

        // 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), reportedPosts.size());
        List<ReportedPostDetailDto> pagedList = reportedPosts.subList(start, end);

        return new PageImpl<>(pagedList, pageable, reportedPosts.size());
    }

    /**
     * 신고 사유별 통계
     */
    public Map<String, Long> getReportStatsByReason() {
        List<PostReport> allReports = postReportRepository.findAll();
        return allReports.stream()
                .collect(Collectors.groupingBy(PostReport::getReason, Collectors.counting()));
    }

    /**
     * 신고 승인 (유효한 신고로 인정) - 해당 게시글의 모든 신고자에게 포인트 지급
     */
    @Transactional
    public void approveReport(Long reportId, Long adminId) {
        PostReport report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다."));
        
        com.byeolnight.domain.entity.user.User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));
        
        // 해당 게시글의 모든 신고 조회
        List<PostReport> allReportsForPost = postReportRepository.findByPost(report.getPost());
        
        // 모든 신고를 검토 완료로 처리하고 승인
        for (PostReport postReport : allReportsForPost) {
            if (!postReport.isReviewed()) {
                postReport.approve(admin);
                postReportRepository.save(postReport);
                
                // 각 신고자에게 포인트 지급
                pointService.awardValidReportPoints(postReport.getUser(), postReport.getPost().getId().toString());
                
                // 신고자에게 인증서 발급 체크
                try {
                    certificateService.checkAndIssueCertificates(postReport.getUser(), 
                        com.byeolnight.service.certificate.CertificateService.CertificateCheckType.REPORT_APPROVED);
                } catch (Exception e) {
                    System.err.println("신고 승인 인증서 발급 실패: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 신고 거부 (허위 신고로 판정) - 해당 게시글의 모든 신고를 거부 처리
     */
    @Transactional
    public void rejectReport(Long reportId, Long adminId, String reason) {
        PostReport report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다."));
        
        com.byeolnight.domain.entity.user.User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));
        
        Post post = report.getPost();
        
        // 해당 게시글의 모든 신고 조회
        List<PostReport> allReportsForPost = postReportRepository.findByPost(post);
        
        // 모든 신고를 검토 완료로 처리하고 거부
        int rejectedCount = 0;
        for (PostReport postReport : allReportsForPost) {
            if (!postReport.isReviewed()) {
                postReport.reject(admin, reason);
                postReportRepository.save(postReport);
                rejectedCount++;
                
                // 허위 신고 페널티 적용
                pointService.applyPenalty(postReport.getUser(), "허위 신고", postReport.getId().toString());
            }
        }
        
        // 거부된 신고 수만큼 게시글의 신고수 감소
        for (int i = 0; i < rejectedCount; i++) {
            post.decreaseReportCount();
        }
    }
}