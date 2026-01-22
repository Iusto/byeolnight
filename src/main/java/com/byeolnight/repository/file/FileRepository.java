package com.byeolnight.repository.file;

import com.byeolnight.entity.file.File;
import com.byeolnight.entity.file.FileStatus;
import com.byeolnight.entity.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    void deleteAllByPost(Post post); // ✔️ 게시글 삭제 시 첨부 파일도 삭제
    List<File> findAllByPost(Post post);

    /**
     * S3 키로 파일 조회
     */
    Optional<File> findByS3Key(String s3Key);

    /**
     * URL로 파일 조회
     */
    Optional<File> findByUrl(String url);

    /**
     * 고아 파일 조회: PENDING 상태이고 지정된 날짜 이전에 생성된 파일
     */
    List<File> findByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime cutoffDate);

    /**
     * 고아 파일 개수 조회
     */
    long countByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime cutoffDate);

    /**
     * 특정 상태의 파일 목록 조회
     */
    List<File> findByStatus(FileStatus status);
}
