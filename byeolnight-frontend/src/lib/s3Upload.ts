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
  size: string;
  validated: string;
}

export const uploadImageToS3 = async (file: File): Promise<string> => {
  try {
    const response = await axios.post('/files/presigned-url', {
      filename: file.name
    });

    const { uploadUrl, url, contentType }: PresignedUrlResponse = response.data.data;

    const uploadResponse = await fetch(uploadUrl, {
      method: 'PUT',
      body: file,
      headers: {
        'Content-Type': contentType
      }
    });

    if (!uploadResponse.ok) {
      throw new Error(`업로드 실패: ${uploadResponse.status}`);
    }

    return url;
  } catch (error) {
    console.error('S3 업로드 실패:', error);
    throw new Error('이미지 업로드에 실패했습니다.');
  }
};

export const uploadImageWithValidation = async (file: File): Promise<string> => {
  try {
    // 파일 크기 체크 (10MB 제한)
    if (file.size > 10 * 1024 * 1024) {
      throw new Error('파일 크기는 10MB를 초과할 수 없습니다.');
    }
    
    // 파일 유효성 추가 검사
    if (!file || !(file instanceof File)) {
      throw new Error('유효한 파일이 아닙니다.');
    }
    
    console.log('업로드 파일 정보:', {
      name: file.name,
      type: file.type,
      size: file.size,
      lastModified: new Date(file.lastModified).toISOString()
    });
    
    const formData = new FormData();
    formData.append('file', file);
    
    // FormData 내용 확인 (디버깅용)
    console.log('FormData 파일 포함 여부:', formData.has('file'));

    // 멀티파트 요청 직접 설정
    const response = await axios.post('/files/upload-image', formData, {
      headers: {
        // Content-Type 헤더를 삭제하여 브라우저가 자동으로 boundary 설정하도록 함
        'Content-Type': undefined
      }
    });

    return response.data.data.url;
  } catch (error: any) {
    if (error.response?.data?.message) {
      throw new Error(error.response.data.message);
    }
    throw new Error('이미지 업로드 중 오류가 발생했습니다.');
  }
};