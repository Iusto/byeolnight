import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { MessageDetailModal, ProfileMessagesTab, ProfileNotificationsTab, UserIconDisplay } from '../components/user';
import ProfileActivityTabs from '../components/user/ProfileActivityTabs';
import { getReceivedMessages, getSentMessages, markMessageAsRead, type Message, type MessageListResponse } from '../lib/api/message';
import { getNotifications, markAsRead, markAllAsRead, deleteNotification } from '../lib/api/notification';
import type { Notification, NotificationListResponse } from '../types/notification';
import { StarfieldBackground } from '../components/common';

import axios from '../lib/axios';
import {
  type MyActivityData,
  type ProfileTab,
  type UserIcon,
  type UserProfile,
} from '../types/profile';

export default function Profile() {
  const { user, refreshUserInfo } = useAuth();
  const [searchParams] = useSearchParams();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<ProfileTab>('info');
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
  const [postsPage, setPostsPage] = useState(0);
  const [commentsPage, setCommentsPage] = useState(0);
  const [postsLoading, setPostsLoading] = useState(false);
  const [commentsLoading, setCommentsLoading] = useState(false);

  useEffect(() => {
    const tab = searchParams.get('tab');
    if (tab && ['info', 'posts', 'comments', 'icons', 'messages', 'notifications'].includes(tab)) {
      setActiveTab(tab as ProfileTab);
    }
  }, [searchParams]);

  const fetchProfile = useCallback(async () => {
    try {
      setLoading(true);
      const response = await axios.get('/member/users/me');
      if (response.data?.success && response.data.data) {
        setProfile(response.data.data);
      }
      
      const iconsResponse = await axios.get('/member/shop/my-icons');
      if (iconsResponse.data?.success) {
        const iconData = iconsResponse.data.data || [];
        setIcons(Array.isArray(iconData) ? iconData : []);
      } else {
        setIcons([]);
      }
      
      try {
        const activityResponse = await axios.get('/member/users/my-activity?page=0&size=10');
        if (activityResponse.data?.success && activityResponse.data.data) {
          setActivity(activityResponse.data.data);
        } else {
          setActivity({
            myPosts: [],
            myComments: [],
            receivedMessages: { messages: [], totalCount: 0 },
            sentMessages: { messages: [], totalCount: 0 },
            totalPostCount: 0,
            totalCommentCount: 0,
            totalReceivedMessageCount: 0,
            totalSentMessageCount: 0,
            postsCurrentPage: 0,
            postsTotalPages: 0,
            postsHasNext: false,
            postsHasPrevious: false,
            commentsCurrentPage: 0,
            commentsTotalPages: 0,
            commentsHasNext: false,
            commentsHasPrevious: false
          });
        }
      } catch {
        setActivity({
          myPosts: [],
          myComments: [],
          receivedMessages: { messages: [], totalCount: 0 },
          sentMessages: { messages: [], totalCount: 0 },
          totalPostCount: 0,
          totalCommentCount: 0,
          totalReceivedMessageCount: 0,
          totalSentMessageCount: 0,
          postsCurrentPage: 0,
          postsTotalPages: 0,
          postsHasNext: false,
          postsHasPrevious: false,
          commentsCurrentPage: 0,
          commentsTotalPages: 0,
          commentsHasNext: false,
          commentsHasPrevious: false
        });
      }
      
    } catch (error) {
      console.error('프로필 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchPostsPage = async (page: number) => {
    try {
      setPostsLoading(true);
      const response = await axios.get(`/member/users/my-activity?page=${page}&size=10`);
      if (response.data?.success && response.data.data) {
        setActivity(prev => prev ? {
          ...prev,
          myPosts: response.data.data.myPosts,
          postsCurrentPage: response.data.data.postsCurrentPage,
          postsTotalPages: response.data.data.postsTotalPages,
          postsHasNext: response.data.data.postsHasNext,
          postsHasPrevious: response.data.data.postsHasPrevious,
        } : response.data.data);
        setPostsPage(page);
      }
    } catch (error) {
      console.error('게시글 페이지 조회 실패:', error);
    } finally {
      setPostsLoading(false);
    }
  };

  const fetchCommentsPage = async (page: number) => {
    try {
      setCommentsLoading(true);
      const response = await axios.get(`/member/users/my-activity?page=${page}&size=10`);
      if (response.data?.success && response.data.data) {
        setActivity(prev => prev ? {
          ...prev,
          myComments: response.data.data.myComments,
          commentsCurrentPage: response.data.data.commentsCurrentPage,
          commentsTotalPages: response.data.data.commentsTotalPages,
          commentsHasNext: response.data.data.commentsHasNext,
          commentsHasPrevious: response.data.data.commentsHasPrevious,
        } : response.data.data);
        setCommentsPage(page);
      }
    } catch (error) {
      console.error('댓글 페이지 조회 실패:', error);
    } finally {
      setCommentsLoading(false);
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



  const fetchMessages = useCallback(async () => {
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
  }, []);

  const fetchNotifications = useCallback(async () => {
    try {
      setNotificationsLoading(true);
      const data = await getNotifications({ page: 0, size: 20 });
      setNotifications(data);
    } catch (error) {
      console.error('알림 데이터 조회 실패:', error);
    } finally {
      setNotificationsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (user) {
      void Promise.all([fetchProfile(), fetchMessages(), fetchNotifications()]);
    }
  }, [fetchMessages, fetchNotifications, fetchProfile, user]);

  const handleMarkAllNotificationsRead = async () => {
    try {
      await markAllAsRead();
      await fetchNotifications();
    } catch (error) {
      console.error('모든 알림 읽음 처리 실패:', error);
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

  const handleDeleteNotification = async (notificationId: number, event: React.MouseEvent) => {
    event.stopPropagation();
    
    try {
      await deleteNotification(notificationId);
      setNotifications(prev => ({
        ...prev,
        notifications: prev.notifications.filter(n => n.id !== notificationId),
        totalCount: prev.totalCount - 1
      }));
    } catch (error) {
      console.error('알림 삭제 실패:', error);
      alert('알림 삭제에 실패했습니다.');
    }
  };

  const handleMessageClick = async (message: Message) => {
    setSelectedMessage(message);
    
    if (messageTab === 'received' && !message.isRead) {
      try {
        await markMessageAsRead(message.id);
        setReceivedMessages(prev => ({
          ...prev,
          messages: prev.messages.map(m => 
            m.id === message.id ? { ...m, isRead: true, readAt: new Date().toISOString() } : m
          )
        }));
        setSelectedMessage(prev => prev ? { ...prev, isRead: true, readAt: new Date().toISOString() } : null);
      } catch (error) {
        console.error('쪽지 읽음 처리 실패:', error);
      }
    }
  };

  const handleDeleteMessage = async (messageId: number, event: React.MouseEvent) => {
    event.stopPropagation();
    
    if (!confirm('이 쪽지를 삭제하시겠습니까?')) {
      return;
    }
    
    try {
      await axios.delete(`/member/messages/${messageId}`);
      
      if (messageTab === 'received') {
        setReceivedMessages(prev => ({
          ...prev,
          messages: prev.messages.filter(m => m.id !== messageId),
          totalCount: prev.totalCount - 1
        }));
      } else {
        setSentMessages(prev => ({
          ...prev,
          messages: prev.messages.filter(m => m.id !== messageId),
          totalCount: prev.totalCount - 1
        }));
      }
      
      if (selectedMessage?.id === messageId) {
        setSelectedMessage(null);
      }
    } catch (error) {
      console.error('쪽지 삭제 실패:', error);
      alert('쪽지 삭제에 실패했습니다.');
    }
  };

  const handleIconEquip = async (userIconId: number) => {
    try {
      setIconLoading(userIconId);
      const currentIcon = icons.find(icon => icon.id === userIconId);
      
      if (!currentIcon) {
        alert('아이콘을 찾을 수 없습니다.');
        return;
      }
      
      const stellaIconId = currentIcon.iconId;
      
      if (currentIcon.equipped) {
        await axios.post(`/member/shop/icons/${stellaIconId}/unequip`);
      } else {
        await axios.post(`/member/shop/icons/${stellaIconId}/equip`);
      }
      
      const iconsResponse = await axios.get('/member/shop/my-icons');
      if (iconsResponse.data?.success) {
        setIcons(iconsResponse.data.data || []);
      }
      
      await refreshUserInfo();
    } catch (error) {
      console.error('아이콘 장착/해제 실패:', error);
      alert('아이콘 장착/해제에 실패했습니다.');
    } finally {
      setIconLoading(null);
    }
  };

  const getTabClassName = (tabName: string, isActive: boolean) => {
    const baseClasses = 'py-2 px-2 sm:px-3 rounded-lg transition-all font-medium whitespace-nowrap text-xs sm:text-sm min-w-[40px] sm:min-w-[44px] flex items-center justify-center flex-shrink-0';
    if (isActive) {
      return `${baseClasses} bg-purple-600 text-white`;
    }
    return `${baseClasses} text-gray-400 hover:text-white hover:bg-purple-600/20`;
  };

  if (!user) {
    return (
      <div className="min-h-screen bg-space-gradient text-white py-12 px-6">
        <div className="max-w-4xl mx-auto bg-[#1f2336] bg-opacity-80 backdrop-blur-md p-8 rounded-xl shadow-xl text-center">
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
      <div className="min-h-screen bg-space-gradient text-white py-12 px-6">
        <div className="max-w-4xl mx-auto bg-[#1f2336] bg-opacity-80 backdrop-blur-md p-8 rounded-xl shadow-xl text-center">
          <p className="text-white text-lg">로딩 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-space-gradient text-white py-3 sm:py-8 px-3 sm:px-6 relative">
      <div className="fixed inset-0 -z-10 pointer-events-none">
        <StarfieldBackground density={50} />
      </div>
      <div className="max-w-6xl mx-auto">
        {/* 헤더 섹션 - 모바일 최적화 */}
        <div className="bg-slate-800/50 rounded-xl p-3 sm:p-6 mb-4 sm:mb-6 border border-slate-600/30">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 sm:gap-6 mb-4 sm:mb-6">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 sm:w-12 sm:h-12 bg-slate-700/50 rounded-lg flex items-center justify-center flex-shrink-0 overflow-hidden">
                <UserIconDisplay iconName={user?.equippedIconName} size="small" className="text-lg sm:text-2xl" />
              </div>
              <div>
                <h1 className="text-lg sm:text-2xl font-bold text-white">내 정보</h1>
                <p className="text-xs sm:text-sm text-gray-400">프로필 정보와 활동 내역</p>
              </div>
            </div>
            <Link
              to="/me"
              className="w-full sm:w-auto px-3 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg transition-colors font-medium text-white text-center text-sm"
            >
              ✏️ 계정 관리
            </Link>
          </div>

          {/* 탭 네비게이션 - 모바일 최적화 */}
          <div className="bg-slate-700/30 rounded-lg p-1 overflow-x-auto">
            <div className="flex gap-1 min-w-max">
              <button
                onClick={() => setActiveTab('info')}
                className={getTabClassName('info', activeTab === 'info')}
              >
                <span className="block sm:hidden">📋</span>
                <span className="hidden sm:block">📋 기본정보</span>
              </button>
              <button
                onClick={() => setActiveTab('posts')}
                className={getTabClassName('posts', activeTab === 'posts')}
              >
                <span className="block sm:hidden">📝</span>
                <span className="hidden sm:block">📝 내게시글</span>
              </button>
              <button
                onClick={() => setActiveTab('comments')}
                className={getTabClassName('comments', activeTab === 'comments')}
              >
                <span className="block sm:hidden">💬</span>
                <span className="hidden sm:block">💬 내댓글</span>
              </button>
              <button
                onClick={() => setActiveTab('icons')}
                className={getTabClassName('icons', activeTab === 'icons')}
              >
                <span className="block sm:hidden">🎨</span>
                <span className="hidden sm:block">🎨 내아이콘</span>
              </button>
              <button
                onClick={() => setActiveTab('messages')}
                className={getTabClassName('messages', activeTab === 'messages')}
              >
                <span className="block sm:hidden">📩</span>
                <span className="hidden sm:block">📩 쪽지함</span>
              </button>
              <button
                onClick={() => setActiveTab('notifications')}
                className={getTabClassName('notifications', activeTab === 'notifications')}
              >
                <span className="block sm:hidden">🔔</span>
                <span className="hidden sm:block">🔔 알림</span>
              </button>
            </div>
          </div>
        </div>

        {/* 콘텐츠 섹션 - 모바일 최적화 */}
        <div className="bg-slate-800/50 rounded-xl p-3 sm:p-6 border border-slate-600/30">
          <ProfileActivityTabs
            activeTab={activeTab}
            profile={profile}
            socialProvider={user?.socialProvider}
            icons={icons}
            activity={activity}
            formatDate={formatDate}
            postsLoading={postsLoading}
            postsPage={postsPage}
            fetchPostsPage={fetchPostsPage}
            commentsLoading={commentsLoading}
            commentsPage={commentsPage}
            fetchCommentsPage={fetchCommentsPage}
            iconLoading={iconLoading}
            handleIconEquip={handleIconEquip}
          />

          {activeTab === 'messages' && (
            <ProfileMessagesTab
              activeBox={messageTab}
              loading={messagesLoading}
              received={receivedMessages}
              sent={sentMessages}
              formatDate={formatDate}
              onBoxChange={setMessageTab}
              onDelete={handleDeleteMessage}
              onMessageClick={handleMessageClick}
              onRefresh={fetchMessages}
            />
          )}

          {activeTab === 'notifications' && (
            <ProfileNotificationsTab
              data={notifications}
              loading={notificationsLoading}
              formatDate={formatDate}
              onDelete={handleDeleteNotification}
              onMarkAllRead={handleMarkAllNotificationsRead}
              onNotificationClick={handleNotificationClick}
            />
          )}
        </div>
        
        {selectedMessage && (
          <MessageDetailModal
            message={selectedMessage}
            formatDate={formatDate}
            onClose={() => setSelectedMessage(null)}
          />
        )}
      </div>
    </div>
  );
}
