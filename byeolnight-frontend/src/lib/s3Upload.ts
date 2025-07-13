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

    // 2. S3에 직접 업로드 (CORS 헤더 명시)
    await axios.put(uploadUrl, file, {
      headers: {
        'Content-Type': contentType,
        'x-amz-acl': 'public-read'
      },
      withCredentials: false // S3 업로드 시 쿠키 제외
    });

    return url;
  } catch (error) {
    console.error('S3 업로드 실패:', error);
    throw new Error('이미지 업로드에 실패했습니다.');
  }
};