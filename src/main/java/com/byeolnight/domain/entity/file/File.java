package com.byeolnight.domain.entity.file;

import com.byeolnight.domain.entity.post.Post;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
public class File {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    @Column(nullable = false)
    private String originalName; // 원본 파일명 (예: "내사진.png")

    @Column(nullable = false)
    private String s3Key; // S3 내 저장 경로 및 파일명 (예: "posts/uuid_내사진.png")

    @Column(nullable = false, length = 2000)
    private String url; // S3 접근용 URL

    public void detachFromPost() {
        this.post = null;
    }

    public static File of(Post post, String originalName, String s3Key, String url) {
        return File.builder()
                .post(post)
                .originalName(originalName)
                .s3Key(s3Key)
                .url(url)
                .build();
    }
}
