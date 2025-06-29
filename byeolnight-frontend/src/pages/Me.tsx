import { useAuth } from '../contexts/AuthContext';
import { Link } from 'react-router-dom';

export default function Me() {
  const { user, loading } = useAuth();

  if (loading) return <div className="text-white p-8">로딩 중...</div>;
  if (!user) return <div className="text-white p-8">로그인이 필요합니다.</div>;

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex justify-center pt-20 text-white">
      <div className="w-full max-w-lg bg-[#1f2336] p-8 rounded-xl shadow-lg">
        <h2 className="text-2xl font-bold mb-6">🙋 내 정보</h2>
        <ul className="space-y-4 text-base">
          <li><strong>이메일:</strong> {user.email}</li>
          <li><strong>닉네임:</strong> {user.nickname}</li>
          <li><strong>전화번호:</strong> {user.phone}</li>
          <li><strong>권한:</strong> {user.role}</li>
        </ul>
        <div className="mt-8 flex justify-between">
          <Link 
            to="/profile/edit" 
            className="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded transition-colors font-medium"
          >
            프로필 수정
          </Link>
          <Link 
            to="/password-change" 
            className="bg-gray-500 hover:bg-gray-600 text-white px-4 py-2 rounded transition-colors font-medium"
          >
            비밀번호 변경
          </Link>
        </div>
      </div>
    </div>
  );
}
