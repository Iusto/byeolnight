-- 뉴스와 별빛시네마는 품질 필터를 통과한 뒤 즉시 공개한다.
-- 기존 검수 대기 게시물도 동일한 정책에 맞춰 공개 상태로 전환한다.
UPDATE `posts`
SET `blinded` = 0,
    `blind_type` = NULL,
    `blinded_at` = NULL,
    `blinded_by_admin_id` = NULL
WHERE `blind_type` = 'PENDING_REVIEW'
  AND `category` IN ('NEWS', 'STARLIGHT_CINEMA')
  AND `is_deleted` = 0;
