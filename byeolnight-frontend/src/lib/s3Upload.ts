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
export const uploadImage = async (file: File, needsModeration = false): Promise<UploadedImageResponse> => {
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
    const response = await axios.post('/api/files/presigned-url', null, {
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
    
    // 3. 검열이 필요한 경우에만 검사 요청 (결과 기다리지 않음)
    if (needsModeration) {
      axios.post('/api/files/check-image', null, {
        params: { 
          imageUrl: presignedData.url,
          needsModeration: true
        }
      }).catch(err => console.error('이미지 검사 요청 실패:', err));
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