package com.byeolnight.dto.post;

import com.byeolnight.dto.file.FileDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import com.byeolnight.domain.entity.post.Post.Category;

import java.util.List;

@Getter
public class PostRequestDto {

    @NotBlank
    private final String title;

    @NotBlank
    private final String content;

    @NotNull
    private final Category category;

    private List<FileDto> images;

    @Builder
    public PostRequestDto(String title, String content, Category category, List<FileDto> images) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.images = images;
    }
}
