USE byeolnight;

SELECT '=== 현재 인덱스 상태 ===' AS ``;
SHOW INDEX FROM chat_messages;

SELECT '=== [수정 전] 다음 페이지 조회: 필터 id, 정렬 timestamp ===' AS ``;
EXPLAIN
SELECT * FROM chat_messages
WHERE room_id = 'public' AND id < 150000
ORDER BY `timestamp` DESC LIMIT 30;

EXPLAIN ANALYZE
SELECT * FROM chat_messages
WHERE room_id = 'public' AND id < 150000
ORDER BY `timestamp` DESC LIMIT 30;

SELECT '=== [수정 전] 첫 페이지 조회: 정렬 id ===' AS ``;
EXPLAIN ANALYZE
SELECT * FROM chat_messages
WHERE room_id = 'public'
ORDER BY id DESC LIMIT 30;
