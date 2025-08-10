import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getNotifications, markAsRead, markAllAsRead, getUnreadCount, deleteNotification } from '../../lib/api/notification';
// import { useWebSocket } from '../../hooks/useWebSocket'; // 임시 비활성화
import type { Notification } from '../../types/notification';
import { NOTIFICATION_ICONS } from '../../types/notification';

export default function NotificationDropdown() {
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // WebSocket 연결 임시 비활성화 (배포 후 수정 예정)
  // useWebSocket((notification) => {
  //   console.log('실시간 알림 수신됨:', notification);
  //   setUnreadCount(prev => prev + 1);
  //   setNotifications(prev => [notification, ...prev.slice(0, 4)]);
  // });

  useEffect(() => {
    if (user) {
      fetchUnreadCount();
      fetchAllNotifications();
      
      // 브라우저 알림 권한 요청 (인앱 브라우저 호환)
      if (typeof Notification !== 'undefined' && Notification.permission === 'default') {
        Notification.requestPermission();
      }
    }
  }, [user]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const fetchUnreadCount = async () => {
    try {
      const count = await getUnreadCount();
      setUnreadCount(count || 0);
    } catch (error) {
      if (import.meta.env.DEV) {
        console.error('읽지 않은 알림 개수 조회 실패:', error);
      }
      setUnreadCount(0);
    }
  };

  const fetchAllNotifications = async () => {
    try {
      setLoading(true);
      const data = await getNotifications({ page: 0, size: 10 });
      setNotifications(data.notifications || []);
    } catch (error) {
      if (import.meta.env.DEV) {
        console.error('알림 조회 실패:', error);
      }
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  };



  const handleNotificationClick = async (notification: Notification) => {
    try {
      if (!notification.isRead) {
        await markAsRead(notification.id);
        setUnreadCount(prev => Math.max(0, prev - 1));
        setNotifications(prev => 
          prev.map(n => n.id === notification.id ? { ...n, isRead: true } : n)
        );
      }
      setIsOpen(false);
    } catch (error) {
      if (import.meta.env.DEV) {
        console.error('알림 읽음 처리 실패:', error);
      }
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await markAllAsRead();
      setUnreadCount(0);
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    } catch (error) {
      if (import.meta.env.DEV) {
        console.error('모든 알림 읽음 처리 실패:', error);
      }
    }
  };

  const handleDeleteNotification = async (notificationId: number, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    try {
      await deleteNotification(notificationId);
      setNotifications(prev => prev.filter(n => n.id !== notificationId));
      setUnreadCount(prev => {
        const notification = notifications.find(n => n.id === notificationId);
        return notification && !notification.isRead ? Math.max(0, prev - 1) : prev;
      });
    } catch (error) {
      if (import.meta.env.DEV) {
        console.error('알림 삭제 실패:', error);
      }
    }
  };

  const formatTimeAgo = (dateString: string) => {
    const now = new Date();
    const date = new Date(dateString);
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));

    if (diffInMinutes < 1) return '방금 전';
    if (diffInMinutes < 60) return `${diffInMinutes}분 전`;
    if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)}시간 전`;
    return `${Math.floor(diffInMinutes / 1440)}일 전`;
  };

  if (!user) return null;

  return (
    <div className="relative" ref={dropdownRef}>
      {/* 알림 버튼 */}
      <button
        onClick={() => {
          setIsOpen(!isOpen);
          if (!isOpen) {
            fetchUnreadCount();
            fetchAllNotifications();
          }
        }}
        className="relative p-2 rounded-lg hover:bg-purple-600/20 text-purple-300 hover:text-purple-200 transition-all duration-200"
        title="알림"
      >
        <span className="text-lg">🔔</span>
        {unreadCount > 0 && (
          <div className="absolute -top-1 -right-1 bg-red-500 text-white text-xs w-5 h-5 rounded-full flex items-center justify-center font-bold">
            {unreadCount > 99 ? '99+' : unreadCount}
          </div>
        )}
      </button>

      {/* 드롭다운 */}
      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-80 bg-[#1f2336]/95 backdrop-blur-md rounded-xl shadow-xl border border-purple-500/20 z-50">
          {/* 헤더 */}
          <div className="flex items-center justify-between p-4 border-b border-purple-500/20">
            <h3 className="text-white font-semibold">알림</h3>
            <div className="flex gap-2">
              {/* 테스트 버튼들 */}

              {unreadCount > 0 && (
                <button
                  onClick={handleMarkAllAsRead}
                  className="text-xs text-purple-300 hover:text-purple-200 transition-colors"
                >
                  모두 읽음
                </button>
              )}
            </div>
          </div>



          {/* 알림 목록 */}
          <div className="max-h-96 overflow-y-auto">
            {loading ? (
              <div className="p-4 text-center text-gray-400">로딩 중...</div>
            ) : notifications.length === 0 ? (
              <div className="p-4 text-center text-gray-400">
                <div className="text-4xl mb-2">🔔</div>
                <p>새로운 알림이 없습니다</p>
              </div>
            ) : (
              notifications.map((notification) => (
                <Link
                  key={notification.id}
                  to={notification.type === 'NEW_MESSAGE' ? '/profile?tab=messages' : (notification.targetUrl || '#')}
                  onClick={() => handleNotificationClick(notification)}
                  className={`block p-4 hover:bg-purple-600/10 transition-colors border-b border-purple-500/10 last:border-b-0 ${
                    !notification.isRead ? 'bg-purple-600/5 border-l-2 border-l-blue-500' : 'opacity-70'
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <div className="text-xl flex-shrink-0 mt-1">
                      {NOTIFICATION_ICONS[notification.type]}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <p className="text-sm font-medium text-white truncate">
                          {notification.title}
                        </p>
                        {!notification.isRead && (
                          <div className="w-2 h-2 bg-blue-500 rounded-full flex-shrink-0"></div>
                        )}
                      </div>
                      <p className="text-xs text-gray-300 line-clamp-2 mb-1">
                        {notification.message}
                      </p>
                      <p className="text-xs text-gray-400">
                        {formatTimeAgo(notification.createdAt)}
                      </p>
                    </div>
                    <button
                      onClick={(e) => handleDeleteNotification(notification.id, e)}
                      className="flex-shrink-0 p-1 hover:bg-red-600/20 rounded text-gray-400 hover:text-red-400 transition-colors"
                      title="알림 삭제"
                    >
                      ×
                    </button>
                  </div>
                </Link>
              ))
            )}
          </div>

          {/* 푸터 */}
          {notifications.length > 0 && (
            <div className="p-3 border-t border-purple-500/20">
              <Link
                to="/profile?tab=notifications"
                className="block text-center text-sm text-purple-300 hover:text-purple-200 transition-colors"
                onClick={() => setIsOpen(false)}
              >
                모든 알림 보기
              </Link>
            </div>
          )}
        </div>
      )}
    </div>
  );
}