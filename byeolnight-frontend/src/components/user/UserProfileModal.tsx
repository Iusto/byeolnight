import { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import axios from '../../lib/axios';
import type { SendMessageRequest } from '../types/message';
import { getErrorMessage, isAxiosError } from '../../types/api';

interface UserProfile {
  id: number;
  nickname: string;
  email: string;

  postCount: number;
  commentCount: number;
  attendanceCount: number;
  iconCount: number;
  certificates: Certificate[];

}

interface Certificate {
  id: number;
  title: string;
  description: string;
  iconUrl: string;
  earnedAt: string;
}

interface UserProfileModalProps {
  userId: number;
  isOpen: boolean;
  onClose: () => void;
}

export default function UserProfileModal({ userId, isOpen, onClose }: UserProfileModalProps) {
  const { user } = useAuth();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(false);
  const [showMessageForm, setShowMessageForm] = useState(false);
  const [messageForm, setMessageForm] = useState({
    title: '',
    content: ''
  });
  const [sendingMessage, setSendingMessage] = useState(false);

  useEffect(() => {
    if (isOpen && userId) {
      fetchUserProfile();
    }
  }, [isOpen, userId]);

  const fetchUserProfile = async () => {
    try {
      setLoading(true);
      console.log('프로필 조회 시작:', userId);
      
      const response = await axios.get(`/public/users/${userId}/profile`);
      const data = response.data;
      
      console.log('프로필 API 응답:', data);
      
      if (data && data.success && data.data) {
        console.log('API 응답 데이터:', data.data);
        setProfile({
          id: data.data.id,
          nickname: data.data.nickname,
          email: data.data.email || '',
          postCount: data.data.postCount || 0,
          commentCount: data.data.commentCount || 0,
          attendanceCount: data.data.attendanceCount || 0,
          iconCount: data.data.iconCount || 0,
          certificates: data.data.certificates || [],
        });
      } else {
        console.error('프로필 데이터 없음:', data);
        setProfile(null);
      }
    } catch (error: unknown) {
      console.error('사용자 프로필 조회 실패:', error);
      console.error('에러 상세:', isAxiosError(error) ? error.response?.data : error);
      setProfile(null);
    } finally {
      setLoading(false);
    }
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!messageForm.title.trim() || !messageForm.content.trim()) {
      alert('제목과 내용을 모두 입력해주세요.');
      return;
    }

    try {
      setSendingMessage(true);
      const messageData: SendMessageRequest = {
        receiverId: userId,
        title: messageForm.title,
        content: messageForm.content
      };
      
      const response = await axios.post('/member/messages', messageData);
      if (!response.data.success) {
        throw new Error(response.data.message || '쪽지 전송 실패');
      }
      alert('쪽지가 성공적으로 전송되었습니다!');
      setShowMessageForm(false);
      setMessageForm({ title: '', content: '' });
    } catch (error: unknown) {
      console.error('쪽지 전송 실패:', error);
      alert(getErrorMessage(error));
    } finally {
      setSendingMessage(false);
    }
  };



  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-2 sm:p-4">
      <div className="bg-[#1f2336]/95 backdrop-blur-md rounded-xl border border-purple-500/20 w-full max-w-2xl max-h-[95vh] sm:max-h-[90vh] overflow-y-auto">
        {/* 헤더 */}
        <div className="flex items-center justify-between p-6 border-b border-purple-500/20">
          <h2 className="text-xl font-bold text-white">👤 사용자 프로필</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-purple-600/20 rounded-lg transition-colors text-gray-400 hover:text-white"
          >
            ✕
          </button>
        </div>

        {loading ? (
          <div className="p-8 text-center">
            <div className="text-white text-lg">로딩 중...</div>
          </div>
        ) : profile ? (
          <div className="p-6 space-y-6">
            {/* 기본 정보 */}
            <div className="bg-[#2a2e45]/60 rounded-lg p-6">
              <div className="mb-4">
                <h3 className="text-2xl font-bold text-white mb-2">{profile.nickname}</h3>
              </div>

              {/* 통계 */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="text-center p-3 bg-purple-600/10 rounded-lg">
                  <div className="text-2xl font-bold text-purple-300">{profile.postCount}</div>
                  <div className="text-sm text-gray-400">작성한 글</div>
                </div>
                <div className="text-center p-3 bg-blue-600/10 rounded-lg">
                  <div className="text-2xl font-bold text-blue-300">{profile.commentCount}</div>
                  <div className="text-sm text-gray-400">작성한 댓글</div>
                </div>
                <div className="text-center p-3 bg-green-600/10 rounded-lg">
                  <div className="text-2xl font-bold text-green-300">{profile.attendanceCount}</div>
                  <div className="text-sm text-gray-400">출석 일수</div>
                </div>
                <div className="text-center p-3 bg-orange-600/10 rounded-lg">
                  <div className="text-2xl font-bold text-orange-300">{profile.iconCount}</div>
                  <div className="text-sm text-gray-400">보유 아이콘</div>
                </div>
              </div>
            </div>

            {/* 최근 획득한 인증서 */}
            <div className="bg-[#2a2e45]/60 rounded-lg p-6">
              <h4 className="text-lg font-bold text-white mb-4">🏆 최근 획득한 인증서</h4>
              {profile.certificates.length === 0 ? (
                <p className="text-gray-400 text-center py-4">아직 획득한 인증서가 없습니다.</p>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {profile.certificates.slice(0, 4).map((cert) => {
                    // 인증서 아이콘 매핑
                    const certIcons = {
                      '별빛 탐험가': '🌠',
                      '우주인 등록증': '🌍',
                      '은하 통신병': '📡',
                      '별 관측 매니아': '🔭',
                      '별빛 채팅사': '🗨️',
                      '별 헤는 밤 시민증': '🏅',
                      '별빛 수호자': '🛡️',
                      '우주 실험자': '⚙️',
                      '건의왕': '💡',
                      '은하 관리자 훈장': '🏆'
                    };
                    const icon = certIcons[cert.title] || cert.iconUrl || '🏆';
                    
                    return (
                      <div key={cert.id} className="relative group">
                        <div className="flex items-center gap-3 p-4 bg-gradient-to-r from-yellow-500/10 to-orange-500/10 rounded-lg border border-yellow-500/30 hover:border-yellow-400/50 transition-all duration-300 hover:shadow-lg hover:shadow-yellow-500/20">
                          <div className="text-3xl animate-pulse">{icon}</div>
                          <div className="flex-1">
                            <h5 className="font-bold text-yellow-300 text-lg">{cert.title}</h5>
                            <p className="text-sm text-gray-300 mt-1">{cert.description}</p>
                            <p className="text-xs text-yellow-400/70 mt-2 font-medium">{new Date(cert.earnedAt).toLocaleDateString('ko-KR')}</p>
                          </div>
                        </div>
                        <div className="absolute inset-0 bg-gradient-to-r from-yellow-500/5 to-orange-500/5 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* 액션 버튼 */}
            {user && user.id !== profile.id && (
              <div className="flex gap-3">
                <button
                  onClick={() => setShowMessageForm(!showMessageForm)}
                  className="flex-1 px-6 py-3 bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white font-medium rounded-lg shadow-lg hover:shadow-xl transition-all duration-200 transform hover:scale-105"
                >
                  📩 쪽지 보내기
                </button>
              </div>
            )}

            {/* 쪽지 작성 폼 */}
            {showMessageForm && (
              <div className="bg-[#2a2e45]/60 rounded-lg p-6">
                <h4 className="text-lg font-bold text-white mb-4">📝 쪽지 작성</h4>
                <form onSubmit={handleSendMessage} className="space-y-4">
                  <div>
                    <label className="block text-white font-medium mb-2">제목</label>
                    <input
                      type="text"
                      value={messageForm.title}
                      onChange={(e) => setMessageForm(prev => ({ ...prev, title: e.target.value }))}
                      placeholder="쪽지 제목을 입력하세요"
                      className="w-full px-4 py-3 bg-[#1f2336]/60 border border-purple-500/30 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-400/20 transition-all"
                      maxLength={100}
                      required
                    />
                  </div>
                  
                  <div>
                    <label className="block text-white font-medium mb-2">내용</label>
                    <textarea
                      value={messageForm.content}
                      onChange={(e) => setMessageForm(prev => ({ ...prev, content: e.target.value }))}
                      placeholder="쪽지 내용을 입력하세요"
                      className="w-full px-4 py-3 bg-[#1f2336]/60 border border-purple-500/30 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-400/20 transition-all resize-none"
                      rows={4}
                      maxLength={1000}
                      required
                    />
                  </div>
                  
                  <div className="flex gap-3">
                    <button
                      type="button"
                      onClick={() => setShowMessageForm(false)}
                      className="flex-1 px-6 py-3 bg-gray-600/30 hover:bg-gray-600/50 text-gray-300 font-medium rounded-lg transition-all duration-200"
                    >
                      취소
                    </button>
                    <button
                      type="submit"
                      disabled={sendingMessage}
                      className="flex-1 px-6 py-3 bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 disabled:from-gray-600 disabled:to-gray-600 text-white font-medium rounded-lg shadow-lg hover:shadow-xl transition-all duration-200 transform hover:scale-105 disabled:scale-100 disabled:cursor-not-allowed"
                    >
                      {sendingMessage ? '전송 중...' : '쪽지 전송'}
                    </button>
                  </div>
                </form>
              </div>
            )}
          </div>
        ) : (
          <div className="p-8 text-center">
            <div className="text-6xl mb-4">😕</div>
            <p className="text-gray-400 text-lg">사용자 정보를 불러올 수 없습니다.</p>
          </div>
        )}
      </div>
    </div>
  );
}