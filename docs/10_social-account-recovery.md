# 계정 복구 설계

## 목표

탈퇴 후 30일 이내 계정은 사용자가 기존 인증 수단의 소유권을 다시 증명한 경우에만 복구한다.
이메일 주소만으로 계정 상태를 변경하지 않으며, URL에는 이메일이나 외부 인증 오류 원문을 포함하지 않는다.

## 공통 복구 흐름

```text
기존 인증 수단 검증
→ 256비트 무작위 복구 티켓 발급
→ Redis에 티켓 해시와 인증 결과를 10분간 저장
→ 사용자 복구 확인
→ 티켓 조회와 동시에 삭제
→ 계정 상태와 인증 결과 재검증
→ 계정 활성화
→ Access/Refresh Token 발급
```

복구 티켓은 한 번만 사용할 수 있으며 Redis에는 원문 대신 SHA-256 해시를 키로 저장한다.

## OAuth 계정

외부 인증이 성공하면 이메일보다 `(social_provider, social_provider_id)`를 우선하여 사용자를 찾는다.

```text
/oauth2/authorization/{provider}
→ 외부 인증 성공
→ provider + providerUserId로 탈퇴 계정 확인
→ /oauth/recover?ticket={일회용 티켓}
→ POST /api/auth/account/recover
→ 계정 복구와 JWT 발급
```

기존 회원 중 `social_provider_id`가 없는 회원은 같은 provider와 이메일로 로그인한 최초 1회에 한해
검증된 provider user ID를 연결한다.

## 비밀번호 계정

탈퇴 계정의 이메일을 입력했다는 사실만으로 복구 화면을 열지 않는다. 기존 비밀번호 검증에 성공한 뒤
복구 티켓을 발급하며, 이후 과정은 OAuth 계정과 동일하다.

```text
이메일 + 비밀번호 검증
→ ACCOUNT_RECOVERY_REQUIRED:{일회용 티켓}
→ POST /api/auth/account/recover
→ 계정 복구와 JWT 발급
```

## 정책

- 탈퇴 후 30일 이내에만 복구할 수 있다.
- 30일이 지나 개인정보가 마스킹된 계정은 복구하지 않는다.
- 복구 가능 기간에는 동일 외부 계정으로 신규 계정을 만들 수 없다.
- 복구 성공 시 이전 활동 기록과 보유 항목을 유지한다.
- 사용한 티켓과 만료된 티켓은 다시 사용할 수 없다.
- 복구 완료 후 OAuth 로그인을 다시 시작하지 않고 즉시 자체 JWT 로그인을 완료한다.

## 구현 책임

OAuth와 복구 코드는 클래스 이름만으로 역할을 추적할 수 있도록 다음처럼 분리한다.

| 클래스 | 책임 |
|---|---|
| `CustomOAuth2UserService` | Spring Security의 외부 UserInfo 조회와 내부 계정 처리 연결 |
| `OAuthAccountService` | 제공자 ID 우선 회원 조회, 계정 상태 검증, 복구 티켓 요청 |
| `SocialUserRegistrationService` | 신규 소셜 회원 생성과 기본 아이콘·인증서 초기화 |
| `AccountRecoveryService` | 인증 수단별 복구 정보 재검증과 계정 활성화 |
| `AccountRecoveryTicketService` | Redis 기반 일회용 복구 티켓 발급과 소비 |
| `WithdrawnUserCleanupService` | 30일 개인정보 마스킹과 2년 경과 계정 삭제 |

외부 OAuth UserInfo 호출에는 DB 트랜잭션을 열지 않는다. 제공자 응답을 받은 뒤
`OAuthAccountService`에서만 회원 조회와 저장 트랜잭션을 시작한다.

## 운영 반영

운영 DB에는 `social_provider_id` 컬럼과 `(social_provider, social_provider_id)` 고유 제약이 필요하다.
DDL 자동 갱신을 사용하지 않는 환경에서는 `docs/sql/oauth-recovery/01_add_social_provider_id.sql`을 적용한다.
