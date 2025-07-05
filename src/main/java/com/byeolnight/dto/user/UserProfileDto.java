package com.byeolnight.dto.user;

import com.byeolnight.dto.certificate.CertificateDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String nickname;
    private String equippedIconUrl;
    private List<CertificateDto.Response> certificates; // 최신 4개
    private int totalIconCount; // 수집한 아이콘 개수
    private int postCount; // 작성한 글 수
    private int commentCount; // 작성한 댓글 수
    private int attendanceCount; // 출석 수
    private LocalDateTime joinedAt; // 가입일
}