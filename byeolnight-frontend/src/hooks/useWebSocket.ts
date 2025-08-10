import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export const useWebSocket = (onNotification?: (notification: any) => void) => {
  const clientRef = useRef<Client | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (!onNotification) return;

    const connect = () => {
      try {
        const client = new Client({
          webSocketFactory: () => new SockJS('/ws'),
          connectHeaders: {},
          reconnectDelay: 5000,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          
          onConnect: () => {
            if (import.meta.env.DEV) {
              console.log('WebSocket 연결 성공');
            }
            
            // 연결 성공 시 바로 구독
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
            
            // 3초 후 재연결 시도
            reconnectTimeoutRef.current = setTimeout(() => {
              if (clientRef.current?.connected === false) {
                connect();
              }
            }, 3000);
          }
        });

        clientRef.current = client;
        client.activate();

      } catch (error) {
        if (import.meta.env.DEV) {
          console.error('WebSocket 초기화 오류:', error);
        }
      }
    };

    connect();

    return () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      
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