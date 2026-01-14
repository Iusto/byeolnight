/**
 * S3 Presigned URL 응답
 */
export interface PresignedUrlResponse {
  uploadUrl: string;
  url: string;
  s3Key: string;
  originalName: string;
  contentType: string;
}

/**
 * 이미지 검열 결과
 */
export interface ModerationResult {
  status: 'completed' | 'error' | 'skipped';
  isSafe: boolean;
  message: string;
  url?: string;
  s3Key?: string;
  originalName?: string;
  contentType?: string;
}

/**
 * CloudFront 이미지 조회 URL 응답
 */
export interface ViewUrlResponse {
  viewUrl: string;
  s3Key: string;
}

/**
 * 이미지 업로드 결과
 */
export interface UploadResult {
  url: string;
  s3Key: string;
  originalName: string;
  contentType: string;
  size: number;
  validated: boolean;
}
