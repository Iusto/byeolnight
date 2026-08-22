package com.byeolnight.service.admin;

import com.byeolnight.dto.admin.AdminDashboardReportStatsDto;
import com.byeolnight.repository.comment.CommentReportRepository;
import com.byeolnight.repository.post.PostReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private PostReportRepository postReportRepository;

    @Mock
    private CommentReportRepository commentReportRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    @DisplayName("검토가 끝난 신고를 제외하고 미처리 신고만 집계한다")
    void getPendingReportStatsCountsOnlyUnreviewedReports() {
        // given
        when(postReportRepository.countByReviewedFalse()).thenReturn(2L);
        when(commentReportRepository.countByReviewedFalse()).thenReturn(3L);

        // when
        AdminDashboardReportStatsDto stats = adminDashboardService.getPendingReportStats();

        // then
        assertThat(stats.getPendingPostReports()).isEqualTo(2L);
        assertThat(stats.getPendingCommentReports()).isEqualTo(3L);
    }
}
