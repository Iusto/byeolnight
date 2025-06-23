package com.byeolnight.domain.repository.post;

import com.byeolnight.domain.entity.file.File;
import com.byeolnight.domain.entity.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {
    void deleteAllByPost(Post post); // ✔️ 게시글 삭제 시 첨부 파일도 삭제
}
