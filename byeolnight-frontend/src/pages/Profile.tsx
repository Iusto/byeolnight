import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import MyActivity from '../components/MyActivity';
import UserIconDisplay from '../components/UserIconDisplay';
import { getReceivedMessages, getSentMessages, markMessageAsRead, type Message, type MessageListResponse } from '../lib/api/message';
import { getNotifications, markAsRead, markAllAsRead } from '../lib/api/notification';
import type { Notification, NotificationListResponse } from '../types/notification';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writerNickname: string;
  likeCount: number;
  commentCount: number;
  viewCount: number;
  isBlinded: boolean;
  createdAt: string;
}

interface Comment {
  id: number;
  postId: number;
  postTitle: string;
  content: string;
  writerNickname: string;
  parentId?: number;
  isBlinded: boolean;
  createdAt: string;
}

interface MyActivityData {
  myPosts: Post[];
  myComments: Comment[];
  receivedMessages: {
    messages: Message[];
    totalCount: number;
  };
  sentMessages: {
    messages: Message[];
    totalCount: number;
  };
  totalPostCount: number;
  totalCommentCount: number;
  totalReceivedMessageCount: number;
  totalSentMessageCount: number;
}

const categoryLabels: Record<string, string> = {
  NEWS: '뉴스',
  DISCUSSION: '토론',
  IMAGE: '사진',
  EVENT: '행사',
  REVIEW: '후기',
  FREE: '자유',
  NOTICE: '공지',
};
import axios from '../lib/axios';

interface UserProfile {
  id: number;
  email: string;
  nickname: string;
  phone: string;
  role: string;
  nicknameChanged: boolean;
  nicknameUpdatedAt?: string;
  points: number;
  attendanceCount?: number;
  ownedIcons?: UserIcon[];
  equippedIcon?: UserIcon;
}

interface UserIcon {
  id: number;
  name: string;
  iconUrl: string;
  price: number;
  purchasedAt: string;
  equipped: boolean;
}

