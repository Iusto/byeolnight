package com.byeolnight.dto.suggestion;

import com.byeolnight.entity.Suggestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class SuggestionDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String title;
        private String content;
        private Suggestion.SuggestionCategory category;
        private Suggestion.SuggestionStatus status;
        private Long authorId;
        private String authorNickname;
        private Boolean isPublic;
        private String adminResponse;
        private LocalDateTime adminResponseAt;
        private String adminNickname;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public void setContent(String content) {
            this.content = content;
        }

        public static Response from(Suggestion suggestion) {
            return Response.builder()
                    .id(suggestion.getId())
                    .title(suggestion.getTitle())
                    .content(suggestion.getContent())
                    .category(suggestion.getCategory())
                    .status(suggestion.getStatus())
                    .authorId(suggestion.getAuthor().getId())
                    .authorNickname(suggestion.getAuthor().getNickname())
                    .isPublic(suggestion.getIsPublic())
                    .adminResponse(suggestion.getAdminResponse())
                    .adminResponseAt(suggestion.getAdminResponseAt())
                    .adminNickname(suggestion.getAdmin() != null ? suggestion.getAdmin().getNickname() : null)
                    .createdAt(suggestion.getCreatedAt())
                    .updatedAt(suggestion.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
        private String title;
        
        @NotBlank(message = "내용은 필수입니다.")
        private String content;
        
        @NotNull(message = "카테고리는 필수입니다.")
        private Suggestion.SuggestionCategory category;
        
        private Boolean isPublic; // 기본값: true
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
        private String title;
        
        @NotBlank(message = "내용은 필수입니다.")
        private String content;
        
        @NotNull(message = "카테고리는 필수입니다.")
        private Suggestion.SuggestionCategory category;
        
        private Boolean isPublic;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminResponseRequest {
        private String response;
        private Suggestion.SuggestionStatus status;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdateRequest {
        private Suggestion.SuggestionStatus status;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private java.util.List<Response> suggestions;
        private long totalCount;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}