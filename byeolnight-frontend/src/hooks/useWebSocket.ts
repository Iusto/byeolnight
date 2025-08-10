import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export const useWebSocket = (onNotification?: (notification: any) => void) => {
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!onNotification) return;

    try {
      const client = new Client({
        webSocketFactory: () => new SockJS('/ws'),
        // HttpOnly 쿠키 환경에서는 쿠키가 자동으로 전송됨
        connectHeaders: {},
        onConnect: () => {
          if (import.meta.env.DEV) {
            console.log('WebSocket 연결 성공');
          }
          
          // 연결 상태 확인 후 구독
          if (client.connected) {
            client.subscribe('/user/queue/notifications', (message) => {
              try {
                const notification = JSON.parse(message.body);
                onNotification(notification);
              } catch (error) {
                if (import.meta.env.DEV) {
                  console.error('알림 파싱 오류:', error);
                }
              }
            });
          }
        },
        onStompError: (frame) => {
          if (import.meta.env.DEV) {
            console.error('STOMP 오류:', frame);
          }
        },
        onWebSocketError: (error) => {
          if (import.meta.env.DEV) {
            console.error('WebSocket 오류:', error);
          }
        },
        onDisconnect: () => {
          if (import.meta.env.DEV) {
            console.log('WebSocket 연결 끊어짐');
          }
        }
      });

      clientRef.current = client;
      client.activate();

    } catch (error) {
      if (import.meta.env.DEV) {
        console.error('WebSocket 초기화 오류:', error);
      }
    }

    return () => {
      if (clientRef.current) {
        try {
          clientRef.current.deactivate();
        } catch (error) {
          // 연결 끊기 오류는 무시
        }
      }
    };
  }, [onNotification]);

  return clientRef.current;
};