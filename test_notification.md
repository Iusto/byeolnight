# 알림 테스트 방법

## 1. 테스트 알림 생성
브라우저에서 다음 URL 접속 (로그인 상태에서):
```
http://localhost:8080/api/notifications/test
```

## 2. 데이터베이스 확인
MySQL에서 다음 쿼리 실행:
```sql
-- 알림 테이블 확인
SELECT * FROM notification ORDER BY created_at DESC LIMIT 5;

-- 쪽지 테이블 확인  
SELECT * FROM message ORDER BY created_at DESC LIMIT 5;
```

## 3. 프론트엔드 테스트
1. 🔔 버튼 클릭
2. 브라우저 콘솔에서 로그 확인:
   - "읽지 않은 알림 개수: X"
   - "읽지 않은 알림 목록: [...]"
3. Network 탭에서 API 응답 확인

## 4. 서버 로그 확인
서버 콘솔에서 다음 로그 확인:
- "읽지 않은 알림 조회 - userId: X"
- "조회된 읽지 않은 알림 수: X"
- "알림: NEW_MESSAGE, 제목: ..., 생성시간: ..."