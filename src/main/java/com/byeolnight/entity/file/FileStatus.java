package com.byeolnight.entity.file;

/**
 * 파일 상태를 나타내는 enum
 *
 * - PENDING: Presigned URL 발급 후 아직 게시글에 연결되지 않은 상태
 * - CONFIRMED: 게시글 저장 시 연결이 확인된 상태
 */
public enum FileStatus {
    PENDING,
    CONFIRMED
}