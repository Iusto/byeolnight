## 📅 2025년 9월 18일 오전 7시, 운영 서버 장애 발생

평소와 같이 커피 한 잔과 함께 모니터링 대시보드를 확인하던 중, **별 헤는 밤** 서비스에 접속이 안 되는 것을 발견했다. 브라우저에는 무정하게도 `ERR_TOO_MANY_REDIRECTS` 오류만 반복되고 있었다.

> "어제까지 잘 되던 사이트가 갑자기 왜...?"

## 🔍 첫 번째 용의자: Nginx 리다이렉트 중복

처음에는 당연히 **Nginx 설정 문제**라고 생각했다. 최근 배포 과정에서 메인 서버와 프론트엔드에 있는 `nginx.conf` 파일 둘 다 리다이렉트 코드가 중복되어 있었고, API 키를 여러 번 호출해야 하는 우리 웹서비스 특성상 이게 근본 원인일 거라 확신했다.

```bash
# 급하게 Nginx 설정 수정
vim /etc/nginx/nginx.conf
# 중복된 리다이렉트 규칙 제거
sudo systemctl reload nginx
```

다행히 수정 후 서비스가 정상화되었다. "역시 Nginx 문제였구나" 하며 안도의 한숨을 쉬었다.

## 😱 데자뷰: 또 다시 발생한 동일 증상

며칠 후, 서비스가 잘 굴러가고 있다고 생각하며 새로운 기능을 배포했다. 그런데...

**또 같은 이슈가 발생했다!**

이번에는 브라우저 오류가 아니라 **서버 로그**에서 더 심각한 문제를 발견했다:

```
2025-09-18T07:01:29.821+09:00 ERROR 1 --- [byeolnight] [main] 
Caused by: java.lang.IllegalArgumentException: 
Could not resolve placeholder 'app.security.jwt.secret' in value "${app.security.jwt.secret}"
```

> "Nginx 문제가 아니었다... 진짜 원인은 따로 있었구나."

## 🕵️ 진짜 범인을 찾아서

### "Config Server는 살아있는데 JWT만 죽었다?"

로그를 자세히 보니 이상한 점이 있었다:

```
✅ Located environment: name=byeolnight, profiles=[prod]
✅ MySQL 비밀번호: 정상 복호화
✅ Redis 비밀번호: 정상 복호화  
✅ AWS API 키들: 정상 복호화
❌ JWT 시크릿: Could not resolve placeholder
```

> "다른 암호화된 설정들은 멀쩡한데 JWT 시크릿만 왜...?"

### 미스터리한 암호화 데이터

`byeolnight-prod.yml` 파일을 열어보니 JWT 시크릿이 이상했다:

```yaml
# 정상적인 다른 설정들
password: '{cipher}4f645acb62e6302d47f02b2d3b88c8cda3c90e64f91d042dc9ff2613955ba***'

# 문제의 JWT 시크릿
jwt:
  secret: '{cipher}eaebecbd17000326cf1b0281b5e9b9c905f10300270c747f95207cea0c029d34***'
```

**추리 결과:**
- 파일 복사 과정에서 암호화 데이터 일부 손실?
- Git 커밋 시 인코딩 문제?
- 다른 암호화 키로 암호화된 이력?

> "범인은... 손상된 암호화 데이터였다!"

## 🚑 응급처치: 일단 살려놓고 보자

### "평문으로라도 일단 서비스부터 살리자!"

운영 서비스가 다운된 상황에서 원인 분석보다는 **빠른 복구**가 우선이었다.

```yaml
# 응급처치: JWT 시크릿을 평문으로 변경
jwt:
  secret: '********************-REDACTED-********************'
```

```bash
# 서버 재시작
docker-compose restart app
```

> "휴... 일단 서비스는 살아났다. 이제 진짜 해결책을 찾아보자."

## 🔧 근본 해결: Config Server로 재암호화

### "EC2 서버에 직접 접속해서 해결하자!"

평문으로 임시 해결한 후, 보안을 위해 **EC2 서버에 직접 접속하여 Config Server의 암호화 엔드포인트를 호출**하는 방식으로 재암호화를 진행했다.

```bash
# 1단계: EC2 서버에 SSH 접속
ssh -i byeolnight-key.pem ec2-user@<EC2-IP>

# 2단계: Config Server가 실행 중인 상태에서 직접 암호화 요청
curl -X POST http://localhost:8888/encrypt \
  -H "Content-Type: text/plain" \
  -d "********************-REDACTED-********************"
```

**터미널 출력:**
```
8bab426a814eb620e297d3a336d22ebf6ede3b285003154b24898d87ba743416***[새로운 암호화 키 생성됨]
```

> "EC2 서버에서 직접 암호화하니 확실하고 안전하다!"

### 최종 해결: 새 암호화 키 적용

```yaml
# byeolnight-prod.yml 최종 수정
jwt:
  secret: '{cipher}8bab426a814eb620e297d3a336d22ebf6ede3b285003154b24898d87ba743416***'
```

