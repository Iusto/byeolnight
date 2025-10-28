package com.byeolnight.service.chat;

import com.byeolnight.entity.chat.ChatBan;
import com.byeolnight.entity.chat.ChatMessage;
import com.byeolnight.repository.chat.ChatBanRepository;
import com.byeolnight.repository.chat.ChatMessageRepository;
import com.byeolnight.dto.admin.ChatStatsDto;
import com.byeolnight.dto.admin.ChatBanStatusDto;
import com.byeolnight.dto.admin.BannedUserDto;
import com.byeolnight.dto.admin.BlindedMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatBanRepository chatBanRepository;

    private final StringRedisTemplate redisTemplate;

    // 메시지 블라인드 처리
    @Transactional
    public void blindMessage(String messageId, Long adminId) {
        Long id = Long.parseLong(messageId);
        ChatMessage message = chatMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다: " + messageId));
        
        message.blind(adminId);
        chatMessageRepository.save(message);
        

        
        log.info("메시지 {} 블라인드 처리됨 by 관리자 {}", messageId, adminId);
    }

    // 메시지 블라인드 해제
    @Transactional
    public void unblindMessage(String messageId) {
        Long id = Long.parseLong(messageId);
        ChatMessage message = chatMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다: " + messageId));
        
        message.unblind();
        chatMessageRepository.save(message);
        

        
        log.info("메시지 {} 블라인드 해제됨", messageId);
    }

    // 사용자 채팅 금지
    @Transactional
    public void banUser(String username, int durationMinutes, Long adminId, String reason) {
        // 기존 활성 금지가 있다면 해제
        chatBanRepository.findByUsernameAndIsActiveTrueAndBannedUntilAfter(username, LocalDateTime.now())
                .ifPresent(ChatBan::unban);
        
        // 새로운 금지 생성
        ChatBan chatBan = ChatBan.builder()
                .username(username)
                .bannedBy(adminId)
                .bannedUntil(LocalDateTime.now().plusMinutes(durationMinutes))
                .reason(reason != null ? reason : durationMinutes + "분간 채팅 금지")
                .build();
        
        chatBanRepository.save(chatBan);
        

        
        log.info("사용자 {} {}분간 채팅 금지됨 by 관리자 {} - 사유: {}", username, durationMinutes, adminId, reason);
    }

    // 채팅 금지 해제
    @Transactional
    public void unbanUser(String userId) {
        chatBanRepository.findByUsernameAndIsActiveTrueAndBannedUntilAfter(userId, LocalDateTime.now())
                .ifPresent(ban -> {
                    ban.unban();
                    chatBanRepository.save(ban);
                    

                    
                    log.info("사용자 {} 채팅 금지 해제됨", userId);
                });
    }

    // 채팅 통계 조회
    public ChatStatsDto getChatStats() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        long totalMessages = chatMessageRepository.count();
        long blindedMessages = chatMessageRepository.countByIsBlindedTrue();
        long bannedUsers = chatBanRepository.countByIsActiveTrueAndBannedUntilAfter(LocalDateTime.now());
        long activeUsers = chatMessageRepository.countDistinctSenderByTimestampAfter(oneHourAgo);
        
        return new ChatStatsDto(totalMessages, blindedMessages, bannedUsers, activeUsers);
    }

    // 제재된 사용자 목록 조회 (페이징 지원)
    public List<BannedUserDto> getBannedUsers(int limit, int offset) {
        List<ChatBan> activeBans = chatBanRepository.findByIsActiveTrueOrderByBannedAtDesc();
        
        return activeBans.stream()
                .filter(ban -> !ban.isExpired())
                .skip(offset)
                .limit(limit)
                .map(ban -> BannedUserDto.builder()
                    .userId(ban.getUsername())
                    .username(ban.getUsername())
                    .bannedUntil(ban.getBannedUntil())
                    .bannedBy(ban.getBannedBy())
                    .reason(ban.getReason())
                    .build())
                .collect(Collectors.toList());
    }

    // 블라인드된 메시지 목록 조회 (페이징 지원)
    public List<BlindedMessageDto> getBlindedMessages(int limit, int offset) {
        List<ChatMessage> blindedMessages = chatMessageRepository
                .findByIsBlindedTrueOrderByBlindedAtDesc(PageRequest.of(offset / limit, limit));
        
        return blindedMessages.stream()
                .map(message -> BlindedMessageDto.builder()
                    .messageId(message.getId().toString())
                    .blindedBy(message.getBlindedBy())
                    .blindedAt(message.getBlindedAt())
                    .originalMessage(message.getMessage())
                    .sender(message.getSender())
                    .build())
                .collect(Collectors.toList());
    }

    // 사용자가 채팅 금지 상태인지 확인
    public boolean isUserBanned(String username) {
        Optional<ChatBan> activeBan = chatBanRepository.findByUsernameAndIsActiveTrueAndBannedUntilAfter(username, LocalDateTime.now());
        
        if (activeBan.isPresent()) {
            ChatBan ban = activeBan.get();
            // 만료된 밴은 즉시 비활성화
            if (ban.isExpired()) {
                ban.unban();
                chatBanRepository.save(ban);
                return false;
            }
            return true;
        }
        return false;
    }

    // 사용자 채팅 금지 상태 상세 정보 조회
    public ChatBanStatusDto getUserBanStatus(String username) {
        Optional<ChatBan> activeBan = chatBanRepository
                .findByUsernameAndIsActiveTrueAndBannedUntilAfter(username, LocalDateTime.now());
        
        if (activeBan.isPresent()) {
            ChatBan ban = activeBan.get();
            // 만료된 밴은 즉시 비활성화
            if (ban.isExpired()) {
                ban.unban();
                chatBanRepository.save(ban);
                return ChatBanStatusDto.builder().banned(false).build();
            } else {
                long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), ban.getBannedUntil()).toMinutes();
                return ChatBanStatusDto.builder()
                    .banned(true)
                    .reason(ban.getReason())
                    .bannedUntil(ban.getBannedUntil())
                    .bannedBy(ban.getBannedBy())
                    .remainingMinutes(Math.max(0, remainingMinutes))
                    .build();
            }
        }
        
        return ChatBanStatusDto.builder().banned(false).build();
    }

    // 만료된 채팅 금지 정리 (스케줄러)
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    @Transactional
    public void cleanupExpiredBans() {
        List<ChatBan> expiredBans = chatBanRepository.findExpiredBans(LocalDateTime.now());
        expiredBans.forEach(ChatBan::unban);
        if (!expiredBans.isEmpty()) {
            chatBanRepository.saveAll(expiredBans);
            log.info("만료된 채팅 금지 {}건 정리됨", expiredBans.size());
        }
    }
}