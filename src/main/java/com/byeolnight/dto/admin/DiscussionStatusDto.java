package com.byeolnight.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionStatusDto {
    private Boolean todayTopicExists;
    private String todayTopicTitle;
    private Long totalDiscussionPosts;
    private LocalDateTime lastUpdated;
}
