package com.byeolnight.dto.suggestion;

import com.byeolnight.domain.entity.Suggestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
        private String adminResponse;
        private LocalDateTime adminResponseAt;
        private String adminNickname;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Suggestion suggestion) {
            return Response.builder()
                    .id(suggestion.getId())
                    .title(suggestion.getTitle())
                    .content(suggestion.getContent())
                    .category(suggestion.getCategory())
                    .status(suggestion.getStatus())
                    .authorId(suggestion.getAuthor().getId())
                    .authorNickname(suggestion.getAuthor().getNickname())
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
        private String title;
        private String content;
        private Suggestion.SuggestionCategory category;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String title;
        private String content;
        private Suggestion.SuggestionCategory category;
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
    public static class ListResponse {
        private java.util.List<Response> suggestions;
        private long totalCount;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}