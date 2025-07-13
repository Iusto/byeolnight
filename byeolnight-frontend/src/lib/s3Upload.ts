import axios from './axios';

export interface PresignedUrlResponse {
  uploadUrl: string;
  url: string;
  s3Key: string;
  originalName: string;
  contentType: string;
}

export const uploadImageToS3 = async (file: File): Promise<string> => {
  try {
    // 1. Presigned URL 요청
    const response = await axios.post('/files/presigned-url', {
      filename: file.name
    });

    const { uploadUrl, url, contentType }: PresignedUrlResponse = response.data.data;

    // 2. S3에 직접 업로드 (fetch 사용)
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