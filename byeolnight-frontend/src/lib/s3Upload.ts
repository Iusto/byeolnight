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
    
    const formData = new FormData();
    formData.append('file', file);

    // Axios는 FormData를 사용할 때 자동으로 Content-Type을 설정하므로 명시적으로 설정하지 않음
    const response = await axios.post('/files/upload-image', formData);

    return response.data.data.url;
  } catch (error: any) {
    if (error.response?.data?.message) {
      throw new Error(error.response.data.message);
    }
    throw new Error('이미지 업로드 중 오류가 발생했습니다.');
  }
};