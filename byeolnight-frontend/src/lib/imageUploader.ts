/**
 * @deprecated 이 파일은 더 이상 사용되지 않습니다. 대신 s3Upload.ts의 uploadImage 함수를 사용하세요.
 */

import { uploadImage } from './s3Upload';

// 기존 코드를 호환성을 위해 유지하되, s3Upload.ts의 함수를 재내보내기
export { uploadImage };

// 이 파일은 s3Upload.ts로 통합되었습니다.
// 새로운 코드에서는 s3Upload.ts의 함수를 직접 사용하세요.