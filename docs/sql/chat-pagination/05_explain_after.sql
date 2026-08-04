USE byeolnight;

SELECT '### 1. 정렬만 id DESC로 바꾸고, 인덱스는 그대로 ###' AS ``;

SELECT '--- public (전체의 80%) ---' AS ``;
EXPLAIN ANALYZE
SELECT * FROM chat_messages
WHERE room_id = 'public' AND id < 150000
ORDER BY id DESC LIMIT 30;

SELECT '--- room-15 (전체의 1%, 메시지 적은 방) ---' AS ``;
EXPLAIN ANALYZE
SELECT * FROM chat_messages
WHERE room_id = 'room-15' AND id < 150000
ORDER BY id DESC LIMIT 30;

SELECT '### 2. (room_id, id DESC) 인덱스 추가 후 ###' AS ``;
CREATE INDEX idx_chat_room_id ON chat_messages (room_id, id DESC);
ANALYZE TABLE chat_messages;

SELECT '--- public ---' AS ``;
EXPLAIN
SELECT * FROM chat_messages
WHERE room_id = 'public' AND id < 150000
ORDER BY id DESC LIMIT 30;

EXPLAIN ANALYZE
SELECT * FROM chat_messages
WHERE room_id = 'public' AND id < 150000
ORDER BY id DESC LIMIT 30;

SELECT '--- room-15 ---' AS ``;
EXPLAIN ANALYZE
SELECT * FROM chat_messages
WHERE room_id = 'room-15' AND id < 150000
ORDER BY id DESC LIMIT 30;

SELECT '--- 첫 페이지 (커서 없음) ---' AS ``;
EXPLAIN ANALYZE
SELECT * FROM chat_messages
WHERE room_id = 'public'
ORDER BY id DESC LIMIT 30;

SELECT '### 3. 커서를 더 깊이 넣었을 때 (id < 20000) ###' AS ``;
SELECT '--- 수정 전 방식 ---' AS ``;
EXPLAIN ANALYZE
SELECT * FROM chat_messages
WHERE room_id = 'public' AND id < 20000
ORDER BY `timestamp` DESC LIMIT 30;

SELECT '--- 수정 후 방식 ---' AS ``;
EXPLAIN ANALYZE
SELECT * FROM chat_messages
WHERE room_id = 'public' AND id < 20000
ORDER BY id DESC LIMIT 30;
