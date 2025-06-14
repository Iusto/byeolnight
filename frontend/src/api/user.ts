import axiosInstance from '@/lib/axiosInstance'

export const fetchCurrentUser = async () => {
  const res = await axiosInstance.get('/users/me') // 경로는 실제 백엔드에 맞춰서 수정
  return res.data
}