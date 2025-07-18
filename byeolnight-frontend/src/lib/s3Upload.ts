import axios from './axios';

export interface PresignedUrlResponse {
  uploadUrl: string;
  url: string;
  s3Key: string;
  originalName: string;
  contentType: string;
}

export interface ValidatedUploadResponse {
  url: string;
  s3Key: string;
  originalName: string;
  contentType: string;
  size?: string;
  validated?: boolean;
}

/**
 * Presigned URL을 요청하고 S3에 직접 업로드하는 함수
 * @param file 업로드할 파일
 * @returns 업로드된 이미지 정보
 */
export const uploadImageToS3 = async (file: File): Promise<PresignedUrlResponse> => {
  try {
    // 파일 크기 체크 (10MB 제한)
    if (file.size > 10 * 1024 * 1024) {
      throw new Error('파일 크기는 10MB를 초과할 수 없습니다.');
    }
    
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

    return presignedData;
  } catch (error) {
    console.error('S3 업로드 실패:', error);
    throw error;
  }
};

/**
 * 이미지를 업로드하고 검증까지 완료하는 통합 함수
 * @param file 업로드할 파일
 * @returns 검증된 이미지 정보
 */
export const uploadImage = async (file: File): Promise<ValidatedUploadResponse> => {
  try {
    // 파일 유효성 검사
    if (!file || !(file instanceof File)) {
      throw new Error('유효한 파일이 아닙니다.');
    }
    
    console.log('업로드 파일 정보:', {
      name: file.name,
      type: file.type,
      size: file.size,
      lastModified: new Date(file.lastModified).toISOString()
    });
    
    // 1. S3에 업로드
    const presignedData = await uploadImageToS3(file);
    
    // 2. 업로드된 이미지 검증
    const validationResponse = await axios.post('/files/validate-image', null, {
      params: { imageUrl: presignedData.url }
    });
    
    const validationResult = validationResponse.data.data;
    
    if (!validationResult.isValid) {
      throw new Error('부적절한 이미지가 감지되어 삭제되었습니다.');
    }
    
    // 3. 검증 통과한 이미지 정보 반환
    return {
      url: presignedData.url,
      s3Key: presignedData.s3Key,
      originalName: presignedData.originalName,
      contentType: presignedData.contentType,
      validated: true
    };
  } catch (error: any) {
    console.error('이미지 업로드 및 검증 실패:', error);
    if (error.response?.data?.message) {
      throw new Error(error.response.data.message);
    }
    throw error;
  }
};

/**
 * 이전 버전과의 호환성을 위한 함수 (직접 업로드 방식)
 * @deprecated 새로운 uploadImage 함수를 사용하세요
 */
export const uploadImageWithValidation = async (file: File): Promise<string> => {
  try {
    const result = await uploadImage(file);
    return result.url;
  } catch (error: any) {
    console.error('이미지 업로드 중 오류:', error);
    throw new Error(error.message || '이미지 업로드 중 오류가 발생했습니다.');
  }
};