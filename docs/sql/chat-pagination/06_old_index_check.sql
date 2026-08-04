USE byeolnight;

-- idx_chat_room_timestamp (is_blinded, room_id, timestamp) 를 실제로 쓰는 쿼리가 있는지 확인.
-- 채팅 조회에는 안 쓰이는 것이 이미 확인됐고, 남은 후보는 관리자 블라인드 쿼리다.

SELECT '--- countByIsBlindedTrue() ---' AS ``;
EXPLAIN ANALYZE SELECT COUNT(*) FROM chat_messages WHERE is_blinded = 1;

SELECT '--- findByIsBlindedTrueOrderByBlindedAtDesc() ---' AS ``;
EXPLAIN ANALYZE
SELECT * FROM chat_messages WHERE is_blinded = 1 ORDER BY blinded_at DESC LIMIT 50;

SELECT '--- countDistinctSenderByTimestampAfter() ---' AS ``;
EXPLAIN ANALYZE
SELECT COUNT(DISTINCT sender) FROM chat_messages
WHERE `timestamp` >= '2025-01-04 00:00:00';

SELECT '=== idx_chat_room_timestamp 제거 후 재측정 ===' AS ``;
ALTER TABLE chat_messages DROP INDEX idx_chat_room_timestamp;
ANALYZE TABLE chat_messages;

SELECT '--- countByIsBlindedTrue() ---' AS ``;
EXPLAIN ANALYZE SELECT COUNT(*) FROM chat_messages WHERE is_blinded = 1;

SELECT '--- findByIsBlindedTrueOrderByBlindedAtDesc() ---' AS ``;
EXPLAIN ANALYZE
SELECT * FROM chat_messages WHERE is_blinded = 1 ORDER BY blinded_at DESC LIMIT 50;

SELECT '--- 채팅 다음 페이지 (수정 후 쿼리) ---' AS ``;
EXPLAIN ANALYZE
SELECT * FROM chat_messages
WHERE room_id = 'room-15' AND id < 150000
ORDER BY id DESC LIMIT 30;
