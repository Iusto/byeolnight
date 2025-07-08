import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from '../contexts/AuthContext';

export const useWebSocket = (onNotification?: (notification: any) => void) => {
  const clientRef = useRef<Client | null>(null);
  const { user } = useAuth();
  const onNotificationRef = useRef(onNotification);

  // onNotification 콜백을 ref로 저장하여 의존성 배열에서 제외
  useEffect(() => {
    onNotificationRef.current = onNotification;
  }, [onNotification]);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token || !user) {
      return;
    }

    // 이미 연결된 클라이언트가 있으면 재사용
    if (clientRef.current && clientRef.current.connected) {
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('WebSocket 연결 성공');
        
        if (onNotificationRef.current) {
          client.subscribe(`/user/queue/notifications`, (message) => {
            try {
              const notification = JSON.parse(message.body);
              onNotificationRef.current?.(notification);
            } catch (error) {
              console.error('알림 메시지 파싱 오류:', error);
            }
          });
          
          client.subscribe(`/topic/notifications/${user.id}`, (message) => {
            try {
              const notification = JSON.parse(message.body);
              onNotificationRef.current?.(notification);
            } catch (error) {
              console.error('브로드캐스트 알림 파싱 오류:', error);
            }
          });
        }
      },
      onStompError: (frame) => {
        console.error('STOMP 오류:', frame.headers?.message);
      },
      onWebSocketError: (error) => {
        console.error('WebSocket 오류:', error);
      }
    });

    clientRef.current = client;
    client.activate();

    return () => {
      if (clientRef.current && clientRef.current.connected) {
        clientRef.current.deactivate();
      }
    };
  }, [user?.id]); // user.id만 의존성으로 사용

  return clientRef.current;
};