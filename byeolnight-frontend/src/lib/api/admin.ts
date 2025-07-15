import axios from '../axios';

// 닉네임 변경권 수여
export const grantNicknameChangeTicket = async (userId: number) => {
  const response = await axios.post(`/admin/users/${userId}/nickname-change-ticket`);
  return response.data;
};