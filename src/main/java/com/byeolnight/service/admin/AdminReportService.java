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

        // 게시글별로 그룹화
        Map<Post, List<PostReport>> groupedReports = reports.stream()
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
                                    .reportedAt(report.getCreatedAt())
                                    .build())
                            .collect(Collectors.toList());

                    return ReportedPostDetailDto.builder()
                            .postId(post.getId())
                            .title(post.getTitle())
                            .content(post.getContent().length() > 100 ? 
                                    post.getContent().substring(0, 100) + "..." : post.getContent())
                            .writer(post.getWriter().getNickname())
                            .category(post.getCategory().name())
                            .createdAt(post.getCreatedAt())
                            .blinded(post.isBlinded())
                            .totalReportCount(postReports.size())
                            .reports(reportDetails)
                            .build();
                })
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
     * 신고 승인 (유효한 신고로 인정)
     */
    @Transactional
    public void approveReport(Long reportId) {
        PostReport report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다."));
        
        // 신고자에게 포인트 지급
        pointService.awardValidReportPoints(report.getUser(), reportId.toString());
    }

    /**
     * 신고 거부 (허위 신고로 판정)
     */
    @Transactional
    public void rejectReport(Long reportId) {
        PostReport report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다."));
        
        // 허위 신고 페널티 적용
        pointService.applyPenalty(report.getUser(), "허위 신고", reportId.toString());
    }
}