-- Hibernate가 기존 MySQL ENUM에 새 Java enum 값을 자동으로 추가하지 못하므로
-- 문자열 컬럼으로 전환해 PENDING_REVIEW 및 이후 검수 상태를 안전하게 저장한다.
ALTER TABLE `posts`
    MODIFY COLUMN `blind_type` VARCHAR(32) NULL;
