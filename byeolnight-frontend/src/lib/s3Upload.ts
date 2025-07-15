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
    const formData = new FormData();
    formData.append('file', file);

    const response = await axios.post('/files/upload-image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
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