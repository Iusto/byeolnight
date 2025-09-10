import axios from '../axios';
import type { 
  Suggestion, 
  CreateSuggestionRequest, 
  SuggestionListResponse,
  SuggestionCategory,
  SuggestionStatus 
} from '../../types/suggestion';

// 건의사항 목록 조회 (공개)
export const getSuggestions = async (params: {
  category?: SuggestionCategory;
  status?: SuggestionStatus;
  page?: number;
  size?: number;
  sort?: string;
  direction?: string;
}): Promise<SuggestionListResponse> => {
  const response = await axios.get('/public/suggestions', { params });
  return response.data.data;
};

// 건의사항 상세 조회 (회원 전용)
export const getSuggestion = async (id: number): Promise<Suggestion> => {
  const response = await axios.get(`/member/suggestions/${id}`);
  return response.data.data;
};

// 건의사항 작성 (회원 전용)
export const createSuggestion = async (data: CreateSuggestionRequest): Promise<Suggestion> => {
  const response = await axios.post('/member/suggestions', data);
  return response.data.data;
};

// 건의사항 수정 (회원 전용)
export const updateSuggestion = async (id: number, data: CreateSuggestionRequest): Promise<Suggestion> => {
  const response = await axios.put(`/member/suggestions/${id}`, data);
  return response.data.data;
};

// 건의사항 삭제 (회원 전용)
export const deleteSuggestion = async (id: number): Promise<void> => {
  await axios.delete(`/member/suggestions/${id}`);
};

// 내 건의사항 조회 (회원 전용)
export const getMySuggestions = async (params: {
  page?: number;
  size?: number;
}): Promise<SuggestionListResponse> => {
  const response = await axios.get('/member/suggestions/my', { params });
  return response.data.data;
};

// 관리자 답변 추가
export const addAdminResponse = async (id: number, data: {
  response: string;
  status?: SuggestionStatus;
}): Promise<Suggestion> => {
  const response = await axios.post(`/member/suggestions/${id}/admin-response`, data);
  return response.data.data;
};

// 건의사항 상태 변경 (관리자 전용)
export const updateSuggestionStatus = async (id: number, status: SuggestionStatus): Promise<Suggestion> => {
  const response = await axios.patch(`/member/suggestions/${id}/status`, { status });
  return response.data.data;
};