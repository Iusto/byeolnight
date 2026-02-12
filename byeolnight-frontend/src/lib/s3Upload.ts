import axios from './axios';
import { diagnoseUploadFailure } from '../utils/browserCompatibility';
import { getErrorMessage } from '../types/api';
import type { PresignedUrlResponse } from '../types/file';

export interface UploadedImageResponse {
  url: string;
  s3Key: string;
  originalName: string;
  contentType: string;
}

/**
 * 이미지를 S3에 업로드하는 함수
 * @param file 업로드할 파일
 * @param needsModeration 검열이 필요한지 여부 (기본값: true)
 * @returns 업로드된 이미지 정보
 */
export const uploadImage = async (file: File, needsModeration = true): Promise<UploadedImageResponse> => {
  try {
    // 파일 유효성 검사
    if (!file || !(file instanceof File)) {
      throw new Error('유효한 파일이 아닙니다.');
    }

    // 파일 크기 체크 (10MB 제한)
    if (file.size > 10 * 1024 * 1024) {
      throw new Error('파일 크기는 10MB를 초과할 수 없습니다. 이미지를 압축하거나 크기를 줄여주세요.');
    }

    // 1. Presigned URL 요청
    let response;
    try {
      response = await axios.post('/files/presigned-url', null, {
        params: {
          filename: file.name,
          contentType: file.type
        },
        timeout: 15000 // 15초 타임아웃
      });
    } catch (presignedError: unknown) {
      const errorMessage = getErrorMessage(presignedError);

      if (errorMessage.includes('Network') || errorMessage.includes('네트워크')) {
        throw new Error('네트워크 연결을 확인해주세요. 인터넷 연결이 불안정하거나 서버에 접근할 수 없습니다.');
      }

      if (errorMessage.includes('timeout') || errorMessage.includes('시간 초과')) {
        throw new Error('서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.');
      }

      if (errorMessage.includes('CORS') || errorMessage.includes('Failed to fetch')) {
        throw new Error('브라우저 보안 정책으로 인해 업로드가 차단되었습니다. 다른 브라우저를 사용하거나 시크릿 모드를 시도해보세요.');
      }

      throw new Error(`업로드 준비 실패: ${errorMessage}`);
    }

    const presignedData: PresignedUrlResponse = response.data.data;

    if (!presignedData || !presignedData.uploadUrl) {
      throw new Error('Presigned URL을 받지 못했습니다.');
    }

    // 2. S3에 직접 업로드
    let uploadResponse;
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 30000); // 30초 타임아웃

      uploadResponse = await fetch(presignedData.uploadUrl, {
        method: 'PUT',
        body: file,
        headers: {
          'Content-Type': presignedData.contentType || file.type
        },
        signal: controller.signal
      });

      clearTimeout(timeoutId);
    } catch (uploadError: unknown) {
      // AbortError (타임아웃)
      if (uploadError instanceof Error && uploadError.name === 'AbortError') {
        throw new Error('파일 업로드 시간이 초과되었습니다. 파일 크기를 줄이거나 네트워크 연결을 확인해주세요.');
      }

      const errorMessage = getErrorMessage(uploadError);

      if (errorMessage.includes('Failed to fetch') || errorMessage.includes('fetch')) {
        throw new Error('파일 업로드 중 네트워크 오류가 발생했습니다. 인터넷 연결을 확인하고 다시 시도해주세요.');
      }

      throw new Error(`파일 업로드 실패: ${errorMessage}`);
    }

    if (!uploadResponse.ok) {
      throw new Error(`업로드 실패 (${uploadResponse.status}): ${uploadResponse.statusText}`);
    }

    // 3. 검열이 필요한 경우 검사 요청
    if (needsModeration) {
      let moderationResult;

      try {
        const moderationResponse = await axios.post('/files/moderate-url', null, {
          params: {
            imageUrl: presignedData.url,
            s3Key: presignedData.s3Key
          },
          timeout: 20000 // 20초 타임아웃
        });

        moderationResult = moderationResponse.data;
      } catch (networkErr: unknown) {
        // 검열 실패 시 S3에서 이미지 삭제
        try {
          await axios.delete('/files/delete', {
            params: { s3Key: presignedData.s3Key },
            timeout: 10000
          });
        } catch {
          // 삭제 실패는 무시 (스케줄러가 고아 이미지 정리)
        }

        const errorMessage = getErrorMessage(networkErr);
        if (errorMessage.includes('Failed to fetch') || errorMessage.includes('Network')) {
          throw new Error('이미지 검열 중 네트워크 오류가 발생했습니다. 다른 이미지를 사용해주세요.');
        }
        throw new Error('이미지 검열 중 오류가 발생했습니다. 다른 이미지를 사용해주세요.');
      }

      // 검열 결과 확인
      if (moderationResult?.data) {
        const { status, safe } = moderationResult.data;

        if (status === 'error' || safe === false) {
          throw new Error('부적절한 이미지가 감지되었습니다. 다른 이미지를 사용해주세요.');
        }
      } else {
        throw new Error('이미지 검열 응답이 올바르지 않습니다.');
      }
    }

    // 4. 평문 CloudFront URL 반환 (DB 저장용)
    return {
      url: presignedData.url,
      s3Key: presignedData.s3Key,
      originalName: presignedData.originalName,
      contentType: presignedData.contentType
    };
  } catch (error: unknown) {
    // 브라우저 호환성 진단 수행
    try {
      const diagnosis = await diagnoseUploadFailure(error);

      if (diagnosis.solutions.length > 0) {
        const enhancedMessage = `${diagnosis.diagnosis}\n\n해결 방법:\n${diagnosis.solutions.map(s => `- ${s}`).join('\n')}`;
        throw new Error(enhancedMessage);
      }
    } catch (diagnosisError) {
      // 진단 자체가 실패한 경우 원본 에러를 그대로 전달
      if (diagnosisError instanceof Error && diagnosisError.message.includes('해결 방법')) {
        throw diagnosisError;
      }
    }

    if (error instanceof Error) {
      throw error;
    }

    throw new Error(getErrorMessage(error));
  }
};
