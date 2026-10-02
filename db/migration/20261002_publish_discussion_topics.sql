-- 기존 검수 대기 상태의 자동 토론 주제를 공개한다.
UPDATE `posts`
SET `blinded` = 0,
    `blind_type` = NULL,
    `blinded_at` = NULL,
    `blinded_by_admin_id` = NULL
WHERE `blind_type` = 'PENDING_REVIEW'
  AND `category` = 'DISCUSSION'
  AND `discussion_topic` = 1
  AND `is_deleted` = 0;
