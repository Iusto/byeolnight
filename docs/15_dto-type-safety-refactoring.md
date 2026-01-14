# DTO 리팩토링 작업 목록

## 완료된 작업

### Java 백엔드
- [x] `CurrentUserResponseDto.java` 생성 (`dto/user/`)
- [x] `PresignedUrlResponseDto.java` 생성 (`dto/file/`)
- [x] `ModerationResultDto.java` 생성 (`dto/file/`)
- [x] `ViewUrlResponseDto.java` 생성 (`dto/file/`)
- [x] `UploadResultDto.java` 생성 (`dto/file/`)
- [x] `S3Service.java` 수정 - Map 반환 -> DTO 반환
- [x] `AuthController.java` 수정 - `ResponseEntity<Object>` -> `ResponseEntity<CurrentUserResponseDto>`
- [x] `FileController.java` 수정 - 모든 Map 반환을 DTO로 교체
- [x] `CertificateService.java` - `getRepresentativeCertificateSafe` 메서드 추가
- [x] `SecureS3Service.java` - `Map<String, String>` → `PresignedUrlResponseDto` 반환 타입 변경
- [x] `SecureFileController.java` - `Map<String, String>` → `PresignedUrlResponseDto` 반환 타입 변경

### TypeScript 프론트엔드 - 타입 파일 생성
- [x] `types/api.ts` - 공통 API 응답 타입, `getErrorMessage()` 헬퍼
- [x] `types/user.ts` - User 인터페이스
- [x] `types/file.ts` - 파일 관련 타입
- [x] `types/icons.ts` - 아이콘 컴포넌트 타입
- [x] `types/i18n.ts` - i18n 번역 함수 타입

### TypeScript 프론트엔드 - 수정 완료 파일

#### contexts / lib
- [x] `contexts/AuthContext.tsx` - User import, error 타입
- [x] `lib/axios.ts` - error 타입
- [x] `lib/s3Upload.ts` - PresignedUrlResponse import, error 타입

#### pages 폴더 (15개 파일)
- [x] `pages/Login.tsx`
- [x] `pages/ProfilePage.tsx`
- [x] `pages/Signup.tsx`
- [x] `pages/PasswordReset.tsx`
- [x] `pages/PostCreate.tsx`
- [x] `pages/PostEdit.tsx`
- [x] `pages/PostDetail.tsx`
- [x] `pages/PostReport.tsx`
- [x] `pages/MyPage.tsx`
- [x] `pages/OAuthCallback.tsx`
- [x] `pages/OAuthNicknameSetup.tsx`
- [x] `pages/OAuthRecover.tsx`
- [x] `pages/StellaShop.tsx`
- [x] `pages/SuggestionCreate.tsx`
- [x] `pages/SuggestionDetail.tsx`
- [x] `pages/SuggestionEdit.tsx`
- [x] `pages/AdminUserPage.tsx`
- [x] `pages/PostList.tsx` - import 경로 수정

#### components 폴더 (7개 파일)
- [x] `components/post/TuiEditor.tsx`
- [x] `components/post/CommentForm.tsx`
- [x] `components/post/CommentList.tsx`
- [x] `components/post/PostActions.tsx`
- [x] `components/post/ImageUploader.tsx`
- [x] `components/user/UserProfileModal.tsx`
- [x] `components/user/UserActionPopup.tsx`

#### 아이콘 컴포넌트 타입 개선
- [x] `components/user/StellaIcon.tsx` - `as any` -> `as IconRegistry`
- [x] `components/user/UserIconDisplay.tsx` - `as any` -> `as IconRegistry`

#### i18n 타입 적용
- [x] `constants/categories.ts` - `t: any` -> `t: TranslationFunction`

#### 중복 코드 제거
- [x] `utils/postHelpers.ts` - 중복된 `getCategoryLabel`, `getCategoryDescription` 제거
- [x] `pages/PostList.tsx` - import 경로를 `constants/categories.ts`로 변경

---

## 검증 완료

### TypeScript 빌드
```
npm run build
✓ 659 modules transformed
✓ built in 21.46s
```

---

## 수정 패턴 요약

### Before
```typescript
} catch (err: any) {
  const errorMessage = err.response?.data?.message || err.message || '오류';
  setError(errorMessage);
}
```

### After
```typescript
import { getErrorMessage } from '../types/api';

} catch (err: unknown) {
  setError(getErrorMessage(err));
}
```

### 아이콘 컴포넌트 수정
```typescript
// Before
const IconComponent = (StellarIcons as any)[icon.iconUrl];

// After
import type { IconRegistry } from '../../types/icons';
const IconComponent = (StellarIcons as IconRegistry)[icon.iconUrl];
```

### i18n 타입 수정
```typescript
// Before
export const getCategoryLabel = (category: string, t: any): string => { ... }

// After
import type { TranslationFunction } from '../types/i18n';
export const getCategoryLabel = (category: string, t: TranslationFunction): string => { ... }
```

---

## axios.ts 리팩토링

### 비밀번호 재설정 리다이렉트 문제 해결
- `axios.ts:91` - `/auth/password/` 경로를 `publicApis` 목록에 추가
- 비밀번호 재설정 API는 401 에러 시 토큰 갱신 시도를 하지 않음
- 세션 만료로 인한 자동 로그인 리다이렉트 방지됨

```typescript
// 공개 API들은 토큰 재발급 시도하지 않음
const publicApis = ['/auth/me', '/auth/login', '/auth/signup', '/auth/password/', '/auth/oauth/', '/public/'];
```

---

## 미사용 DTO 제거 완료

### 삭제된 DTO
- [x] `dto/user/LogoutRequestDto.java` - HttpOnly 쿠키 기반으로 마이그레이션되어 미사용
- [x] `dto/auth/TokenRefreshRequestDto.java` - HttpOnly 쿠키 기반으로 마이그레이션되어 미사용

### 삭제 이유
이전 토큰 기반 인증 방식에서는 클라이언트가 refreshToken을 Request Body로 전송했으나,
현재는 HttpOnly 쿠키를 통해 자동으로 전송되므로 해당 DTO가 불필요해짐

---

## 작업 완료일: 2026-01-14
