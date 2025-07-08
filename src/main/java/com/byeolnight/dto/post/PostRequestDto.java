package com.byeolnight.dto.post;

import com.byeolnight.dto.file.FileDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import com.byeolnight.domain.entity.post.Post.Category;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostRequestDto {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private Category category;

    private List<FileDto> images;
    
    private Long originTopicId; // 토론 주제 연결 ID
}
