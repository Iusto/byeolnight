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
 * 이미지를 S3에 업로드하는 간단한 함수
 * @param file 업로드할 파일
 * @param needsModeration 검열이 필요한지 여부 (기본값: false)
 * @returns 업로드된 이미지 정보
 */
export const uploadImage = async (file: File, needsModeration = true): Promise<UploadedImageResponse> => {
  // 개발 환경에서 API 경로 로깅
  console.log('이미지 업로드 API 경로:', import.meta.env.VITE_API_BASE_URL || '/api');
  
  try {
    // 파일 유효성 검사
    if (!file || !(file instanceof File)) {
      throw new Error('유효한 파일이 아닙니다.');
    }
    
    // 파일 크기 체크 (10MB 제한으로 변경)
    if (file.size > 10 * 1024 * 1024) {
      throw new Error('파일 크기는 10MB를 초과할 수 없습니다. 이미지를 압축하거나 크기를 줄여주세요.');
    }
    
    console.log('이미지 업로드 시작:', {
      name: file.name,
      type: file.type,
      size: file.size,
      lastModified: new Date(file.lastModified).toISOString(),
      userAgent: navigator.userAgent
    });
    
    // 1. Presigned URL 요청 (상세한 오류 처리 추가)
    let response;
    try {
      console.log('Presigned URL 요청 시작...');
      response = await axios.post('/files/presigned-url', null, {
        params: { 
          filename: file.name,
          contentType: file.type
        },
        timeout: 15000 // 15초 타임아웃
      });
      console.log('Presigned URL 응답:', response.status, response.data);
    } catch (presignedError: unknown) {
      console.error('Presigned URL 요청 실패:', presignedError);

      const errorMessage = getErrorMessage(presignedError);

      // 에러 메시지 기반 분류
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
    
    // 2. S3에 직접 업로드 (상세한 오류 처리 추가)
    let uploadResponse;
    try {
      console.log('S3 업로드 시작:', presignedData.uploadUrl);
      
      // AbortController로 타임아웃 제어
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
      console.log('S3 업로드 응답:', uploadResponse.status, uploadResponse.statusText);
    } catch (uploadError: unknown) {
      console.error('S3 업로드 실패:', uploadError);

      // AbortError (타임아웃)
      if (uploadError instanceof Error && uploadError.name === 'AbortError') {
        throw new Error('파일 업로드 시간이 초과되었습니다. 파일 크기를 줄이거나 네트워크 연결을 확인해주세요.');
      }

      const errorMessage = getErrorMessage(uploadError);

      // 네트워크 오류
      if (errorMessage.includes('Failed to fetch') || errorMessage.includes('fetch')) {
        throw new Error('파일 업로드 중 네트워크 오류가 발생했습니다. 인터넷 연결을 확인하고 다시 시도해주세요.');
      }

      throw new Error(`파일 업로드 실패: ${errorMessage}`);
    }

    if (!uploadResponse.ok) {
      const errorText = await uploadResponse.text().catch(() => '응답 본문 읽기 실패');
      console.error('S3 업로드 실패 응답:', {
        status: uploadResponse.status,
        statusText: uploadResponse.statusText,
        body: errorText
      });
      throw new Error(`업로드 실패 (${uploadResponse.status}): ${uploadResponse.statusText}`);
    }
    
    // 3. 검열이 필요한 경우 검사 요청 (결과 기다림)
    if (needsModeration) {
      console.log('이미지 검열 시작...');
      let moderationResult;

      try {
        // 검열 API 호출 - URL 기반 검열로 변경
        const moderationResponse = await axios.post('/files/moderate-url', null, {
          params: {
            imageUrl: presignedData.url,
            s3Key: presignedData.s3Key
          },
          timeout: 20000 // 20초 타임아웃
        });

        moderationResult = moderationResponse.data;
        console.log('이미지 검열 결과:', moderationResult);
      } catch (networkErr: unknown) {
        console.error('이미지 검열 네트워크 오류:', networkErr);

        // S3에서 이미지 삭제 요청
        try {
          await axios.delete('/files/delete', {
            params: { s3Key: presignedData.s3Key },
            timeout: 10000
          });
          console.log('네트워크 오류로 이미지 삭제 요청:', presignedData.s3Key);
        } catch (deleteErr) {
          console.error('이미지 삭제 실패:', deleteErr);
        }

        const errorMessage = getErrorMessage(networkErr);
        if (errorMessage.includes('Failed to fetch') || errorMessage.includes('Network')) {
          throw new Error('이미지 검열 중 네트워크 오류가 발생했습니다. 다른 이미지를 사용해주세요.');
        }
        throw new Error('이미지 검열 중 오류가 발생했습니다. 다른 이미지를 사용해주세요.');
      }

      // 검열 결과 확인
      if (moderationResult?.data) {
        const { status, safe, message } = moderationResult.data;

        // 부적절한 이미지인 경우 (safe 필드 사용 - Java Lombok이 isSafe를 safe로 직렬화)
        if (status === 'error' || safe === false) {
          throw new Error('부적절한 이미지가 감지되었습니다. 다른 이미지를 사용해주세요.');
        }
      } else {
        // data가 없으면 에러로 처리
        throw new Error('이미지 검열 응답이 올바르지 않습니다.');
      }
    }
    
    // 4. 평문 CloudFront URL 사용 (DB 저장용)
    const plainCloudFrontUrl = presignedData.url; // 이미 평문 CloudFront URL
    
    console.log('이미지 업로드 완료:', plainCloudFrontUrl);
    return {
      url: plainCloudFrontUrl, // 평문 CloudFront URL
      s3Key: presignedData.s3Key,
      originalName: presignedData.originalName,
      contentType: presignedData.contentType
    };
  } catch (error: unknown) {
    console.error('이미지 업로드 전체 실패:', error);

    // 브라우저 호환성 진단 수행
    try {
      const diagnosis = await diagnoseUploadFailure(error);
      console.warn('이미지 업로드 실패 진단:', diagnosis);

      // 진단 결과를 바탕으로 더 상세한 오류 메시지 제공
      if (diagnosis.solutions.length > 0) {
        const enhancedMessage = `${diagnosis.diagnosis}\n\n해결 방법:\n${diagnosis.solutions.map(s => `- ${s}`).join('\n')}`;
        throw new Error(enhancedMessage);
      }
    } catch (diagnosisError) {
      console.warn('진단 실패:', diagnosisError);
    }

    // 이미 처리된 오류 메시지는 그대로 전달
    if (error instanceof Error) {
      throw error;
    }

    // 기본 오류 처리
    throw new Error(getErrorMessage(error));
  }
};