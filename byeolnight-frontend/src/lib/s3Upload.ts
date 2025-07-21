import axios from './axios';

export interface PresignedUrlResponse {
  uploadUrl: string;
  url: string;
  s3Key: string;
  originalName: string;
  contentType: string;
}

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
    
    // 파일 크기 체크 (10MB 제한)
    if (file.size > 10 * 1024 * 1024) {
      throw new Error('파일 크기는 10MB를 초과할 수 없습니다.');
    }
    
    console.log('이미지 업로드 시작:', {
      name: file.name,
      type: file.type,
      size: file.size,
      lastModified: new Date(file.lastModified).toISOString()
    });
    
    // 1. Presigned URL 요청
    const response = await axios.post('/files/presigned-url', null, {
      params: { 
        filename: file.name,
        contentType: file.type
      }
    });

    const presignedData: PresignedUrlResponse = response.data.data;
    
    if (!presignedData || !presignedData.uploadUrl) {
      throw new Error('Presigned URL을 받지 못했습니다.');
    }
    
    // 2. S3에 직접 업로드
    const uploadResponse = await fetch(presignedData.uploadUrl, {
      method: 'PUT',
      body: file,
      headers: {
        'Content-Type': presignedData.contentType || file.type
      }
    });

    if (!uploadResponse.ok) {
      throw new Error(`업로드 실패: ${uploadResponse.status}`);
    }
    
    // 3. 검열이 필요한 경우 검사 요청 (결과 기다림)
    if (needsModeration) {
      try {
        console.log('이미지 검열 시작...');
        // 검열 API 호출 - 직접 업로드 방식으로 변경
        const formData = new FormData();
        formData.append('file', file);
        formData.append('needsModeration', 'true');
        
        const moderationResponse = await axios.post('/files/moderate-image', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        });
        
        // 검열 결과 확인
        const moderationResult = moderationResponse.data;
        console.log('이미지 검열 결과:', moderationResult);
        
        // 부적절한 이미지인 경우 예외 발생
        if (moderationResult.data && moderationResult.data.isSafe === false) {
          // S3에서 이미지 삭제 요청
          try {
            await axios.delete('/files/delete', {
              params: { s3Key: presignedData.s3Key }
            });
            console.log('부적절한 이미지 삭제 요청:', presignedData.s3Key);
          } catch (deleteErr) {
            console.error('이미지 삭제 실패:', deleteErr);
          }
          
          throw new Error('부적절한 이미지가 감지되었습니다. 다른 이미지를 사용해주세요.');
        }
      } catch (err: any) {
        console.error('이미지 검사 요청 실패:', err);
        // S3에서 이미지 삭제 요청
        try {
          await axios.delete('/files/delete', {
            params: { s3Key: presignedData.s3Key }
          });
          console.log('검열 실패한 이미지 삭제 요청:', presignedData.s3Key);
        } catch (deleteErr) {
          console.error('이미지 삭제 실패:', deleteErr);
        }
        
        if (err.response?.data?.message) {
          throw new Error(err.response.data.message);
        }
        throw new Error('이미지 검열 실패: 부적절한 이미지가 감지되었습니다.');
      }
    }
    
    // 4. 업로드된 이미지 정보 반환
    return {
      url: presignedData.url,
      s3Key: presignedData.s3Key,
      originalName: presignedData.originalName,
      contentType: presignedData.contentType
    };
  } catch (error: any) {
    console.error('이미지 업로드 실패:', error);
    if (error.response?.data?.message) {
      throw new Error(error.response.data.message);
    }
    throw error;
  }
};