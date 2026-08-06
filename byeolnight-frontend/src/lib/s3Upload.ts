import axios from './axios';
import { diagnoseUploadFailure } from '../utils/browserCompatibility';
import { getErrorMessage } from '../types/api';

export interface UploadedImageResponse {
  url: string;
  s3Key: string;
  originalName: string;
  contentType: string;
}

/**
 * 이미지를 서버로 전송해 검열을 통과한 파일만 S3에 공개한다.
 */
export const uploadImage = async (file: File): Promise<UploadedImageResponse> => {
  try {
    if (!file || !(file instanceof File)) {
      throw new Error('유효한 파일이 아닙니다.');
    }

    if (file.size > 10 * 1024 * 1024) {
      throw new Error('파일 크기는 10MB를 초과할 수 없습니다.');
    }

    const formData = new FormData();
    formData.append('file', file);

    const response = await axios.post('/files/upload', formData, {
      timeout: 40000
    });
    const uploadedImage = response.data?.data as UploadedImageResponse | undefined;

    if (!uploadedImage?.url || !uploadedImage.s3Key) {
      throw new Error('이미지 업로드 응답이 올바르지 않습니다.');
    }

    return uploadedImage;
  } catch (error: unknown) {
    try {
      const diagnosis = await diagnoseUploadFailure(error);
      if (diagnosis.solutions.length > 0) {
        const solutions = diagnosis.solutions.map(solution => `- ${solution}`).join('\n');
        throw new Error(`${diagnosis.diagnosis}\n\n해결 방법:\n${solutions}`);
      }
    } catch (diagnosisError) {
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
