import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from '../contexts/AuthContext';

export const useWebSocket = (onNotification?: (notification: any) => void) => {
  const clientRef = useRef<Client | null>(null);
  const { user } = useAuth();

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token || !user) {
      console.log('WebSocket 연결 실패: 토큰 또는 사용자 정보 없음');
      return;
    }

    console.log('WebSocket 연결 시도 - userId:', user.id);

    // WebSocket 클라이언트 생성
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      debug: (str) => {
        console.log('STOMP Debug:', str);
      },
      onConnect: () => {
        console.log('WebSocket 연결 성공');
        
        // 개인 알림 구독
        if (onNotification) {
          client.subscribe(`/user/queue/notifications`, (message) => {
            try {
              const notification = JSON.parse(message.body);
              console.log('실시간 알림 수신:', notification);
              onNotification(notification);
            } catch (error) {
              console.error('알림 메시지 파싱 오류:', error);
            }
          });
          
          // 브로드캐스트 알림도 구독 (디버깅용)
          client.subscribe(`/topic/notifications/${user.id}`, (message) => {
            try {
              const notification = JSON.parse(message.body);
              console.log('브로드캐스트 알림 수신:', notification);
              onNotification(notification);
            } catch (error) {
              console.error('브로드캐스트 알림 파싱 오류:', error);
            }
          });
        }
      },
      onStompError: (frame) => {
        console.error('STOMP 오류:', frame);
      },
      onWebSocketError: (error) => {
        console.error('WebSocket 오류:', error);
      },
      onDisconnect: () => {
        console.log('WebSocket 연결 해제됨');
      }
    });

    clientRef.current = client;
    client.activate();

    return () => {
      console.log('WebSocket 정리 중...');
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, [user, onNotification]);

  return clientRef.current;
};