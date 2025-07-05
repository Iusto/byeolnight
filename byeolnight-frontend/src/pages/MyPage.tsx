import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import { Link, useNavigate } from 'react-router-dom';
import StellaIcon from '../components/StellaIcon';
import WithdrawModal from '../components/WithdrawModal';
import type { UserIcon, EquippedIcon } from '../types/stellaIcon';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  blinded: boolean;
  likeCount: number;
}



export default function MyPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [posts, setPosts] = useState<Post[]>([]);
  const [equippedIcon, setEquippedIcon] = useState<EquippedIcon | null>(null);
  const [ownedIcons, setOwnedIcons] = useState<UserIcon[]>([]);
  const [loading, setLoading] = useState(true);
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);
  const [activeTab, setActiveTab] = useState<'posts' | 'icons'>('posts');

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

  const fetchOwnedIcons = async () => {
    try {
      // 임시로 빈 배열 사용 (백엔드 API 구현 전)
      setOwnedIcons([]);
    } catch (err) {
      console.error('보유 아이콘 조회 실패', err);
    }
  };

  const fetchEquippedIcon = async () => {
    try {
      // 임시로 null 사용 (백엔드 API 구현 전)
      setEquippedIcon(null);
    } catch (err) {
      console.error('장착 아이콘 조회 실패', err);
    }
  };

  const handleEquipIcon = async (iconId: number) => {
    try {
      // 임시로 시뮬레이션 (백엔드 API 구현 전)
      await new Promise(resolve => setTimeout(resolve, 500));
      alert('아이콘을 장착했습니다! (시뮬레이션)');
    } catch (err: any) {
      alert('아이콘 장착에 실패했습니다.');
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
    if (user) {
      fetchMyPosts();
      fetchEquippedIcon();
      fetchOwnedIcons();
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
                  icon={equippedIcon.icon}
                  size="small"
                />
              )}
            </div>
            <div className="flex items-center justify-center gap-6 text-sm text-gray-300 mb-4">
              <div className="flex items-center gap-1">
                <span className="text-yellow-400">⭐</span>
                <span>{user?.points || 0} 스텔라</span>
              </div>
              <div className="flex items-center gap-1">
                <span className="text-blue-400">📝</span>
                <span>{posts.length}개 게시글</span>
              </div>
              <div className="flex items-center gap-1">
                <span className="text-purple-400">🎨</span>
                <span>{ownedIcons.length}개 아이콘</span>
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
                to="/shop"
                className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition text-sm"
              >
                ⭐ 상점
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
        
        {/* 탭 메뉴 */}
        <div className="flex justify-center gap-2 mb-8">
          <button
            onClick={() => setActiveTab('posts')}
            className={`px-6 py-3 rounded-lg font-medium transition-all duration-200 ${
              activeTab === 'posts'
                ? 'bg-purple-600 text-white'
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            📝 내 게시글
          </button>
          <button
            onClick={() => setActiveTab('icons')}
            className={`px-6 py-3 rounded-lg font-medium transition-all duration-200 ${
              activeTab === 'icons'
                ? 'bg-purple-600 text-white'
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            🎨 보유 아이콘
          </button>
        </div>

        {/* 탭 컨텐츠 */}
        {activeTab === 'posts' ? (
          // 게시글 탭
          loading ? (
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
          )
        ) : (
          // 아이콘 탭
          <div>
            <h3 className="text-xl font-bold mb-6 text-center">🎨 보유중인 스텔라 아이콘</h3>
            {ownedIcons.length === 0 ? (
              <div className="text-center py-12">
                <p className="text-gray-400 mb-4">보유중인 아이콘이 없습니다.</p>
                <Link 
                  to="/shop"
                  className="inline-block bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white px-6 py-3 rounded-lg font-medium transition-all duration-200 transform hover:scale-105"
                >
                  ⭐ 스텔라 상점 가기
                </Link>
              </div>
            ) : (
              <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-4">
                {ownedIcons.map(userIcon => (
                  <div key={userIcon.id} className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-3 hover:bg-[#252842]/80 transition-all duration-300">
                    <StellaIcon
                      icon={userIcon.icon}
                      size="medium"
                      equipped={equippedIcon?.iconId === userIcon.iconId}
                      onClick={() => handleEquipIcon(userIcon.iconId)}
                      showName={true}
                    />
                    <button
                      onClick={() => handleEquipIcon(userIcon.iconId)}
                      disabled={equippedIcon?.iconId === userIcon.iconId}
                      className={`w-full mt-2 py-1 px-2 rounded text-xs font-medium transition-all duration-200 ${
                        equippedIcon?.iconId === userIcon.iconId
                          ? 'bg-green-600/50 text-green-300 cursor-not-allowed'
                          : 'bg-purple-600 hover:bg-purple-700 text-white'
                      }`}
                    >
                      {equippedIcon?.iconId === userIcon.iconId ? '장착됨' : '장착하기'}
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
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
