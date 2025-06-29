import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import { Link, useNavigate } from 'react-router-dom';
import StellaIcon from '../components/StellaIcon';
import WithdrawModal from '../components/WithdrawModal';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  blinded: boolean;
  likeCount: number;
}

interface EquippedIcon {
  id: number;
  name: string;
  iconUrl: string;
  grade: string;
  animationClass?: string;
}

export default function MyPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [posts, setPosts] = useState<Post[]>([]);
  const [equippedIcon, setEquippedIcon] = useState<EquippedIcon | null>(null);
  const [loading, setLoading] = useState(true);
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);

  const fetchMyPosts = async () => {
    try {
      const res = await axios.get('/member/posts/mine');
      setPosts(res.data.data.content || res.data.data); // Page or List
    } catch (err) {
      console.error('내 게시글 조회 실패', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchEquippedIcon = async () => {
    try {
      const res = await axios.get('/shop/my-icons');
      const myIcons = res.data.data || [];
      const equipped = myIcons.find((icon: any) => icon.equipped);
      if (equipped) {
        setEquippedIcon({
          id: equipped.iconId,
          name: equipped.name,
          iconUrl: equipped.iconUrl,
          grade: equipped.grade,
          animationClass: equipped.animationClass
        });
      }
    } catch (err) {
      console.error('장착 아이콘 조회 실패', err);
    }
  };

  const handleWithdraw = async (password: string, reason: string) => {
    try {
      await axios.delete('/auth/withdraw', {
        data: {
          password,
          reason
        }
      });
      alert('회원 탈퇴가 완료되었습니다. 그동안 이용해 주셔서 감사합니다.');
      logout();
      navigate('/');
    } catch (err: any) {
      const errorMsg = err?.response?.data?.message || '회원 탈퇴에 실패했습니다.';
      alert(errorMsg);
    }
  };

  useEffect(() => {
    fetchMyPosts();
    if (user?.equippedIconId) {
      fetchEquippedIcon();
    }
  }, [user]);

  if (!user) return <div className="text-white p-8">로그인이 필요합니다.</div>;

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-5xl mx-auto">
        {/* 사용자 정보 섹션 */}
        <div className="text-center mb-8">
          <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 mb-8">
            <div className="flex items-center justify-center gap-3 mb-4">
              <h2 className="text-3xl font-bold drop-shadow-glow">
                {user?.nickname}
              </h2>
              {equippedIcon && (
                <StellaIcon
                  iconUrl={equippedIcon.iconUrl}
                  animationClass={equippedIcon.animationClass}
                  grade={equippedIcon.grade}
                  size="md"
                  showTooltip={true}
                  tooltipText={equippedIcon.name}
                />
              )}
            </div>
            <div className="flex items-center justify-center gap-6 text-sm text-gray-300 mb-4">
              <div className="flex items-center gap-1">
                <span className="text-yellow-400">💰</span>
                <span>{user?.points || 0} 포인트</span>
              </div>
              <div className="flex items-center gap-1">
                <span className="text-blue-400">📝</span>
                <span>{posts.length}개 게시글</span>
              </div>
            </div>
            
            {/* 계정 관리 버튼 */}
            <div className="flex justify-center gap-3">
              <Link 
                to="/profile/edit"
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition text-sm"
              >
                프로필 수정
              </Link>
              <Link 
                to="/password-change"
                className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg transition text-sm"
              >
                비밀번호 변경
              </Link>
              <button
                onClick={() => setShowWithdrawModal(true)}
                className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition text-sm"
              >
                회원 탈퇴
              </button>
            </div>
          </div>
        </div>
        
        <h3 className="text-2xl font-bold mb-6 text-center">🌠 내 활동</h3>

        {loading ? (
          <p className="text-center text-gray-400">불러오는 중...</p>
        ) : posts.length === 0 ? (
          <p className="text-center text-gray-400">작성한 게시글이 없습니다.</p>
        ) : (
          <ul className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            {posts.map((post) => (
              <li
                key={post.id}
                className="bg-[#1f2336]/80 backdrop-blur-md p-5 rounded-xl shadow hover:shadow-purple-700 transition-shadow"
              >
                <Link to={`/posts/${post.id}`} className="block h-full">
                  <h3 className="text-lg font-semibold text-white mb-2">
                    {post.title}{' '}
                    {post.blinded && <span className="text-red-400 text-sm">(블라인드)</span>}
                  </h3>
                  <p className="text-sm text-gray-300 line-clamp-3 mb-2">{post.content}</p>
                  <div className="text-sm text-gray-400">
                    🗂 {post.category} · ❤️ {post.likeCount}
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
        
        {/* 회원탈퇴 모달 */}
        <WithdrawModal 
          isOpen={showWithdrawModal}
          onClose={() => setShowWithdrawModal(false)}
          onConfirm={handleWithdraw}
        />
      </div>
    </div>
  );
}
