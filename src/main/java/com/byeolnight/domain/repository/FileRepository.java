package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.file.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByPostId(Long postId);
}
