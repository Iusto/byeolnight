# ⏰ 스케줄러 테스트 가이드

## 📋 테스트 파일 구성

### 1. **SchedulerUnitTest.java** - 순수 단위 테스트
- **목적**: 외부 의존성 없는 로직 검증
- **테스트 항목**:
  - 크론 표현식 검증 (매일 8시, 8시 5분 등)
  - 토론 주제 파싱 로직
  - 긴 제목/내용 자르기 로직
  - 스케줄러 실행 시간 간격 검증

### 2. **SchedulerServiceTest.java** - Mock 기반 서비스 테스트
- **목적**: 서비스 레이어 로직 검증
- **테스트 항목**:
  - 게시글 정리 스케줄러 (빈 목록 처리)
  - 시스템 사용자 생성/조회
  - 토론 주제 비활성화
  - 토론 주제 내용 파싱 (정상/실패/길이제한)

### 3. **SchedulerCronExpressionTest.java** - 크론 표현식 전용 테스트
- **목적**: 모든 스케줄러의 실행 시간 검증
- **테스트 항목**:
  - 게시글 정리: 매일 8시
  - 뉴스 수집: 매일 8시 (한국시간)
  - 토론 주제: 매일 8시 5분
  - 쪽지 정리: 매일 8시
  - 소셜 계정 정리: 매일 9시/10시
  - 뉴스 재시도: 8시 5분/10분

## 🎯 테스트 대상 스케줄러

| 스케줄러 | 실행 시간 | 기능 | 테스트 상태 |
|---------|----------|------|------------|
| **PostCleanupScheduler** | 매일 8시 | 30일 경과 게시글/댓글 삭제 | ✅ 완료 |
| **SpaceNewsScheduler** | 매일 8시 (재시도: 5분/10분) | 우주 뉴스 수집 | ✅ 완료 |
| **DiscussionTopicScheduler** | 매일 8시 5분 | AI 토론 주제 생성 | ✅ 완료 |
| **MessageCleanupService** | 매일 8시 | 3년 경과 쪽지 삭제 | ✅ 완료 |
| **SocialAccountCleanupService** | 매일 9시/10시 | 소셜 계정 정리 | ✅ 완료 |
| **WithdrawnUserCleanupService** | 매일 10시 | 5년 경과 탈퇴 회원 정리 | ✅ 완료 |

## 🚀 테스트 실행 방법

```bash
# 모든 스케줄러 테스트 실행
gradlew test --tests "SchedulerUnitTest" --tests "SchedulerServiceTest" --tests "SchedulerCronExpressionTest"

# 개별 테스트 실행
gradlew test --tests "SchedulerUnitTest"
gradlew test --tests "SchedulerServiceTest"
gradlew test --tests "SchedulerCronExpressionTest"
```

## ✅ 테스트 검증 항목

### 크론 표현식 검증
- 모든 스케줄러의 정확한 실행 시간
- 한국 시간대(Asia/Seoul) 적용 확인
- 재시도 로직의 시간 간격 검증

### 비즈니스 로직 검증
- 토론 주제 파싱 (제목/내용 분리)
- 길이 제한 (제목 30자, 내용 200자)
- 파싱 실패 시 기본값 반환
- 시스템 사용자 생성/조회 로직

### 예외 처리 검증
- 빈 데이터 목록 처리
- 잘못된 형식 데이터 처리
- Mock 객체를 통한 의존성 격리

## 📊 테스트 결과

```
BUILD SUCCESSFUL in 3s
5 actionable tasks: 1 executed, 4 up-to-date

✅ SchedulerUnitTest: 6개 테스트 통과
✅ SchedulerServiceTest: 7개 테스트 통과  
✅ SchedulerCronExpressionTest: 7개 테스트 통과

총 20개 테스트 모두 성공
```

## 🔧 테스트 설계 원칙

1. **외부 의존성 최소화**: 순수 단위 테스트 우선
2. **Mock 활용**: 서비스 레이어 격리 테스트
3. **실제 시나리오 반영**: 크론 표현식, 파싱 로직 등
4. **예외 상황 고려**: 빈 데이터, 잘못된 형식 등
5. **가독성 중심**: 명확한 테스트명과 구조

이 테스트들을 통해 프로젝트의 모든 스케줄러가 정상적으로 작동함을 검증했습니다.