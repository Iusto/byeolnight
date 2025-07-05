package com.byeolnight.service.suggestion;

import com.byeolnight.domain.entity.Suggestion;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.SuggestionRepository;

import com.byeolnight.domain.repository.user.UserRepository;
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

    // 건의사항 목록 조회
    public SuggestionDto.ListResponse getSuggestions(
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

    // 건의사항 상세 조회
    public SuggestionDto.Response getSuggestion(Long id) {
        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new SuggestionNotFoundException());
        
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
                .build();

        Suggestion savedSuggestion = suggestionRepository.save(suggestion);
        return SuggestionDto.Response.from(savedSuggestion);
    }

    // 건의사항 수정
    @Transactional
    public SuggestionDto.Response updateSuggestion(Long id, Long userId, SuggestionDto.UpdateRequest request) {
        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new SuggestionNotFoundException());

        // 작성자 본인만 수정 가능
        if (!suggestion.getAuthor().getId().equals(userId)) {
            throw new SuggestionAccessDeniedException();
        }

        // 검토 중인 상태에서만 수정 가능
        if (suggestion.getStatus() != Suggestion.SuggestionStatus.PENDING) {
            throw new SuggestionModificationException("검토 중인 건의사항만 수정할 수 있습니다.");
        }

        suggestion.update(request.getTitle(), request.getContent(), request.getCategory());
        return SuggestionDto.Response.from(suggestion);
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

        // 검토 중인 상태에서만 삭제 가능
        if (suggestion.getStatus() != Suggestion.SuggestionStatus.PENDING) {
            throw new SuggestionModificationException("검토 중인 건의사항만 삭제할 수 있습니다.");
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

        suggestion.addAdminResponse(request.getResponse(), admin);
        if (request.getStatus() != null) {
            suggestion.updateStatus(request.getStatus());
        }

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
}