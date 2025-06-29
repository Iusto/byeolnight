import React, { useState, useEffect, useRef } from 'react';
import { NotificationDto } from '../../types/notification';
import { notificationApi } from '../../lib/api/notification';
import { useWebSocket } from '../../hooks/useWebSocket';

const NotificationDropdown: React.FC = () => {
  const [notifications, setNotifications] = useState<NotificationDto.Response[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // WebSocket으로 실시간 알림 수신
  useWebSocket('/user/queue/notifications', (notification: NotificationDto.Response) => {
    setNotifications(prev => [notification, ...prev.slice(0, 9)]);
    setUnreadCount(prev => prev + 1);
  });

  useEffect(() => {
    loadNotifications();
    loadUnreadCount();
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const loadNotifications = async () => {
    try {
      setLoading(true);
      const response = await notificationApi.getNotifications(0, 10);
      setNotifications(response.content);
    } catch (error) {
      console.error('알림 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadUnreadCount = async () => {
    try {
      const count = await notificationApi.getUnreadCount();
      setUnreadCount(count);
    } catch (error) {
      console.error('읽지 않은 알림 수 로드 실패:', error);
    }
  };

  const handleNotificationClick = async (notification: NotificationDto.Response) => {
    if (!notification.isRead) {
      try {
        await notificationApi.markAsRead(notification.id);
        setNotifications(prev => 
          prev.map(n => n.id === notification.id ? { ...n, isRead: true } : n)
        );
        setUnreadCount(prev => Math.max(0, prev - 1));
      } catch (error) {
        console.error('알림 읽음 처리 실패:', error);
      }
    }

    // 알림 타입에 따라 해당 페이지로 이동
    switch (notification.type) {
      case 'MESSAGE':
        window.location.href = `/messages/${notification.referenceId}`;
        break;
      case 'COMMENT':
      case 'REPLY':
        window.location.href = `/posts/${notification.referenceId}`;
        break;
    }
    
    setIsOpen(false);
  };

  const handleDelete = async (notificationId: number, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await notificationApi.deleteNotification(notificationId);
      setNotifications(prev => prev.filter(n => n.id !== notificationId));
    } catch (error) {
      console.error('알림 삭제 실패:', error);
    }
  };

  const handleClearAll = async () => {
    if (confirm('모든 알림을 삭제하시겠습니까?')) {
      try {
        await notificationApi.clearAllNotifications();
        setNotifications([]);
        setUnreadCount(0);
      } catch (error) {
        console.error('알림 전체 삭제 실패:', error);
      }
    }
  };

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'MESSAGE':
        return '💌';
      case 'COMMENT':
        return '💬';
      case 'REPLY':
        return '↩️';
      default:
        return '🔔';
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 text-gray-600 hover:text-gray-900 focus:outline-none"
      >
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                d="M15 17h5l-5 5v-5zM11 19H6.5A2.5 2.5 0 014 16.5v-9A2.5 2.5 0 016.5 5h11A2.5 2.5 0 0120 7.5v3.5" />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-lg shadow-lg border z-50">
          <div className="p-4 border-b flex justify-between items-center">
            <h3 className="font-semibold text-gray-900">알림</h3>
            {notifications.length > 0 && (
              <button
                onClick={handleClearAll}
                className="text-sm text-red-600 hover:text-red-800"
              >
                전체 삭제
              </button>
            )}
          </div>
          
          <div className="max-h-96 overflow-y-auto">
            {loading ? (
              <div className="p-4 text-center text-gray-500">로딩 중...</div>
            ) : notifications.length === 0 ? (
              <div className="p-4 text-center text-gray-500">알림이 없습니다</div>
            ) : (
              notifications.map((notification) => (
                <div
                  key={notification.id}
                  onClick={() => handleNotificationClick(notification)}
                  className={`p-3 border-b hover:bg-gray-50 cursor-pointer ${
                    !notification.isRead ? 'bg-blue-50' : ''
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-start space-x-2 flex-1">
                      <span className="text-lg">{getNotificationIcon(notification.type)}</span>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900 truncate">
                          {notification.title}
                        </p>
                        <p className="text-sm text-gray-600 mt-1 line-clamp-2">
                          {notification.content}
                        </p>
                        <p className="text-xs text-gray-400 mt-1">
                          {new Date(notification.createdAt).toLocaleString()}
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={(e) => handleDelete(notification.id, e)}
                      className="text-gray-400 hover:text-red-600 ml-2"
                    >
                      ×
                    </button>
                  </div>
                  {!notification.isRead && (
                    <div className="w-2 h-2 bg-blue-500 rounded-full absolute left-1 top-1/2 transform -translate-y-1/2"></div>
                  )}
                </div>
              ))
            )}
          </div>
          
          {notifications.length > 0 && (
            <div className="p-3 border-t text-center">
              <button
                onClick={() => {
                  window.location.href = '/notifications';
                  setIsOpen(false);
                }}
                className="text-sm text-blue-600 hover:text-blue-800"
              >
                모든 알림 보기
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default NotificationDropdown;