export default function Profile() {
  const { user, refreshUserInfo } = useAuth();
  const [searchParams] = useSearchParams();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'info' | 'posts' | 'comments' | 'icons' | 'messages' | 'notifications'>('info');
  const [icons, setIcons] = useState<UserIcon[]>([]);
  const [activity, setActivity] = useState<MyActivityData | null>(null);
  const [messageTab, setMessageTab] = useState<'received' | 'sent'>('received');
  const [selectedMessage, setSelectedMessage] = useState<Message | null>(null);
  const [receivedMessages, setReceivedMessages] = useState<MessageListResponse>({ messages: [], totalCount: 0, currentPage: 0, totalPages: 0, hasNext: false, hasPrevious: false });
  const [sentMessages, setSentMessages] = useState<MessageListResponse>({ messages: [], totalCount: 0, currentPage: 0, totalPages: 0, hasNext: false, hasPrevious: false });
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [notifications, setNotifications] = useState<NotificationListResponse>({ notifications: [], totalCount: 0, currentPage: 0, totalPages: 0, hasNext: false, hasPrevious: false });
  const [notificationsLoading, setNotificationsLoading] = useState(false);
  const [iconLoading, setIconLoading] = useState<number | null>(null);

  useEffect(() => {
    if (user) {
      fetchProfile();
    }
  }, [user]);

  useEffect(() => {
    // URL 파라미터에서 탭 정보 읽기
    const tab = searchParams.get('tab');
    if (tab && ['info', 'posts', 'comments', 'icons', 'messages', 'notifications'].includes(tab)) {
      setActiveTab(tab as 'info' | 'posts' | 'comments' | 'icons' | 'messages' | 'notifications');
    }
  }, [searchParams]);

  const fetchProfile = async () => {
    try {
      setLoading(true);
      const response = await axios.get('/member/users/me');
      console.log('프로필 응답:', response.data);
      if (response.data?.success && response.data.data) {
        setProfile(response.data.data);
        console.log('프로필 설정 완료:', response.data.data);
      } else {
        console.log('프로필 데이터 없음');
      }
      
      // 보유 아이콘 정보 가져오기
      const iconsResponse = await axios.get('/member/shop/my-icons');
      console.log('아이콘 응답:', iconsResponse.data);
      if (iconsResponse.data?.success) {
        const iconData = iconsResponse.data.data || [];
        console.log('아이콘 데이터:', iconData);
        setIcons(Array.isArray(iconData) ? iconData : []);
      } else {
        setIcons([]);
      }
      
      // 활동 내역 가져오기
      try {
        const activityResponse = await axios.get('/member/users/my-activity?page=0&size=10');
        console.log('활동 내역 응답:', activityResponse.data);
        if (activityResponse.data?.success && activityResponse.data.data) {
          setActivity(activityResponse.data.data);
          console.log('활동 내역 설정 완료:', activityResponse.data.data);
        } else {
          console.log('활동 내역 데이터 없음');
          setActivity({
            myPosts: [],
            myComments: [],
            receivedMessages: { messages: [], totalCount: 0 },
            sentMessages: { messages: [], totalCount: 0 },
            totalPostCount: 0,
            totalCommentCount: 0,
            totalReceivedMessageCount: 0,
            totalSentMessageCount: 0
          });
        }
      } catch (error) {
        console.error('활동 내역 조회 실패:', error);
        setActivity({
          myPosts: [],
          myComments: [],
          receivedMessages: { messages: [], totalCount: 0 },
          sentMessages: { messages: [], totalCount: 0 },
          totalPostCount: 0,
          totalCommentCount: 0,
          totalReceivedMessageCount: 0,
          totalSentMessageCount: 0
        });
      }
      
      // 쪽지 데이터 가져오기
      await fetchMessages();
      
      // 알림 데이터 가져오기
      await fetchNotifications();
    } catch (error) {
      console.error('프로필 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const maskPhone = (phone: string) => {
    if (!phone) return '';
    return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
  };

  const fetchMessages = async () => {
    try {
      setMessagesLoading(true);
      const [receivedData, sentData] = await Promise.all([
        getReceivedMessages({ page: 0, size: 10 }),
        getSentMessages({ page: 0, size: 10 })
      ]);
      setReceivedMessages(receivedData);
      setSentMessages(sentData);
    } catch (error) {
      console.error('쪽지 데이터 조회 실패:', error);
    } finally {
      setMessagesLoading(false);
    }
  };

  const fetchNotifications = async () => {
    try {
      setNotificationsLoading(true);
      const data = await getNotifications({ page: 0, size: 20 });
      setNotifications(data);
    } catch (error) {
      console.error('알림 데이터 조회 실패:', error);
    } finally {
      setNotificationsLoading(false);
    }
  };

  const handleNotificationClick = async (notification: Notification) => {
    if (!notification.isRead) {
      try {
        await markAsRead(notification.id);
        setNotifications(prev => ({
          ...prev,
          notifications: prev.notifications.map(n => 
            n.id === notification.id ? { ...n, isRead: true } : n
          )
        }));
      } catch (error) {
        console.error('알림 읽음 처리 실패:', error);
      }
    }
    
    if (notification.targetUrl) {
      window.location.href = notification.targetUrl;
    }
  };

  const handleMessageClick = async (message: Message) => {
    setSelectedMessage(message);
    
    if (messageTab === 'received' && !message.isRead) {
      try {
        await markMessageAsRead(message.id);
        // 로컬 상태 업데이트
        setReceivedMessages(prev => ({
          ...prev,
          messages: prev.messages.map(m => 
            m.id === message.id ? { ...m, isRead: true, readAt: new Date().toISOString() } : m
          )
        }));
        // 선택된 메시지도 업데이트
        setSelectedMessage(prev => prev ? { ...prev, isRead: true, readAt: new Date().toISOString() } : null);
      } catch (error) {
        console.error('쪽지 읽음 처리 실패:', error);
      }
    }
  };

  const truncateText = (text: string, maxLength: number) => {
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  };

  const handleIconEquip = async (userIconId: number) => {
    try {
      setIconLoading(userIconId);
      const currentIcon = icons.find(icon => icon.id === userIconId);
      
      if (!currentIcon) {
        alert('아이콘을 찾을 수 없습니다.');
        return;
      }
      
      // StellaIcon ID를 사용
      const stellaIconId = currentIcon.iconId;
      
      if (currentIcon.equipped) {
        await axios.post(`/member/shop/icons/${stellaIconId}/unequip`);
      } else {
        await axios.post(`/member/shop/icons/${stellaIconId}/equip`);
      }
      
      // 아이콘 목록 새로고침
      const iconsResponse = await axios.get('/member/shop/my-icons');
      if (iconsResponse.data?.success) {
        setIcons(iconsResponse.data.data || []);
      }
      
      // 사용자 정보 새로고침 (navbar 아이콘 업데이트용)
      await refreshUserInfo();
    } catch (error) {
      console.error('아이콘 장착/해제 실패:', error);
      alert('아이콘 장착/해제에 실패했습니다.');
    } finally {
      setIconLoading(null);
    }
  };

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
        <div className="max-w-4xl mx-auto bg-[#1f2336]/80 backdrop-blur-md p-8 rounded-xl shadow-xl text-center">
          <h1 className="text-2xl font-bold mb-4">로그인이 필요합니다</h1>
          <Link
            to="/login"
            className="inline-block px-6 py-3 bg-purple-600 hover:bg-purple-700 rounded-lg transition-colors"
          >
            로그인하기
          </Link>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
        <div className="max-w-4xl mx-auto bg-[#1f2336]/80 backdrop-blur-md p-8 rounded-xl shadow-xl text-center">
          <p className="text-white text-lg">로딩 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-6xl mx-auto">
        {/* 헤더 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md p-8 rounded-xl shadow-xl mb-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-3xl font-bold mb-2 drop-shadow-glow flex items-center gap-3">
                <span className="bg-[#0f1419] px-2 py-1 rounded-lg">
                  <UserIconDisplay iconName={user?.equippedIconName} size="medium" className="text-2xl" />
                </span> 
                내 정보
              </h1>
              <p className="text-gray-400">프로필 정보와 활동 내역을 확인하세요</p>
            </div>
            <Link
              to="/me"
              className="px-6 py-3 bg-purple-600 hover:bg-purple-700 rounded-lg transition-colors font-medium text-white"
            >
              ✏️ 내 정보 수정
            </Link>
          </div>

          {/* 탭 메뉴 */}
          <div className="flex space-x-1 bg-[#2a2e45]/60 rounded-lg p-1 overflow-x-auto">
            <button
              onClick={() => setActiveTab('info')}
              className={`py-3 px-4 rounded-md transition-all font-medium whitespace-nowrap ${
                activeTab === 'info'
                  ? 'bg-purple-600 text-white'
                  : 'text-gray-400 hover:text-white hover:bg-purple-600/20'
              }`}
            >
              📋 기본정보
            </button>
            <button
              onClick={() => setActiveTab('posts')}
              className={`py-3 px-4 rounded-md transition-all font-medium whitespace-nowrap ${
                activeTab === 'posts'
                  ? 'bg-purple-600 text-white'
                  : 'text-gray-400 hover:text-white hover:bg-purple-600/20'
              }`}
            >
              📝 내게시글
            </button>
            <button
              onClick={() => setActiveTab('comments')}
              className={`py-3 px-4 rounded-md transition-all font-medium whitespace-nowrap ${
                activeTab === 'comments'
                  ? 'bg-purple-600 text-white'
                  : 'text-gray-400 hover:text-white hover:bg-purple-600/20'
              }`}
            >
              💬 내댓글
            </button>
            <button
              onClick={() => setActiveTab('icons')}
              className={`py-3 px-4 rounded-md transition-all font-medium whitespace-nowrap ${
                activeTab === 'icons'
                  ? 'bg-purple-600 text-white'
                  : 'text-gray-400 hover:text-white hover:bg-purple-600/20'
              }`}
            >
              🎨 내아이콘
            </button>
            <button
              onClick={() => setActiveTab('messages')}
              className={`py-3 px-4 rounded-md transition-all font-medium whitespace-nowrap ${
                activeTab === 'messages'
                  ? 'bg-purple-600 text-white'
                  : 'text-gray-400 hover:text-white hover:bg-purple-600/20'
              }`}
            >
              📩 쪽지함
            </button>
            <button
              onClick={() => setActiveTab('notifications')}
              className={`py-3 px-4 rounded-md transition-all font-medium whitespace-nowrap ${
                activeTab === 'notifications'
                  ? 'bg-purple-600 text-white'
                  : 'text-gray-400 hover:text-white hover:bg-purple-600/20'
              }`}
            >
              🔔 알림
            </button>
          </div>
        </div>

        {/* 내용 영역 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl shadow-xl p-8">
          {activeTab === 'info' && (
            <div className="space-y-6">
              <h2 className="text-2xl font-bold text-white mb-6">📋 기본 정보</h2>
              {console.log('Info tab - profile:', profile)}
              {console.log('Info tab - activity:', activity)}
              {!profile ? (
                <div className="text-center py-8">
                  <div className="text-gray-400">로딩 중...</div>
                </div>
              ) : (
                <>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {/* 계정 정보 */}
                    <div className="bg-[#2a2e45]/60 rounded-lg p-6">
                      <h3 className="text-lg font-bold text-white mb-4">🔐 계정 정보</h3>
                      <div className="space-y-4">
                        <div>
                          <label className="block text-sm font-medium text-gray-400 mb-1">이메일</label>
                          <div className="text-white bg-[#1f2336]/60 px-4 py-3 rounded-lg">
                            {profile.email}
                          </div>
                        </div>
                        <div>
                          <label className="block text-sm font-medium text-gray-400 mb-1">닉네임</label>
                          <div className="text-white bg-[#1f2336]/60 px-4 py-3 rounded-lg">
                            {profile.nickname}
                          </div>
                        </div>
                        <div>
                          <label className="block text-sm font-medium text-gray-400 mb-1">전화번호</label>
                          <div className="text-white bg-[#1f2336]/60 px-4 py-3 rounded-lg">
                            {maskPhone(profile.phone)}
                          </div>
                        </div>
                        <div>
                          <label className="block text-sm font-medium text-gray-400 mb-1">권한</label>
                          <div className="text-white bg-[#1f2336]/60 px-4 py-3 rounded-lg">
                            {profile.role === 'ADMIN' ? '관리자' : '일반 사용자'}
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* 활동 정보 */}
                    <div className="bg-[#2a2e45]/60 rounded-lg p-6">
                      <h3 className="text-lg font-bold text-white mb-4">✨ 활동 정보</h3>
                      <div className="grid grid-cols-2 gap-4 mb-4">
                        <div className="flex items-center justify-between p-4 bg-[#1f2336]/60 rounded-lg">
                          <div className="flex items-center gap-3 min-w-0 flex-1">
                            <span className="text-2xl flex-shrink-0">✨</span>
                            <div className="min-w-0">
                              <div className="text-white font-medium whitespace-nowrap">보유 포인트</div>
                              <div className="text-sm text-gray-400 whitespace-nowrap">활동으로 획득</div>
                            </div>
                          </div>
                          <div className="text-xl font-bold text-yellow-400 flex-shrink-0">
                            {(profile.points || 0).toLocaleString()}
                          </div>
                        </div>
                        
                        <div className="flex items-center justify-between p-4 bg-[#1f2336]/60 rounded-lg">
                          <div className="flex items-center gap-3 min-w-0 flex-1">
                            <span className="text-2xl flex-shrink-0">🎨</span>
                            <div className="min-w-0">
                              <div className="text-white font-medium whitespace-nowrap">보유 아이콘</div>
                              <div className="text-sm text-gray-400 whitespace-nowrap">스텔라 아이콘</div>
                            </div>
                          </div>
                          <div className="text-xl font-bold text-blue-400 flex-shrink-0">
                            {icons.length}
                          </div>
                        </div>
                        
                        <div className="flex items-center justify-between p-4 bg-[#1f2336]/60 rounded-lg">
                          <div className="flex items-center gap-3 min-w-0 flex-1">
                            <span className="text-2xl flex-shrink-0">📝</span>
                            <div className="min-w-0">
                              <div className="text-white font-medium whitespace-nowrap">작성 게시글</div>
                              <div className="text-sm text-gray-400 whitespace-nowrap">내가 작성한 글</div>
                            </div>
                          </div>
                          <div className="text-xl font-bold text-green-400 flex-shrink-0">
                            {activity?.totalPostCount || 0}
                          </div>
                        </div>
                        
                        <div className="flex items-center justify-between p-4 bg-[#1f2336]/60 rounded-lg">
                          <div className="flex items-center gap-3 min-w-0 flex-1">
                            <span className="text-2xl flex-shrink-0">💬</span>
                            <div className="min-w-0">
                              <div className="text-white font-medium whitespace-nowrap">작성 댓글</div>
                              <div className="text-sm text-gray-400 whitespace-nowrap">내가 작성한 댓글</div>
                            </div>
                          </div>
                          <div className="text-xl font-bold text-purple-400 flex-shrink-0">
                            {activity?.totalCommentCount || 0}
                          </div>
                        </div>
                        
                        <div className="flex items-center justify-between p-4 bg-[#1f2336]/60 rounded-lg">
                          <div className="flex items-center gap-3 min-w-0 flex-1">
                            <span className="text-2xl flex-shrink-0">📅</span>
                            <div className="min-w-0">
                              <div className="text-white font-medium whitespace-nowrap">출석일수</div>
                              <div className="text-sm text-gray-400 whitespace-nowrap">누적 출석체크</div>
                            </div>
                          </div>
                          <div className="text-xl font-bold text-orange-400 flex-shrink-0">
                            {profile?.attendanceCount || 0}일
                          </div>
                        </div>
                      </div>
                      
                      <div className="bg-[#1f2336]/60 rounded-lg p-4">
                        <h4 className="text-white font-medium mb-3">닉네임 변경 정보</h4>
                        <div className="space-y-2 text-sm">
                          <div className="flex justify-between">
                            <span className="text-gray-400">변경 여부:</span>
                            <span className={profile.nicknameChanged ? 'text-green-400' : 'text-gray-400'}>
                              {profile.nicknameChanged ? '변경함' : '변경 안함'}
                            </span>
                          </div>
                          {profile.nicknameChanged && profile.nicknameUpdatedAt && (
                            <div className="flex justify-between">
                              <span className="text-gray-400">마지막 변경:</span>
                              <span className="text-white">{formatDate(profile.nicknameUpdatedAt)}</span>
                            </div>
                          )}
                          <div className="text-xs text-gray-500 mt-2">
                            * 닉네임은 6개월마다 변경 가능합니다
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* 보유 아이콘 */}
                  <div className="bg-[#2a2e45]/60 rounded-lg p-6">
                    <h3 className="text-lg font-bold text-white mb-4">🎨 보유 아이콘</h3>
                    {icons.length === 0 ? (
                      <div className="text-center text-gray-400 py-8">
                        <div className="text-4xl mb-2">🛍️</div>
                        <p>아직 구매한 아이콘이 없습니다.</p>
                        <Link
                          to="/shop"
                          className="inline-block mt-3 px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg text-sm transition-colors text-white"
                        >
                          상점에서 구매하기
                        </Link>
                      </div>
                    ) : (
                      <div className="grid grid-cols-4 md:grid-cols-6 gap-4">
                        {icons.map((icon) => (
                          <div
                            key={icon.id}
                            className={`relative p-3 rounded-lg text-center transition-all ${
                              icon.equipped
                                ? 'bg-purple-600/30 border-2 border-purple-400'
                                : 'bg-[#1f2336]/60 hover:bg-[#1f2336]/80'
                            }`}
                          >
                            <div className="text-2xl mb-1">{icon.iconUrl}</div>
                            <div className="text-xs text-gray-400">{icon.name}</div>
                            {icon.equipped && (
                              <div className="absolute -top-1 -right-1 w-4 h-4 bg-green-500 rounded-full flex items-center justify-center">
                                <span className="text-xs text-white">✓</span>
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* 빠른 액션 */}
                  <div className="bg-[#2a2e45]/60 rounded-lg p-6">
                    <h3 className="text-lg font-bold text-white mb-4">🚀 빠른 액션</h3>
                    <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
                      <Link
                        to="/me"
                        className="flex flex-col items-center gap-2 p-4 bg-[#1f2336]/60 hover:bg-[#1f2336]/80 rounded-lg transition-colors group"
                      >
                        <span className="text-2xl group-hover:scale-110 transition-transform">✏️</span>
                        <span className="text-sm text-gray-400 group-hover:text-white">내 정보 수정</span>
                      </Link>
                      <Link
                        to="/posts/new"
                        className="flex flex-col items-center gap-2 p-4 bg-[#1f2336]/60 hover:bg-[#1f2336]/80 rounded-lg transition-colors group"
                      >
                        <span className="text-2xl group-hover:scale-110 transition-transform">📝</span>
                        <span className="text-sm text-gray-400 group-hover:text-white">글 작성</span>
                      </Link>
                      <button
                        onClick={() => setActiveTab('messages')}
                        className="flex flex-col items-center gap-2 p-4 bg-[#1f2336]/60 hover:bg-[#1f2336]/80 rounded-lg transition-colors group"
                      >
                        <span className="text-2xl group-hover:scale-110 transition-transform">📩</span>
                        <span className="text-sm text-gray-400 group-hover:text-white">쪽지함</span>
                      </button>
                      <Link
                        to="/shop"
                        className="flex flex-col items-center gap-2 p-4 bg-[#1f2336]/60 hover:bg-[#1f2336]/80 rounded-lg transition-colors group"
                      >
                        <span className="text-2xl group-hover:scale-110 transition-transform">🛍️</span>
                        <span className="text-sm text-gray-400 group-hover:text-white">상점</span>
                      </Link>
                      <Link
                        to="/certificates"
                        className="flex flex-col items-center gap-2 p-4 bg-[#1f2336]/60 hover:bg-[#1f2336]/80 rounded-lg transition-colors group"
                      >
                        <span className="text-2xl group-hover:scale-110 transition-transform">🏆</span>
                        <span className="text-sm text-gray-400 group-hover:text-white">인증서</span>
                      </Link>
                    </div>
                  </div>
                </>
              )}
            </div>
          )}

          {activeTab === 'posts' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white">📝 내가 작성한 게시글</h2>
                <div className="text-sm text-gray-400">
                  총 {activity?.totalPostCount || 0}개
                </div>
              </div>
              {console.log('Posts tab - activity:', activity)}
              {console.log('Posts tab - myPosts:', activity?.myPosts)}
              {!activity ? (
                <div className="text-center py-8">
                  <div className="text-gray-400">로딩 중...</div>
                </div>
              ) : !activity.myPosts || activity.myPosts.length === 0 ? (
                <div className="text-center text-gray-400 py-8">
                  <div className="text-4xl mb-2">📝</div>
                  <p>아직 작성한 게시글이 없습니다.</p>
                  <Link
                    to="/posts/new"
                    className="inline-block mt-3 px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg text-sm transition-colors text-white"
                  >
                    첫 게시글 작성하기
                  </Link>
                </div>
              ) : (
                <div className="space-y-3">
                  {activity.myPosts.map((post) => (
                    <div key={post.id} className="bg-[#2a2e45]/60 rounded-lg p-4 hover:bg-[#2a2e45]/80 transition-colors">
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <Link
                            to={`/posts/${post.id}`}
                            className="text-white hover:text-purple-300 font-medium block mb-2"
                          >
                            {post.title}
                            {post.isBlinded && <span className="text-red-400 ml-2">(블라인드)</span>}
                          </Link>
                          <p className="text-gray-400 text-sm mb-2">
                            {truncateText(post.content.replace(/<[^>]*>/g, ''), 100)}
                          </p>
                          <div className="flex items-center gap-4 text-xs text-gray-500">
                            <span>🗂 {categoryLabels[post.category] || post.category}</span>
                            <span>❤️ {post.likeCount}</span>
                            <span>💬 {post.commentCount}</span>
                            <span>👁 {post.viewCount}</span>
                            <span>📅 {formatDate(post.createdAt)}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                  
                  {activity.totalPostCount > activity.myPosts.length && (
                    <div className="text-center py-4">
                      <button className="px-4 py-2 bg-purple-600/20 hover:bg-purple-600/30 rounded-lg text-sm text-purple-300 transition-colors">
                        더 보기 ({activity.totalPostCount - activity.myPosts.length}개 더)
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {activeTab === 'comments' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white">💬 내가 작성한 댓글</h2>
                <div className="text-sm text-gray-400">
                  총 {activity?.totalCommentCount || 0}개
                </div>
              </div>
              {console.log('Comments tab - activity:', activity)}
              {console.log('Comments tab - myComments:', activity?.myComments)}
              {!activity ? (
                <div className="text-center py-8">
                  <div className="text-gray-400">로딩 중...</div>
                </div>
              ) : !activity.myComments || activity.myComments.length === 0 ? (
                <div className="text-center text-gray-400 py-8">
                  <div className="text-4xl mb-2">💬</div>
                  <p>아직 작성한 댓글이 없습니다.</p>
                  <Link
                    to="/posts"
                    className="inline-block mt-3 px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg text-sm transition-colors text-white"
                  >
                    게시글 보러가기
                  </Link>
                </div>
              ) : (
                <div className="space-y-3">
                  {activity.myComments.map((comment) => (
                    <div key={comment.id} className="bg-[#2a2e45]/60 rounded-lg p-4 hover:bg-[#2a2e45]/80 transition-colors">
                      <div className="mb-2">
                        <Link
                          to={`/posts/${comment.postId}`}
                          className="text-purple-300 hover:text-purple-200 text-sm font-medium"
                        >
                          📄 {comment.postTitle}
                        </Link>
                        {comment.parentId && (
                          <span className="text-xs text-gray-500 ml-2">(답글)</span>
                        )}
                      </div>
                      <p className="text-white mb-2">
                        {comment.content}
                        {comment.isBlinded && <span className="text-red-400 ml-2">(블라인드)</span>}
                      </p>
                      <div className="text-xs text-gray-500">
                        📅 {formatDate(comment.createdAt)}
                      </div>
                    </div>
                  ))}
                  
                  {activity.totalCommentCount > activity.myComments.length && (
                    <div className="text-center py-4">
                      <button className="px-4 py-2 bg-purple-600/20 hover:bg-purple-600/30 rounded-lg text-sm text-purple-300 transition-colors">
                        더 보기 ({activity.totalCommentCount - activity.myComments.length}개 더)
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {activeTab === 'icons' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white">🎨 보유 스텔라 아이콘</h2>
                <div className="text-sm text-gray-400">
                  총 {icons.length}개
                </div>
              </div>
              {icons.length === 0 ? (
                <div className="text-center text-gray-400 py-8">
                  <div className="text-4xl mb-2">🛒️</div>
                  <p>아직 구매한 아이콘이 없습니다.</p>
                  <Link
                    to="/shop"
                    className="inline-block mt-3 px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg text-sm transition-colors text-white"
                  >
                    상점에서 구매하기
                  </Link>
                </div>
              ) : (
                <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4">
                  {icons.map((icon) => (
                    <div
                      key={icon.id}
                      className={`relative p-4 rounded-lg text-center transition-all min-h-[180px] flex flex-col justify-between ${
                        icon.equipped
                          ? 'bg-purple-600/30 border-2 border-purple-400'
                          : 'bg-[#2a2e45]/60 hover:bg-[#2a2e45]/80'
                      }`}
                    >
                      <div className="flex-1">
                        <div className="text-3xl mb-3">{icon.iconUrl}</div>
                        <div className="text-sm text-gray-300 mb-2 break-words leading-tight">{icon.name}</div>
                        <div className="text-xs text-gray-500 mb-1">{(icon.price || 0).toLocaleString()} 스텔라</div>
                        <div className="text-xs text-gray-500 mb-3">{formatDate(icon.purchasedAt)}</div>
                      </div>
                      
                      <button
                        onClick={() => handleIconEquip(icon.id)}
                        disabled={iconLoading === icon.id}
                        className={`w-full py-2 px-2 text-xs rounded transition-colors font-medium ${
                          icon.equipped
                            ? 'bg-red-600/20 text-red-400 hover:bg-red-600/30'
                            : 'bg-purple-600/20 text-purple-400 hover:bg-purple-600/30'
                        } ${iconLoading === icon.id ? 'opacity-50 cursor-not-allowed' : ''}`}
                      >
                        {iconLoading === icon.id ? '...' : (icon.equipped ? '해제' : '장착')}
                      </button>
                      
                      {icon.equipped && (
                        <div className="absolute -top-1 -right-1 w-5 h-5 bg-green-500 rounded-full flex items-center justify-center">
                          <span className="text-xs text-white">✓</span>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'messages' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white">📩 쪽지함</h2>
                <button
                  onClick={fetchMessages}
                  className="px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg text-sm transition-colors"
                >
                  🔄 새로고침
                </button>
              </div>
              
              {/* 쪽지함 탭 */}
              <div className="flex space-x-1 bg-[#2a2e45]/60 rounded-lg p-1">
                <button
                  onClick={() => {
                    setMessageTab('received');
                    if (receivedMessages.messages.length === 0) {
                      fetchMessages();
                    }
                  }}
                  className={`flex-1 py-2 px-4 rounded-md transition-all text-sm ${
                    messageTab === 'received'
                      ? 'bg-blue-600 text-white'
                      : 'text-gray-400 hover:text-white hover:bg-blue-600/20'
                  }`}
                >
                  📥 받은 쪽지 ({receivedMessages.totalCount})
                </button>
                <button
                  onClick={() => {
                    setMessageTab('sent');
                    if (sentMessages.messages.length === 0) {
                      fetchMessages();
                    }
                  }}
                  className={`flex-1 py-2 px-4 rounded-md transition-all text-sm ${
                    messageTab === 'sent'
                      ? 'bg-blue-600 text-white'
                      : 'text-gray-400 hover:text-white hover:bg-blue-600/20'
                  }`}
                >
                  📤 보낸 쪽지 ({sentMessages.totalCount})
                </button>
              </div>

              {/* 쪽지 목록 */}
              <div className="space-y-3">
                {messagesLoading ? (
                  <div className="text-center py-8">
                    <div className="text-gray-400">로딩 중...</div>
                  </div>
                ) : messageTab === 'received' ? (
                  receivedMessages.messages.length === 0 ? (
                    <div className="text-center text-gray-400 py-8">
                      <div className="text-4xl mb-2">📭</div>
                      <p>받은 쪽지가 없습니다.</p>
                    </div>
                  ) : (
                    receivedMessages.messages.map((message) => (
                      <div
                        key={message.id}
                        onClick={() => handleMessageClick(message)}
                        className={`bg-[#2a2e45]/60 rounded-lg p-4 hover:bg-[#2a2e45]/80 transition-colors cursor-pointer ${
                          !message.isRead ? 'border-l-4 border-blue-500' : ''
                        }`}
                      >
                        <div className="flex items-start justify-between mb-2">
                          <h4 className={`font-medium ${
                            !message.isRead ? 'text-white font-bold' : 'text-gray-300'
                          }`}>
                            {message.title}
                            {!message.isRead && <span className="text-blue-400 ml-2">●</span>}
                          </h4>
                          <span className="text-xs text-gray-500">
                            {formatDate(message.createdAt)}
                          </span>
                        </div>
                        <p className="text-sm text-gray-400 mb-2">
                          보낸이: {message.senderNickname}
                        </p>
                        <p className="text-sm text-gray-300">
                          {truncateText(message.content, 100)}
                        </p>
                      </div>
                    ))
                  )
                ) : (
                  sentMessages.messages.length === 0 ? (
                    <div className="text-center text-gray-400 py-8">
                      <div className="text-4xl mb-2">📤</div>
                      <p>보낸 쪽지가 없습니다.</p>
                    </div>
                  ) : (
                    sentMessages.messages.map((message) => (
                      <div
                        key={message.id}
                        onClick={() => handleMessageClick(message)}
                        className="bg-[#2a2e45]/60 rounded-lg p-4 hover:bg-[#2a2e45]/80 transition-colors cursor-pointer"
                      >
                        <div className="flex items-start justify-between mb-2">
                          <h4 className="text-gray-300 font-medium">
                            {message.title}
                          </h4>
                          <span className="text-xs text-gray-500">
                            {formatDate(message.createdAt)}
                          </span>
                        </div>
                        <p className="text-sm text-gray-400 mb-2">
                          받는이: {message.receiverNickname}
                        </p>
                        <p className="text-sm text-gray-300">
                          {truncateText(message.content, 100)}
                        </p>
                        <div className="text-xs text-gray-500 mt-2">
                          {message.isRead ? (
                            <span className="text-green-400">✓ 읽음 ({formatDate(message.readAt!)})</span>
                          ) : (
                            <span className="text-gray-400">○ 읽지 않음</span>
                          )}
                        </div>
                      </div>
                    ))
                  )
                )}
              </div>
            </div>
          )}

          {activeTab === 'notifications' && (
            <div>
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white">🔔 알림</h2>
                <button
                  onClick={async () => {
                    try {
                      await markAllAsRead();
                      await fetchNotifications();
                    } catch (error) {
                      console.error('모든 알림 읽음 처리 실패:', error);
                    }
                  }}
                  className="px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg text-sm transition-colors"
                >
                  모두 읽음
                </button>
              </div>
              
              {notificationsLoading ? (
                <div className="text-center py-8">
                  <p className="text-gray-400">로딩 중...</p>
                </div>
              ) : !notifications || notifications.notifications.length === 0 ? (
                <div className="text-center py-8">
                  <p className="text-gray-400">알림이 없습니다.</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {notifications?.notifications?.map((notification) => (
                    <div
                      key={notification.id}
                      onClick={() => handleNotificationClick(notification)}
                      className={`p-4 rounded-lg border cursor-pointer transition-all hover:bg-[#2a2e45]/60 ${
                        notification.isRead
                          ? 'bg-[#1f2336]/60 border-gray-600 opacity-70'
                          : 'bg-[#2a2e45]/80 border-purple-500/50'
                      }`}
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-1">
                            <span className="text-sm font-medium text-purple-400">
                              {notification.type === 'COMMENT_ON_POST' && '게시글 댓글'}
                              {notification.type === 'REPLY_ON_COMMENT' && '댓글 답글'}
                              {notification.type === 'NEW_MESSAGE' && '새 쪽지'}
                              {notification.type === 'NEW_NOTICE' && '공지사항'}
                            </span>
                            {!notification.isRead && (
                              <span className="w-2 h-2 bg-red-500 rounded-full"></span>
                            )}
                          </div>
                          <h3 className="font-medium text-white mb-1">{notification.title}</h3>
                          <p className="text-sm text-gray-300 mb-2">{notification.message}</p>
                          <p className="text-xs text-gray-500">{formatDate(notification.createdAt)}</p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
        
        {/* 쪽지 상세 모달 */}
        {selectedMessage && (
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-[#1f2336]/95 backdrop-blur-md rounded-xl border border-purple-500/20 w-full max-w-2xl max-h-[80vh] overflow-y-auto">
              <div className="flex items-center justify-between p-6 border-b border-purple-500/20">
                <h2 className="text-xl font-bold text-white">📩 쪽지 상세</h2>
                <button
                  onClick={() => setSelectedMessage(null)}
                  className="p-2 hover:bg-purple-600/20 rounded-lg transition-colors text-gray-400 hover:text-white"
                >
                  ✕
                </button>
              </div>
              <div className="p-6 space-y-4">
                <div>
                  <h3 className="text-lg font-bold text-white mb-2">{selectedMessage.title}</h3>
                  <div className="text-sm text-gray-400 space-y-1">
                    <p>보낸이: {selectedMessage.senderNickname}</p>
                    <p>받는이: {selectedMessage.receiverNickname}</p>
                    <p>보낸 시간: {formatDate(selectedMessage.createdAt)}</p>
                    {selectedMessage.isRead && selectedMessage.readAt && (
                      <p>읽은 시간: {formatDate(selectedMessage.readAt)}</p>
                    )}
                  </div>
                </div>
                <hr className="border-gray-600" />
                <div className="text-white whitespace-pre-wrap">
                  {selectedMessage.content}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}