import { useEffect, useState } from 'react';
import axios from '../lib/axios';

interface UserSummary {
  id: number;
  email: string;
  nickname: string;
  phone: string;
  role: string;
  status: 'ACTIVE' | 'BANNED' | 'SUSPENDED' | 'WITHDRAWN';
}

export default function AdminUserPage() {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchUsers = async () => {
    try {
      const res = await axios.get('/admin/users');
      setUsers(res.data.data);
    } catch (err) {
      console.error('사용자 목록 조회 실패', err);
    } finally {
      setLoading(false);
    }
  };

  const handleLock = async (id: number) => {
    try {
      await axios.patch(`/admin/users/${id}/lock`);
      fetchUsers();
    } catch {
      alert('계정 잠금 실패');
    }
  };

  const handleStatusChange = async (id: number, status: string) => {
    const reason = prompt(`사유를 입력하세요 (${status})`);
    if (!reason) return;
    try {
      await axios.patch(`/admin/users/${id}/status`, { status, reason });
      fetchUsers();
    } catch {
      alert('상태 변경 실패');
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    try {
      await axios.delete(`/admin/users/${id}`);
      fetchUsers();
    } catch {
      alert('강제 탈퇴 실패');
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white px-6 py-12">
      <div className="max-w-6xl mx-auto">
        <h2 className="text-3xl font-bold mb-8 drop-shadow-glow text-center">🔐 관리자 - 사용자 관리</h2>

        {loading ? (
          <p className="text-center text-gray-400">불러오는 중...</p>
        ) : (
          <table className="w-full text-sm border border-gray-600 bg-[#1f2336]/80 backdrop-blur rounded-xl overflow-hidden">
            <thead className="bg-[#2a2e45] text-gray-300">
              <tr>
                <th className="p-3">ID</th>
                <th className="p-3">이메일</th>
                <th className="p-3">닉네임</th>
                <th className="p-3">전화번호</th>
                <th className="p-3">권한</th>
                <th className="p-3">상태</th>
                <th className="p-3">조치</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id} className="border-t border-gray-700">
                  <td className="p-3 text-center">{user.id}</td>
                  <td className="p-3">{user.email}</td>
                  <td className="p-3">{user.nickname}</td>
                  <td className="p-3">{user.phone}</td>
                  <td className="p-3 text-center">{user.role}</td>
                  <td className="p-3 text-center">{user.status}</td>
                  <td className="p-3 text-center space-x-2">
                    <button
                      onClick={() => handleLock(user.id)}
                      className="bg-yellow-500 hover:bg-yellow-600 text-black px-2 py-1 rounded text-xs"
                    >
                      잠금
                    </button>
                    <button
                      onClick={() => handleStatusChange(user.id, 'SUSPENDED')}
                      className="bg-red-500 hover:bg-red-600 px-2 py-1 rounded text-xs"
                    >
                      정지
                    </button>
                    <button
                      onClick={() => handleDelete(user.id)}
                      className="bg-gray-600 hover:bg-gray-700 px-2 py-1 rounded text-xs"
                    >
                      탈퇴
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
