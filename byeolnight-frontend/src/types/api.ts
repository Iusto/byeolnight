import { AxiosError } from 'axios';

/**
 * API 공통 응답 타입
 */
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

/**
 * API 에러 타입
 */
export interface ApiError {
  message: string;
  code?: string;
  status?: number;
}

/**
 * Axios 에러 타입 가드
 */
export function isAxiosError(error: unknown): error is AxiosError<ApiResponse<unknown>> {
  return (error as AxiosError)?.isAxiosError === true;
}

/**
 * 에러에서 메시지 추출
 */
export function getErrorMessage(error: unknown): string {
  if (isAxiosError(error)) {
    const responseMessage = error.response?.data?.message;
    if (responseMessage) {
      return responseMessage;
    }
    return error.message || '오류가 발생했습니다.';
  }
  if (error instanceof Error) {
    return error.message;
  }
  return '알 수 없는 오류가 발생했습니다.';
}
