package com.byeolnight.service.admin;

import com.byeolnight.dto.admin.AdminDashboardReportStatsDto;
import com.byeolnight.repository.comment.CommentReportRepository;
import com.byeolnight.repository.post.PostReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final PostReportRepository postReportRepository;
    private final CommentReportRepository commentReportRepository;

    @Transactional(readOnly = true)
    public AdminDashboardReportStatsDto getPendingReportStats() {
        return new AdminDashboardReportStatsDto(
                postReportRepository.countByReviewedFalse(),
                commentReportRepository.countByReviewedFalse()
        );
    }
}
