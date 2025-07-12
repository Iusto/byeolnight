package com.byeolnight.service.log;

import com.byeolnight.domain.entity.log.DeleteLog;
import com.byeolnight.domain.repository.log.DeleteLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeleteLogService {

    private final DeleteLogRepository deleteLogRepository;

    @Transactional
    public void logDeletion(Long targetId, DeleteLog.TargetType targetType, 
                           DeleteLog.ActionType actionType, Long deletedBy, 
                           String reason, String originalContent) {
        
        // 개인정보 마스킹
        String maskedContent = maskSensitiveInfo(originalContent);
        
        DeleteLog deleteLog = DeleteLog.builder()
                .targetId(targetId)
                .targetType(targetType)
                .actionType(actionType)
                .deletedBy(deletedBy)
                .reason(reason)
                .originalContent(maskedContent)
                .build();
                
        deleteLogRepository.save(deleteLog);
    }

    public Page<DeleteLog> getAllDeleteLogs(Pageable pageable) {
        return deleteLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<DeleteLog> getDeleteLogsByType(DeleteLog.TargetType targetType, Pageable pageable) {
        return deleteLogRepository.findByTargetTypeOrderByCreatedAtDesc(targetType, pageable);
    }

    private String maskSensitiveInfo(String content) {
        if (content == null) return null;
        
        // 이메일 마스킹
        content = content.replaceAll("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})", "$1***@$2");
        
        // 전화번호 마스킹
        content = content.replaceAll("(\\d{3})-?(\\d{4})-?(\\d{4})", "$1-****-$3");
        
        return content;
    }
}