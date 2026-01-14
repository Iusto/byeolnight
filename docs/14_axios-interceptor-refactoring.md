# Axios 인터셉터 리팩토링

## 개요

프론트엔드 `axios.ts`의 응답 인터셉터가 HTTP 통신 외에 페이지 라우팅까지 담당하고 있어 단일 책임 원칙(SRP)을 위반하고 있었습니다. 이로 인해 비밀번호 재설정 페이지에서 예상치 못한 로그인 페이지 리다이렉트가 발생하는 버그가 있었습니다.

## 문제 상황

### 증상
- 비밀번호 재설정 페이지(`/reset-password`)에서 토큰 유효시간이 지나면 자동으로 로그인 페이지로 리다이렉트됨
- 사용자가 새 비밀번호를 입력하는 도중 갑자기 페이지가 전환되는 UX 문제

### 원인
axios 인터셉터가 토큰 갱신 실패 시 `window.location.href`를 직접 조작하여 리다이렉트를 수행

---

## 변경 전 코드

### 1. 토큰 갱신 실패 시 자동 리다이렉트 로직

```typescript
// byeolnight-frontend/src/lib/axios.ts (Before)

} catch (refreshError) {
  processQueue(refreshError);

  // 회원 전용 API에서만 알림 후 로그인 페이지로 이동
  const currentPath = window.location.pathname;
  const isAuthRequiredApi = originalRequest.url?.startsWith('/member/');

  if (currentPath.includes('/suggestions') && isAuthRequiredApi) {
    // 중복 알림 방지를 위해 세션스토리지 체크
    const alertShown = sessionStorage.getItem('auth-alert-shown');
    if (!alertShown) {
      sessionStorage.setItem('auth-alert-shown', 'true');
      alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
    }
    window.location.href = '/login';  // 강제 리다이렉트
    return Promise.reject(error);
  }

  // 공개 페이지에서는 리다이렉트하지 않음
  const publicPaths = ['/', '/posts', '/login', '/signup', '/reset-password', '/oauth-recover', '/suggestions'];
  const isPublicPath = publicPaths.some(path =>
    currentPath === path || currentPath.startsWith('/posts/')
  );

  // 비공개 페이지에서만 로그인 페이지로 리다이렉트
  if (!isPublicPath && currentPath !== '/login') {
    window.location.href = '/login';  // 강제 리다이렉트
  }

  return Promise.reject(error);
}
```

### 2. 공개 API 체크 순서 문제

```typescript
// Before - publicApis 체크가 401 체크보다 먼저 실행
const publicApis = ['/auth/withdraw', '/auth/password/', '/auth/oauth/recover'];
if (publicApis.some(api => originalRequest.url?.includes(api))) {
  return Promise.reject(error);
}

// 그 다음 401 체크
if (error.response?.status !== 401 || originalRequest._retry) {
  return Promise.reject(error);
}
```

### 3. 타입 안전성 부재

```typescript
// Before
const processQueue = (error: any) => { ... }
```

---

## 문제점 분석

### 1. 단일 책임 원칙(SRP) 위반
- axios 인터셉터가 HTTP 통신과 페이지 라우팅 두 가지 책임을 동시에 담당
- 라우팅 로직이 axios에 있어 디버깅과 유지보수가 어려움

### 2. 복잡한 조건 분기
- `publicPaths`, `publicApis`, `isAuthRequiredApi` 등 여러 조건이 얽혀 있음
- 새로운 페이지나 API 추가 시 axios.ts까지 수정해야 함

### 3. 예측 불가능한 동작
- `window.location.href` 직접 조작으로 React Router의 상태 관리 우회
- 컴포넌트 언마운트 없이 페이지 전환되어 메모리 누수 가능성

### 4. 타입 안전성 부재
- `error: any` 타입으로 런타임 에러 가능성

---

## 변경 후 코드

### 1. 리다이렉트 로직 완전 제거

```typescript
// byeolnight-frontend/src/lib/axios.ts (After)

} catch (refreshError) {
  processQueue(refreshError);
  // 토큰 갱신 실패 시 에러만 반환 (각 페이지/컴포넌트에서 처리)
  return Promise.reject(error);
}
```

### 2. 로직 순서 정리 + 공개 API 목록 확장

```typescript
// After - 401 체크 먼저, 그 다음 publicApis 체크
if (error.response?.status !== 401 || originalRequest._retry) {
  return Promise.reject(error);
}

// 공개 API들은 토큰 재발급 시도하지 않음 (백엔드에서 이미 permitAll 처리됨)
const publicApis = ['/auth/me', '/auth/login', '/auth/signup', '/auth/password/', '/auth/oauth/', '/public/'];
if (publicApis.some(api => originalRequest.url?.includes(api))) {
  return Promise.reject(error);
}
```

### 3. 타입 개선

```typescript
// After
const processQueue = (error: unknown) => { ... }
```

---

## 변경 요약

| 항목 | Before | After |
|------|--------|-------|
| **책임** | axios가 라우팅까지 담당 | axios는 HTTP 통신만 담당 |
| **리다이렉트** | 인터셉터에서 `window.location.href` 호출 | 각 페이지/컴포넌트에서 에러 처리 |
| **publicApis** | 3개 | 6개 (확장) |
| **타입** | `error: any` | `error: unknown` |
| **코드량** | 약 55줄 | 약 30줄 (-25줄) |

---

## 설계 원칙

### axios 인터셉터의 역할 (수정 후)
1. 요청 시 FormData 처리
2. 네트워크 에러 감지 및 서버 헬스체크
3. 401 에러 시 토큰 갱신 시도
4. **에러 반환** (리다이렉트 X)

### 인증 에러 처리 흐름 (수정 후)
```
API 호출 → 401 에러 발생 → 토큰 갱신 시도 → 실패 → 에러 반환
                                                    ↓
                                          페이지/컴포넌트에서 처리
                                          (에러 메시지 표시 또는 리다이렉트)
```

---

## 관련 파일

- `byeolnight-frontend/src/lib/axios.ts` - 수정된 파일
- `byeolnight-frontend/src/types/api.ts` - 에러 처리 헬퍼 함수
- `byeolnight-frontend/src/contexts/AuthContext.tsx` - 인증 상태 관리

---

## 작업일: 2026-01-14
