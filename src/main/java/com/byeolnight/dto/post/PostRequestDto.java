package com.byeolnight.dto.post;

import com.byeolnight.dto.file.FileDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import com.byeolnight.domain.entity.post.Post.Category;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostRequestDto {

    @NotBlank
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    private String title;

    @NotBlank
    @Size(max = 50000, message = "내용은 50,000자를 초과할 수 없습니다.")
    private String content;

    @NotNull
    private Category category;

    private List<FileDto> images;
    
    private Long originTopicId; // 토론 주제 연결 ID
}
