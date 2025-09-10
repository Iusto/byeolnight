import axios from '../axios';
import type { 
  Suggestion, 
  CreateSuggestionRequest, 
  SuggestionListResponse,
  SuggestionCategory,
  SuggestionStatus 
} from '../../types/suggestion';

// 건의사항 목록 조회
export const getSuggestions = async (params: {
  category?: SuggestionCategory;
  status?: SuggestionStatus;
  page?: number;
  size?: number;
  sort?: string;
  direction?: string;
}): Promise<SuggestionListResponse> => {
  const response = await axios.get('/api/suggestions', { params });
  return response.data.data;
};

// 건의사항 상세 조회
export const getSuggestion = async (id: number): Promise<Suggestion> => {
  const response = await axios.get(`/api/suggestions/${id}`);
  return response.data.data;
};

// 건의사항 작성
export const createSuggestion = async (data: CreateSuggestionRequest): Promise<Suggestion> => {
  const response = await axios.post('/api/suggestions', data);
  return response.data.data;
};

// 건의사항 수정
export const updateSuggestion = async (id: number, data: CreateSuggestionRequest): Promise<Suggestion> => {
  const response = await axios.put(`/api/suggestions/${id}`, data);
  return response.data.data;
};

// 건의사항 삭제
export const deleteSuggestion = async (id: number): Promise<void> => {
  await axios.delete(`/api/suggestions/${id}`);
};

// 내 건의사항 조회
export const getMySuggestions = async (params: {
  page?: number;
  size?: number;
}): Promise<SuggestionListResponse> => {
  const response = await axios.get('/api/suggestions/my', { params });
  return response.data.data;
};

// 관리자 답변 추가
export const addAdminResponse = async (id: number, data: {
  response: string;
  status?: SuggestionStatus;
}): Promise<Suggestion> => {
  const response = await axios.post(`/api/suggestions/${id}/admin-response`, data);
  return response.data.data;
};

// 건의사항 상태 변경 (관리자 전용)
export const updateSuggestionStatus = async (id: number, status: SuggestionStatus): Promise<Suggestion> => {
  const response = await axios.patch(`/api/suggestions/${id}/status`, { status });
  return response.data.data;
};