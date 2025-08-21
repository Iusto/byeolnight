package com.byeolnight.service.suggestion;

import com.byeolnight.entity.Suggestion;

import com.byeolnight.entity.Notification;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.SuggestionRepository;

import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.dto.suggestion.SuggestionDto;
import com.byeolnight.infrastructure.exception.SuggestionNotFoundException;
import com.byeolnight.infrastructure.exception.SuggestionAccessDeniedException;
import com.byeolnight.infrastructure.exception.SuggestionModificationException;
import com.byeolnight.infrastructure.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;
    private final UserRepository userRepository;
    private final com.byeolnight.service.certificate.CertificateService certificateService;

    // 건의사항 목록 조회 (공개 건의사항만)
    public SuggestionDto.ListResponse getSuggestions(
            Suggestion.SuggestionCategory category,
            Suggestion.SuggestionStatus status,
            Pageable pageable
    ) {
        Page<Suggestion> suggestions;

        if (category != null && status != null) {
            suggestions = suggestionRepository.findByCategoryAndStatusAndIsPublicTrue(category, status, pageable);
        } else if (category != null) {
            suggestions = suggestionRepository.findByCategoryAndIsPublicTrue(category, pageable);
        } else if (status != null) {
            suggestions = suggestionRepository.findByStatusAndIsPublicTrue(status, pageable);
        } else {
            suggestions = suggestionRepository.findByIsPublicTrue(pageable);
        }

        return SuggestionDto.ListResponse.builder()
                .suggestions(suggestions.getContent().stream()
                        .map(SuggestionDto.Response::from)
                        .toList())
                .totalCount(suggestions.getTotalElements())
                .currentPage(suggestions.getNumber())
                .totalPages(suggestions.getTotalPages())
                .hasNext(suggestions.hasNext())
                .hasPrevious(suggestions.hasPrevious())
                .build();
    }

    // 건의사항 상세 조회
    public SuggestionDto.Response getSuggestion(Long id) {
        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new SuggestionNotFoundException());
        
        return SuggestionDto.Response.from(suggestion);
    }

    // 건의사항 상세 조회 (접근 권한 체크)
    public SuggestionDto.Response getSuggestion(Long id, Long userId) {
        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new SuggestionNotFoundException());
        
        // 비공개 건의사항인 경우 작성자 또는 관리자만 접근 가능
        if (!suggestion.getIsPublic()) {
            if (userId == null) {
                throw new SuggestionAccessDeniedException();
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
            
            boolean isAuthor = suggestion.getAuthor().getId().equals(userId);
            boolean isAdmin = user.getRole() == User.Role.ADMIN;
            
            if (!isAuthor && !isAdmin) {
                throw new SuggestionAccessDeniedException();
            }
        }
        
        return SuggestionDto.Response.from(suggestion);
    }

    // 건의사항 작성
    @Transactional
    public SuggestionDto.Response createSuggestion(Long userId, SuggestionDto.CreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Suggestion suggestion = Suggestion.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .author(user)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true) // 기본값: 공개
                .build();

        Suggestion savedSuggestion = suggestionRepository.save(suggestion);
        
        // 건의사항 인증서 체크
        try {
            certificateService.checkAndIssueCertificates(user, com.byeolnight.service.certificate.CertificateService.CertificateCheckType.SUGGESTION_WRITE);
        } catch (Exception e) {
            System.err.println("건의사항 인증서 발급 실패: " + e.getMessage());
        }
        
        return SuggestionDto.Response.from(savedSuggestion);
    }

    // 건의사항 수정
    @Transactional
    public SuggestionDto.Response updateSuggestion(Long id, Long userId, SuggestionDto.UpdateRequest request) {
        // 요청 데이터 유효성 검사
        if (request == null) {
            throw new IllegalArgumentException("수정 요청 데이터가 없습니다.");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("내용은 필수입니다.");
        }
        if (request.getCategory() == null) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }
        
        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new SuggestionNotFoundException());

        // 작성자 본인만 수정 가능
        if (!suggestion.getAuthor().getId().equals(userId)) {
            throw new SuggestionAccessDeniedException();
        }

        // 관리자 답변이 없는 경우에만 수정 가능
        if (suggestion.getAdminResponse() != null) {
            throw new SuggestionModificationException("관리자 답변이 등록된 건의사항은 수정할 수 없습니다.");
        }

        try {
            suggestion.update(request.getTitle(), request.getContent(), request.getCategory(), request.getIsPublic());
            return SuggestionDto.Response.from(suggestion);
        } catch (Exception e) {
            throw new SuggestionModificationException("건의사항 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 건의사항 삭제
    @Transactional
    public void deleteSuggestion(Long id, Long userId) {
        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new SuggestionNotFoundException());

        // 작성자 본인만 삭제 가능
        if (!suggestion.getAuthor().getId().equals(userId)) {
            throw new SuggestionAccessDeniedException();
        }

        // 관리자 답변이 없는 경우에만 삭제 가능
        if (suggestion.getAdminResponse() != null) {
            throw new SuggestionModificationException("관리자 답변이 등록된 건의사항은 삭제할 수 없습니다.");
        }

        suggestionRepository.delete(suggestion);
    }

    // 관리자 답변 추가
    @Transactional
    public SuggestionDto.Response addAdminResponse(Long id, Long adminId, SuggestionDto.AdminResponseRequest request) {
        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new SuggestionNotFoundException());

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("관리자를 찾을 수 없습니다."));

        // 상태가 지정되지 않았으면 기본값으로 COMPLETED 사용
        Suggestion.SuggestionStatus status = request.getStatus() != null ? request.getStatus() : Suggestion.SuggestionStatus.COMPLETED;
        suggestion.addAdminResponse(request.getResponse(), admin, status);
        
        // 건의사항 작성자에게 알림 전송
        try {
            com.byeolnight.service.notification.NotificationService notificationService = 
                com.byeolnight.infrastructure.config.ApplicationContextProvider
                    .getBean(com.byeolnight.service.notification.NotificationService.class);
            
            String notificationMessage = String.format("건의사항 '%s'에 관리자 답변이 등록되었습니다.", 
                suggestion.getTitle().length() > 20 ? suggestion.getTitle().substring(0, 20) + "..." : suggestion.getTitle());
            
            notificationService.createNotification(
                suggestion.getAuthor().getId(),
                Notification.NotificationType.SUGGESTION_RESPONSE,
                "건의사항 답변 알림",
                notificationMessage,
                "/suggestions/" + suggestion.getId(),
                suggestion.getId()
            );
        } catch (Exception e) {
            System.err.println("건의사항 답변 알림 전송 실패: " + e.getMessage());
        }

        return SuggestionDto.Response.from(suggestion);
    }

    // 건의사항 상태만 변경
    @Transactional
    public SuggestionDto.Response updateStatus(Long id, Long adminId, SuggestionDto.StatusUpdateRequest request) {
        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new SuggestionNotFoundException());

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("관리자를 찾을 수 없습니다."));

        suggestion.updateStatus(request.getStatus());
        return SuggestionDto.Response.from(suggestion);
    }

    // 내 건의사항 조회
    public SuggestionDto.ListResponse getMySuggestions(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Page<Suggestion> suggestions = suggestionRepository.findByAuthor(user, pageable);

        return SuggestionDto.ListResponse.builder()
                .suggestions(suggestions.getContent().stream()
                        .map(SuggestionDto.Response::from)
                        .toList())
                .totalCount(suggestions.getTotalElements())
                .currentPage(suggestions.getNumber())
                .totalPages(suggestions.getTotalPages())
                .hasNext(suggestions.hasNext())
                .hasPrevious(suggestions.hasPrevious())
                .build();
    }

    // 관리자용 전체 건의사항 조회 (공개/비공개 모두 포함)
    public SuggestionDto.ListResponse getAllSuggestionsForAdmin(
            Suggestion.SuggestionCategory category,
            Suggestion.SuggestionStatus status,
            Pageable pageable
    ) {
        Page<Suggestion> suggestions;

        if (category != null && status != null) {
            suggestions = suggestionRepository.findByCategoryAndStatus(category, status, pageable);
        } else if (category != null) {
            suggestions = suggestionRepository.findByCategory(category, pageable);
        } else if (status != null) {
            suggestions = suggestionRepository.findByStatus(status, pageable);
        } else {
            suggestions = suggestionRepository.findAll(pageable);
        }

        return SuggestionDto.ListResponse.builder()
                .suggestions(suggestions.getContent().stream()
                        .map(SuggestionDto.Response::from)
                        .toList())
                .totalCount(suggestions.getTotalElements())
                .currentPage(suggestions.getNumber())
                .totalPages(suggestions.getTotalPages())
                .hasNext(suggestions.hasNext())
                .hasPrevious(suggestions.hasPrevious())
                .build();
    }

    // 관리자 여부 확인
    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        
        User user = userRepository.findById(userId)
                .orElse(null);
        
        return user != null && user.getRole() == User.Role.ADMIN;
    }
}