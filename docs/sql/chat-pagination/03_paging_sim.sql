-- =====================================================================
-- 커서 페이지네이션 시뮬레이션: 200페이지를 실제로 넘겨본다.
--
-- before : 현재 구현 (필터 id <, 정렬 timestamp DESC)  ← 커서 키 != 정렬 키
-- fixed  : 수정본     (필터 id <, 정렬 id DESC)         ← 커서 키 == 정렬 키
--
-- 두 방식 모두 1페이지는 동일하게 id DESC로 시작한다.
-- =====================================================================

USE byeolnight;

DROP TABLE IF EXISTS sim_seen;
CREATE TABLE sim_seen (id BIGINT PRIMARY KEY, page INT);

DROP TABLE IF EXISTS sim_page;
CREATE TABLE sim_page (id BIGINT PRIMARY KEY, ts DATETIME(6));

DROP TABLE IF EXISTS sim_result;
CREATE TABLE sim_result (
    mode VARCHAR(20), pages INT, rows_returned INT, duplicated INT, lost INT
);

DROP PROCEDURE IF EXISTS simulate_paging;
DELIMITER $$
CREATE PROCEDURE simulate_paging(IN p_mode VARCHAR(20), IN p_pages INT)
BEGIN
    DECLARE v_cursor BIGINT;
    DECLARE v_next BIGINT;
    DECLARE v_i INT DEFAULT 0;
    DECLARE v_dup INT DEFAULT 0;
    DECLARE v_lost INT DEFAULT 0;
    DECLARE v_rows INT DEFAULT 0;
    DECLARE v_page_dup INT;
    DECLARE v_page_lost INT;

    TRUNCATE sim_seen;

    -- 1페이지 (양쪽 공통): findRecentByRoomIdOrderByIdDesc
    INSERT INTO sim_seen (id, page)
    SELECT id, 1 FROM (
        SELECT id FROM chat_messages WHERE room_id = 'public' ORDER BY id DESC LIMIT 30
    ) t;
    SET v_cursor = (SELECT MIN(id) FROM sim_seen);
    SET v_rows = 30;

    WHILE v_i < p_pages DO
        TRUNCATE sim_page;

        IF p_mode = 'before' THEN
            INSERT INTO sim_page (id, ts)
            SELECT id, `timestamp` FROM chat_messages
            WHERE room_id = 'public' AND id < v_cursor
            ORDER BY `timestamp` DESC LIMIT 30;
        ELSE
            INSERT INTO sim_page (id, ts)
            SELECT id, `timestamp` FROM chat_messages
            WHERE room_id = 'public' AND id < v_cursor
            ORDER BY id DESC LIMIT 30;
        END IF;

        -- 중복: 이미 본 행이 또 나왔는가
        SELECT COUNT(*) INTO v_page_dup
        FROM sim_page p JOIN sim_seen s ON p.id = s.id;
        SET v_dup = v_dup + v_page_dup;
        SET v_rows = v_rows + (SELECT COUNT(*) FROM sim_page);

        -- 다음 커서 = 화면 맨 위(가장 오래된) 메시지의 id.
        -- 서비스가 reverse() 하므로 정렬키 기준 마지막 행이 화면 첫 줄이 된다.
        IF p_mode = 'before' THEN
            SET v_next = (SELECT id FROM sim_page ORDER BY ts ASC LIMIT 1);
        ELSE
            SET v_next = (SELECT MIN(id) FROM sim_page);
        END IF;

        -- 누락: id가 (v_next, v_cursor) 구간인데 이번 페이지에 안 나온 행.
        -- 다음 조회부터는 id < v_next 이므로 영구히 조회되지 않는다.
        SELECT COUNT(*) INTO v_page_lost
        FROM chat_messages c
        WHERE c.room_id = 'public' AND c.id < v_cursor AND c.id > v_next
          AND c.id NOT IN (SELECT id FROM sim_page);
        SET v_lost = v_lost + v_page_lost;

        INSERT IGNORE INTO sim_seen (id, page) SELECT id, v_i + 2 FROM sim_page;

        SET v_cursor = v_next;
        SET v_i = v_i + 1;
    END WHILE;

    INSERT INTO sim_result VALUES (p_mode, p_pages, v_rows, v_dup, v_lost);
END$$
DELIMITER ;

CALL simulate_paging('before', 200);
CALL simulate_paging('fixed',  200);

SELECT
    mode          AS `방식`,
    pages         AS `페이지수`,
    rows_returned AS `반환행수`,
    duplicated    AS `중복`,
    lost          AS `영구누락`
FROM sim_result;
