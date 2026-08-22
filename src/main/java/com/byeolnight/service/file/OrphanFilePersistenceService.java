package com.byeolnight.service.file;

import com.byeolnight.entity.file.File;
import com.byeolnight.entity.file.FileStatus;
import com.byeolnight.repository.file.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 고아 파일 정리에서 DB 조회·삭제의 짧은 트랜잭션만 담당한다. */
@Service
@RequiredArgsConstructor
public class OrphanFilePersistenceService {

    private final FileRepository fileRepository;

    @Transactional(readOnly = true)
    public int countBefore(LocalDateTime cutoff) {
        return Math.toIntExact(fileRepository.countByStatusAndCreatedAtBefore(FileStatus.PENDING, cutoff));
    }

    @Transactional(readOnly = true)
    public List<File> findBefore(LocalDateTime cutoff) {
        return fileRepository.findByStatusAndCreatedAtBefore(FileStatus.PENDING, cutoff);
    }

    @Transactional
    public void deleteRecord(Long fileId) {
        fileRepository.deleteById(fileId);
    }
}
