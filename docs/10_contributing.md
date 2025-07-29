# 10. 기여 방법 (Contributing Guide)

> 오픈소스로 공개 시, 외부 기여자들이 프로젝트에 참여할 수 있도록 작성한 문서입니다.

## 👋 환영합니다!

별 헤는 밤(byeolnight)은 누구나 기여할 수 있는 오픈 프로젝트를 지향합니다.
기여 전 아래 내용을 참고해주세요.

## ✅ 브랜치 전략

* `main`: 배포용 브랜치
* `develop`: 개발 중인 기능 통합 브랜치
* `feature/이름`: 기능별 단일 브랜치 (예: `feature/post-like`)

## 🛠️ 개발 환경

* Java 21, Gradle 8+
* Docker / Docker Compose
* 프론트엔드: Node.js 20, React 18

## 💬 기여 방식

1. Issue 또는 Discussion에 제안 등록
2. `feature/` 브랜치에서 개발 후 Pull Request
3. 최소 1명 이상의 리뷰 승인 후 머지

## 📑 커밋 컨벤션 (예시)

```
feat: 게시글 좋아요 기능 추가
fix: 토큰 재발급 오류 수정
refactor: 인증 로직 개선
chore: 주석 정리 및 패키지 구조 정리
docs: README 문서 수정
```

## 🔎 코드 컨벤션

* Java: Google Java Style Guide + 사내 룰 기반
* 네이밍: camelCase, 클래스는 PascalCase
* Controller는 API 명세 기반 URL 구성

## 📄 라이선스

MIT 라이선스를 기반으로 하며, 자유롭게 사용/수정/배포 가능합니다.
단, 2차 배포 시 출처 표기 바랍니다.

---

감사합니다! 언제든 Pull Request를 보내주세요 🚀
