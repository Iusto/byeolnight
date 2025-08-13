package com.byeolnight.service.message;

import com.byeolnight.entity.Message;
import com.byeolnight.entity.log.DeleteLog;
import com.byeolnight.repository.MessageRepository;
import com.byeolnight.service.log.DeleteLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageCleanupService {
    
    private final MessageRepository messageRepository;
    private final DeleteLogService deleteLogService;
    
    @Scheduled(cron = "0 0 10 * * ?") // 매일 아침 10시 실행
    @Transactional
    public void cleanupOldMessages() {
        List<Message> messagesToDelete = messageRepository.findMessagesEligibleForPermanentDeletion();
        
        for (Message message : messagesToDelete) {
            // 삭제 로그 기록
            deleteLogService.logDeletion(
                message.getId(),
                DeleteLog.TargetType.MESSAGE,
                DeleteLog.ActionType.PERMANENT_DELETE,
                null, // 시스템 자동 삭제
                "3년 경과 후 자동 영구 삭제",
                message.getTitle() + ": " + message.getContent()
            );
            
            messageRepository.delete(message);
        }
        
        log.info("오래된 쪽지 영구 삭제 완료: {}건", messagesToDelete.size());
    }
}