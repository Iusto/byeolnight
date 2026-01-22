package com.byeolnight.entity.file;

import com.byeolnight.entity.post.Post;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FileStatus status = FileStatus.CONFIRMED; // 기존 데이터 마이그레이션용 기본값

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void detachFromPost() {
        this.post = null;
    }

    /**
     * 파일 상태를 CONFIRMED로 변경하고 Post와 연결
     */
    public void confirmWithPost(Post post) {
        this.post = post;
        this.status = FileStatus.CONFIRMED;
    }

    /**
     * 파일 상태만 CONFIRMED로 변경 (Post 연결 없이)
     */
    public void confirm() {
        this.status = FileStatus.CONFIRMED;
    }

    public static File of(Post post, String originalName, String s3Key, String url) {
        return File.builder()
                .post(post)
                .originalName(originalName)
                .s3Key(s3Key)
                .url(url)
                .status(FileStatus.CONFIRMED)
                .build();
    }

    /**
     * Presigned URL 발급 시 PENDING 상태로 생성
     */
    public static File createPending(String originalName, String s3Key, String url) {
        return File.builder()
                .originalName(originalName)
                .s3Key(s3Key)
                .url(url)
                .status(FileStatus.PENDING)
                .build();
    }
}
