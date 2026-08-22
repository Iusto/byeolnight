import type { MouseEvent } from 'react';
import type { Notification, NotificationListResponse } from '../../types/notification';

interface ProfileNotificationsTabProps {
  data: NotificationListResponse;
  loading: boolean;
  formatDate: (date?: string) => string;
  onDelete: (notificationId: number, event: MouseEvent) => void;
  onMarkAllRead: () => void;
  onNotificationClick: (notification: Notification) => void;
}

const notificationTypeLabel: Partial<Record<Notification['type'], string>> = {
  COMMENT_ON_POST: '게시글 댓글',
  REPLY_ON_COMMENT: '댓글 답글',
  NEW_MESSAGE: '새 쪽지',
  NEW_NOTICE: '공지사항',
};

/** 프로필 화면에서 알림 목록의 표현만 담당합니다. */
export default function ProfileNotificationsTab({
  data,
  loading,
  formatDate,
  onDelete,
  onMarkAllRead,
  onNotificationClick,
}: ProfileNotificationsTabProps) {
  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg sm:text-2xl font-bold text-white">🔔 알림</h2>
        <button
          onClick={onMarkAllRead}
          className="px-3 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg text-xs sm:text-sm transition-colors"
        >
          모두 읽음
        </button>
      </div>

      {loading ? (
        <div className="text-center py-8 text-gray-400">로딩 중...</div>
      ) : data.notifications.length === 0 ? (
        <div className="text-center py-8 text-gray-400">알림이 없습니다.</div>
      ) : (
        <div className="space-y-3">
          {data.notifications.map(notification => (
            <div
              key={notification.id}
              onClick={() => onNotificationClick(notification)}
              className={[
                'p-3 sm:p-4 rounded-lg border cursor-pointer transition-all hover:bg-slate-700/50',
                notification.isRead
                  ? 'bg-slate-800/30 border-gray-600 opacity-70'
                  : 'bg-slate-700/50 border-purple-500/50',
              ].join(' ')}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-sm font-medium text-purple-400">
                      {notificationTypeLabel[notification.type] ?? '알림'}
                    </span>
                    {!notification.isRead && <span className="w-2 h-2 bg-red-500 rounded-full" />}
                  </div>
                  <h3 className="font-medium text-white mb-1">{notification.title}</h3>
                  <p className="text-sm text-gray-300 mb-2">{notification.message}</p>
                  <p className="text-xs text-gray-500">{formatDate(notification.createdAt)}</p>
                </div>
                <button
                  onClick={event => onDelete(notification.id, event)}
                  className="ml-2 p-2 text-gray-400 hover:text-red-400 hover:bg-red-500 hover:bg-opacity-10 rounded transition-colors flex-shrink-0 min-w-[44px] min-h-[44px] flex items-center justify-center"
                  title="알림 삭제"
                >
                  ✕
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