## 🎉 해피엔딩: 별 헤는 밤이 다시 빛났다

### "드디어 정상 로그가 떴다!"

```bash
# 서버 재시작 후 로그 확인
docker logs -f byeolnight-app-1
```

**기다리던 성공 로그:**
```
✅ JwtAuthenticationFilter: Filter 'jwtAuthenticationFilter' configured for use
✅ TomcatWebServer: Tomcat started on port 8080 (http)  
✅ ByeolnightApplication: Started ByeolnightApplication in 28.808 seconds
✅ 별 헤는 밤이 다시 살아났습니다! 🌟
```

### 검증: 모든 기능이 정상 작동

- ✅ JWT 토큰 생성/검증 완벽
- ✅ 사용자 로그인/로그아웃 정상
- ✅ API 호출 인증 성공
- ✅ 소셜 로그인 (Google, Kakao, Naver) 정상
- ✅ 실시간 채팅 WebSocket 연결 성공

> "이제야 마음 놓고 커피를 마실 수 있겠다... ☕"

## 🔧 사용된 도구 및 기술

### Config Server 암호화 구조
```yaml
# Config Server (application.yml)
encrypt:
  key: '********************-REDACTED-********************'
  fail-on-error: false
```

### 암호화 방식 (EC2 서버 직접 접속)
```bash
# EC2 서버에 SSH 접속 후 Config Server의 /encrypt 엔드포인트를 직접 호출
curl -X POST http://localhost:8888/encrypt \
  -H "Content-Type: text/plain" \
  -d "암호화할 값"
```

### JWT 설정 구조
```java
@Component
public class JwtTokenProvider {
    public JwtTokenProvider(@Value("${app.security.jwt.secret}") String secret, 
                           StringRedisTemplate redisTemplate) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.redisTemplate = redisTemplate;
    }
}
```

## 📚 학습 포인트

### Spring Cloud Config 암호화 특성
- **대칭키 암호화 (AES)** 사용
- **시간 기반 만료 없음** (JWT와 다름)
- **암호화 키 일치 시** 언제든 복호화 가능
- **데이터 무결성** 중요

### 문제 해결 접근법
1. **증상 vs 원인** 구분
2. **다른 설정과 비교** 분석
3. **단계별 검증** (임시 → 영구 해결)
4. **재현 가능한 해결책** 적용

### 운영 환경 고려사항
- Config Server **고가용성** 필요
- 암호화 키 **백업 및 관리**
- **모니터링 및 알림** 체계
- **롤백 계획** 수립

## 🚀 향후 개선 방안

### 1. 모니터링 강화
```yaml
# Config Server Health Check
management:
  endpoints:
    web:
      exposure:
        include: health,info,decrypt
```

### 2. 암호화 검증 자동화 (적용 완료)

현재 `deploy.sh`가 Config Server의 `/byeolnight/prod` 응답에서 `app.security.jwt.secret`을 직접 추출해 다음 조건을 검사한다.

- 값 누락 또는 `null`
- `{cipher}`·`invalid`·`${...}` 형태의 미복호화 값
- UTF-8 기준 32바이트 미만

검증에 실패하면 기존 백엔드를 교체하기 전에 배포를 중단한다. 애플리케이션 내부의 `JwtTokenProvider`도 시작 시 최소 길이를 재검증해 배포 경로 밖에서 실행해도 fail fast 한다.

### 3. 백업 전략
- Config 파일 **Git 이력 관리**
- 암호화 키 **안전한 저장소** 보관
- **복구 절차** 문서화

## 🎓 이 사건에서 배운 교훈들

### 1. "증상과 원인은 다를 수 있다"
- 처음 `ERR_TOO_MANY_REDIRECTS`는 Nginx 문제로 보였지만
- 진짜 원인은 **JWT 암호화 데이터 손상**이었다
- 이후 Config Server 응답을 배포 전에 검증하고 애플리케이션 시작 시 한 번 더 검증하도록 보완했다
- **근본 원인**을 찾지 않으면 같은 문제가 반복된다

### 2. "Config Server 암호화는 생각보다 취약하다"
- 파일 복사, Git 커밋 과정에서 데이터 손상 가능
- **정기적인 암호화 데이터 검증**이 필요하다
- 백업된 암호화 키 관리의 중요성

### 3. "응급처치 → 근본해결 순서가 중요하다"
- 운영 서비스 다운 시: **빠른 복구 우선**
- 안정화 후: **보안을 고려한 근본 해결**
- 평문 → 재암호화 단계적 접근이 효과적

---

**에필로그:** 그날 밤, 별 헤는 밤 서비스는 다시 평화롭게 사용자들의 우주 이야기를 담아내고 있었다. 때로는 예상치 못한 곳에서 문제가 발생하지만, **차근차근 원인을 찾아 해결하는 과정**이야말로 개발자의 진짜 실력이 아닐까? 🌙✨
