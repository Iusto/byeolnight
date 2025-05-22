
# 별 헤는 밤
> 실시간 채팅 + AI 추천 기능이 통합된 커뮤니티 플랫폼  
> 구조적 문제 해결과 운영 환경을 고려한 백엔드 중심 설계 프로젝트

## 1. 프로젝트 개요
- **목표**: 단순 CRUD 게시판이 아닌, 인증/보안/세션/실시간 통신/AI 추천까지 포함한 실서비스 수준 백엔드 시스템 구축
- **역할**: 100% 개인 설계 및 구현 (백엔드 중심 + 프론트 일부 연동)
- **기간**: 2025.05 ~ 진행 중
- **기여 목적**: 기존 Outven과 Galaxy ML 프로젝트의 통합 개선 버전

## 2. 기술 스택 및 아키텍처
| 구분 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.2, JPA, Spring Security, WebSocket |
| Infra | Docker, GitHub Actions, AWS EC2, Nginx |
| DB | MySQL 8.0, Redis |
| Test | JUnit5, Mockito |
| API Docs | Swagger/OpenAPI 3.0 |
| Frontend | React (간단한 연동), REST API 기반 |

### 전체 아키텍처 구성도
> (클라이언트 → React → API Gateway(Nginx) → Spring → DB/Redis)

## 3. 주요 기능 요약
| 기능 영역 | 설명 |
|-----------|------|
| 회원가입/로그인 | JWT + Spring Security 인증 처리 |
| 게시판 CRUD | 글 목록/상세/등록/수정/삭제 + 댓글 |
| 실시간 채팅 | WebSocket + STOMP 기반 메시지 송수신 |
| AI 추천 | (예정) Galaxy ML 연동 통한 유사 게시글 추천 |
| API 문서화 | Swagger 3.0 기반 자동 문서 생성 |
| 세션/보안 | Redis + Spring Session 구성, BCrypt 암호화 |
| 테스트 | JUnit5 단위 테스트, Mockito Mock 기반 테스트 |
| 배포 | Docker + GitHub Actions + EC2 + Nginx 자동 배포 구성

## 4. 구현 상세: 화면 & API 흐름
- 로그인 → JWT 발급 → localStorage 저장 → 게시판 접근
- 게시글 조회: `/api/boards?page=x`
- 게시글 등록: POST `/api/boards` (JWT 인증 필요)
- 채팅: WebSocket 연결 → 토픽 구독 `/topic/room/1` → 메시지 발행 `/app/chat`

> (React 연동된 캡처 이미지 삽입 가능)

## 5. 인증 및 보안 구조
- 인증 방식: JWT + Spring Security + Redis 세션 TTL
- 암호화: BCrypt → 비밀번호 평문 저장 방지
- 예외 처리: Global ControllerAdvice → API 일관된 예외 메시지 제공

## 6. 배포 구조 및 CI/CD
- GitHub Actions → Docker 이미지 빌드 → EC2 SSH 전송
- Nginx Reverse Proxy 구성 → 80/443 포트 HTTPS 인증서 적용 (Let’s Encrypt)
- `.env` 기반 운영/개발 환경 분리 구성

> (배포 구조도 삽입: GitHub → Actions → EC2 → Docker → Spring/Nginx)

## 7. 테스트 전략
- JUnit5 기반 단위 테스트: Service 단위 테스트
- Mockito 기반 Mock 객체 활용
- 통합 테스트: 컨트롤러 → 서비스 흐름 전체 검증

## 8. 폴더 구조 요약
```bash
byeolnight/
├── domain/
│   ├── board/
│   ├── user/
│   └── chat/
├── application/
├── infrastructure/
│   ├── security/
│   ├── config/
│   └── redis/
├── api/
├── test/
```
→ 클린 아키텍처 기반으로 domain-application-infra-api 계층 구분

## 9. 기술 선택 이유 및 철학
- JWT 인증 + Redis 세션: 무상태 기반 인증 구조 확보 + 확장성/보안 모두 고려
- 클린 아키텍처 구조: 유지보수성과 테스트 용이성 고려한 분리 설계
- GitHub Actions 도입: 반복적인 수동 배포 제거 → 효율적인 작업 흐름
- React 연동: 실제 API 소비 흐름을 직접 구현하여 API 설계 적합성 검증

## 10. 실행 방법
```bash
# EC2 기준
git clone https://github.com/Iusto/byeolnight.git
cd byeolnight
cp .env.example .env
docker-compose up -d
```
> 포트: 8080 (Spring), 3000 (React), 6379 (Redis)  
> Swagger: `https://byeolnight.site/swagger-ui/index.html`

## 11. 개발자로서의 성장 포인트
- Outven에서의 구조 설계/보안 경험을 실서비스 운영 관점으로 확장
- Galaxy ML에서의 AI 모델 개선을 추천 기능으로 연결
- 단순 기능 구현이 아닌, 전체 시스템을 아우르는 통합적 사고와 실행력 확보
