import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export const useWebSocket = (destination: string, onMessage: (message: any) => void) => {
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) return;

    // WebSocket 클라이언트 생성
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      debug: (str) => {
        console.log('STOMP: ' + str);
      },
      onConnect: () => {
        console.log('WebSocket 연결됨');
        
        // 구독 설정
        client.subscribe(destination, (message) => {
          try {
            const data = JSON.parse(message.body);
            onMessage(data);
          } catch (error) {
            console.error('메시지 파싱 오류:', error);
          }
        });
      },
      onStompError: (frame) => {
        console.error('STOMP 오류:', frame);
      },
      onWebSocketError: (error) => {
        console.error('WebSocket 오류:', error);
      }
    });

    clientRef.current = client;
    client.activate();

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, [destination, onMessage]);

  return clientRef.current;
};