-- 더미 데이터 30만 건 — 실제 서비스 형태를 최대한 따른다.
--
-- [방 분포] 실제 앱은 'public' 방 하나에 대부분의 메시지가 모인다(Swagger 기본값도 public).
--           80%를 public에, 나머지를 19개 방에 분산한다.
--
-- [시각] 전역으로 1초 간격. 다만 1% 행은 +2.5초를 더해 "뒤에 삽입된 행보다
--        timestamp가 늦은" 상태를 만든다.
--        이는 조작이 아니라 실제 발생하는 상황을 재현한 것이다:
--        id는 INSERT 시점에 DB가 채번하고, timestamp는 @CreatedDate가
--        flush 이전 자바 코드에서 채운다. 두 메시지가 거의 동시에 도착하면
--        채번 순서와 @CreatedDate 순서가 어긋날 수 있다.
--        이 어긋남이 커서 페이지네이션 버그의 발현 조건이다.

USE byeolnight;

SET SESSION cte_max_recursion_depth = 1000000;

INSERT INTO chat_messages
    (room_id, sender, message, is_blinded, `timestamp`, ip_address)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 300000
)
SELECT
    IF(CRC32(CONCAT('room', n)) % 5 = 0,
       CONCAT('room-', CRC32(CONCAT('r2', n)) % 19 + 1),
       'public'),
    CONCAT('user-', n % 500),
    CONCAT('메시지 본문 ', n),
    IF(n % 97 = 0, 1, 0),
    DATE_ADD('2025-01-01 00:00:00',
             INTERVAL (n * 1000000
                       + IF(CRC32(CONCAT('inv', n)) % 100 = 0, 2500000, 0)) MICROSECOND),
    '127.0.0.1'
FROM seq;

ANALYZE TABLE chat_messages;

SELECT COUNT(*) AS total_rows FROM chat_messages;

SELECT room_id, COUNT(*) AS cnt
FROM chat_messages GROUP BY room_id ORDER BY cnt DESC LIMIT 3;

-- id 순서와 timestamp 순서가 실제로 어긋난 비율 (public 방 기준)
SELECT
    SUM(inverted)                              AS inverted_pairs,
    COUNT(*)                                   AS compared_pairs,
    ROUND(100 * SUM(inverted) / COUNT(*), 3)   AS inverted_pct
FROM (
    SELECT IF(`timestamp` > LEAD(`timestamp`) OVER (ORDER BY id), 1, 0) AS inverted
    FROM chat_messages
    WHERE room_id = 'public'
) t;
