package com.byeolnight.service.crawler;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.dto.ai.NewsDto;
import com.byeolnight.dto.ai.EventDto;
import com.byeolnight.service.post.PostService;
import com.byeolnight.domain.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService {
    
    private final PostService postService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    
    @Transactional
    public void processNewsData(NewsDto newsDto) {
        try {
            // 뉴스봇 계정 조회
            User newsBot = getNewsBotUser();
            
            // 뉴스는 매번 새 게시글로 등록 (중복 처리 없음)
            String formattedContent = formatNewsContent(newsDto);
            
            postService.createNewsPost(
                newsDto.getTitle(),
                formattedContent,
                Post.Category.NEWS,
                newsBot
            );
            
            log.info("뉴스 게시글 등록 완료: {}", newsDto.getTitle());
            
        } catch (Exception e) {
            log.error("뉴스 처리 중 오류 발생 - 제목: {}, 오류: {}", newsDto.getTitle(), e.getMessage(), e);
            throw new RuntimeException("뉴스 처리 실패", e);
        }
    }
    
    private User getNewsBotUser() {
        return userRepository.findByEmail("newsbot@byeolnight.com")
                .orElseThrow(() -> new RuntimeException("뉴스봇 사용자를 찾을 수 없습니다."));
    }
    
    private User getObservatoryBotUser() {
        return userRepository.findByEmail("observatorybot@byeolnight.com")
                .orElseThrow(() -> new RuntimeException("천문대봇 사용자를 찾을 수 없습니다."));
    }
    
    private boolean isDuplicateEvent(EventDto eventDto, User writer) {
        try {
            // 동일 제목과 일정을 가진 이벤트가 최근 30일 내에 있는지 확인
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            return postRepository.existsByTitleAndCategoryAndWriterAndCreatedAtAfter(
                eventDto.getTitle(),
                Post.Category.EVENT,
                writer,
                thirtyDaysAgo
            );
        } catch (Exception e) {
            log.warn("중복 이벤트 판단 실패 - 제목: {}, 오류: {}", eventDto.getTitle(), e.getMessage());
            return false; // 오류 시 중복이 아닌 것으로 간주하여 등록 진행
        }
    }
    
    private String formatNewsContent(NewsDto newsDto) {
        StringBuilder content = new StringBuilder();
        
        content.append(newsDto.getContent()).append("\n\n");
        
        // 출처 정보 추가
        if (newsDto.getSource() != null) {
            content.append("**출처:** ").append(newsDto.getSource()).append("\n");
        }
        
        // 원본 URL 추가
        if (newsDto.getUrl() != null) {
            content.append("**원문 링크:** [바로가기](").append(newsDto.getUrl()).append(")\n");
        }
        
        // 발행 시간 추가
        if (newsDto.getPublishedAt() != null) {
            content.append("**발행 시간:** ").append(newsDto.getPublishedAt()).append("\n");
        }
        
        // 태그 추가
        if (newsDto.getTags() != null && newsDto.getTags().length > 0) {
            content.append("**태그:** ");
            for (int i = 0; i < newsDto.getTags().length; i++) {
                content.append("#").append(newsDto.getTags()[i]);
                if (i < newsDto.getTags().length - 1) {
                    content.append(" ");
                }
            }
        }
        
        return content.toString();
    }
    
    @Transactional
    public void processEventData(EventDto eventDto) {
        try {
            // 천문대봇 계정 조회
            User observatoryBot = getObservatoryBotUser();
            
            // 중복 처리: 동일 제목+일정이면 skip
            if (isDuplicateEvent(eventDto, observatoryBot)) {
                log.info("중복 이벤트 게시글 스킵: {}", eventDto.getTitle());
                return;
            }
            
            // 이벤트 게시글 생성
            String formattedContent = formatEventContent(eventDto);
            
            postService.createEventPost(
                eventDto.getTitle(),
                formattedContent,
                observatoryBot
            );
            
            log.info("이벤트 게시글 등록 완료: {}", eventDto.getTitle());
            
        } catch (Exception e) {
            log.error("이벤트 처리 중 오류 발생 - 제목: {}, 오류: {}", eventDto.getTitle(), e.getMessage(), e);
            throw new RuntimeException("이벤트 처리 실패", e);
        }
    }
    
    private String formatEventContent(EventDto eventDto) {
        StringBuilder content = new StringBuilder();
        
        // 기본 내용
        if (eventDto.getContent() != null && !eventDto.getContent().trim().isEmpty()) {
            content.append(eventDto.getContent()).append("\n\n");
        }
        
        // 천문대 정보 섹션
        content.append("---\n");
        content.append("🌌 **천문대 정보**\n\n");
        
        // 천문대 유형 표시
        if (eventDto.getObservatoryType() != null) {
            String typeIcon = getObservatoryTypeIcon(eventDto.getObservatoryType());
            content.append(typeIcon).append(" **분류:** ").append(eventDto.getObservatoryType()).append("\n");
        }
        
        // 프로그램명
        if (eventDto.getProgramName() != null) {
            content.append("🎆 **프로그램:** ").append(eventDto.getProgramName()).append("\n");
        }
        
        // 일정 (운영 시간 및 요일)
        if (eventDto.getEventDate() != null) {
            content.append("📅 **일정:** ").append(eventDto.getEventDate()).append("\n");
        }
        
        // 위치 (정확한 주소)
        if (eventDto.getLocation() != null) {
            content.append("📍 **위치:** ").append(eventDto.getLocation()).append("\n");
        }
        
        // 참가비 (요금 정보)
        if (eventDto.getFee() != null) {
            content.append("💰 **참가비:** ").append(eventDto.getFee()).append("\n");
        }
        
        // 연락처 (예약 및 문의 전화번호)
        if (eventDto.getContact() != null) {
            content.append("📞 **연락처:** ").append(eventDto.getContact()).append("\n");
        }
        
        // 신청방법 (온라인 예약 링크)
        if (eventDto.getRegistrationUrl() != null) {
            content.append("🔗 **신청방법:** [온라인 예약](").append(eventDto.getRegistrationUrl()).append(")\n");
        }
        
        // 출처 정보
        if (eventDto.getSource() != null) {
            content.append("\n**출처:** ").append(eventDto.getSource()).append("\n");
        }
        
        // 원문 링크
        if (eventDto.getUrl() != null) {
            content.append("**상세 정보:** [공식 홈페이지](").append(eventDto.getUrl()).append(")\n");
        }
        
        // 태그
        if (eventDto.getTags() != null && eventDto.getTags().length > 0) {
            content.append("\n**태그:** ");
            for (int i = 0; i < eventDto.getTags().length; i++) {
                content.append("#").append(eventDto.getTags()[i]);
                if (i < eventDto.getTags().length - 1) {
                    content.append(" ");
                }
            }
        }
        
        return content.toString();
    }
    
    private String getObservatoryTypeIcon(String type) {
        if (type == null) return "🌌";
        
        if (type.contains("한국천문연구원")) {
            return "🏢"; // 한국천문연구원 산하
        } else if (type.contains("공립과학관")) {
            return "🏛️"; // 공립 과학관
        } else if (type.contains("지역천문대")) {
            return "🌟"; // 지역 천문대
        }
        
        return "🌌"; // 기본 아이콘
    }
}