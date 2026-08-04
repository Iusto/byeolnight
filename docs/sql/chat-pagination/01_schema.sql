-- 현재 ChatMessage 엔티티가 생성하는 스키마 그대로 재현한다.
-- 인덱스도 수정 전 상태(idx_chat_room_timestamp = is_blinded, room_id, timestamp)를 유지한다.

USE byeolnight;

DROP TABLE IF EXISTS chat_messages;

CREATE TABLE chat_messages (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    room_id     VARCHAR(255) NOT NULL,
    sender      VARCHAR(255) NOT NULL,
    sender_icon VARCHAR(255),
    message     TEXT         NOT NULL,
    ip_address  VARCHAR(45),
    is_blinded  BIT(1)       NOT NULL,
    blinded_by  BIGINT,
    blinded_at  DATETIME(6),
    `timestamp` DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_chat_room_timestamp (is_blinded, room_id, `timestamp`),
    KEY idx_chat_timestamp (`timestamp`)
) ENGINE = InnoDB;
