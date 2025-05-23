package com.byeolnight.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import com.byeolnight.domain.entity.post.Post.Category;

@Getter
public class PostRequestDto {

    @NotBlank
    private final String title;

    @NotBlank
    private final String content;

    @NotNull
    private final Category category;

    @Builder
    public PostRequestDto(String title, String content, Category category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }
}
