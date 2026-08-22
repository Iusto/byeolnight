package com.byeolnight.service.file;

import com.byeolnight.entity.file.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 게시글에 연결되지 않은 임시 이미지의 조회와 정리 정책을 담당한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrphanImageCleanupService {

    private static final int RETENTION_DAYS = 7;

    private final OrphanFilePersistenceService persistenceService;
    private final S3Service s3Service;

    public int getOrphanImageCount() {
        return persistenceService.countBefore(cutoffDate());
    }

    /** S3 객체 삭제에 성공한 파일만 DB에서도 제거한다. */
    public int cleanupOrphanImages() {
        List<File> orphanFiles = persistenceService.findBefore(cutoffDate());
        int deletedCount = 0;
        for (File orphanFile : orphanFiles) {
            try {
                s3Service.deleteObjectOrThrow(orphanFile.getS3Key());
                persistenceService.deleteRecord(orphanFile.getId());
                deletedCount++;
            } catch (Exception exception) {
                log.error("고아 이미지 삭제 실패: key={}, id={}",
                        orphanFile.getS3Key(), orphanFile.getId(), exception);
            }
        }
        log.info("고아 이미지 정리 완료: {}건", deletedCount);
        return deletedCount;
    }

    private LocalDateTime cutoffDate() {
        return LocalDateTime.now().minusDays(RETENTION_DAYS);
    }
}
