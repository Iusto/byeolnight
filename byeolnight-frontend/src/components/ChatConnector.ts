import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export let stompClient: Client | null = null;
let isConnected = false; // ✅ 중복 연결 방지용 flag

export const connectChat = (onMessage: (msg: any) => void) => {
  if (stompClient && isConnected) {
    console.log('🟡 이미 WebSocket 연결됨');
    return;
  }

  const wsUrl = import.meta.env.VITE_WS_URL || '/ws';
  // SockJS는 HTTP/HTTPS 프로토콜을 사용
  const socketUrl = wsUrl;

  // 클라이언트 IP 가져오기
  const clientIp = sessionStorage.getItem('clientIp') || 'unknown';
  
  stompClient = new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    connectHeaders: {
      'X-Client-IP': clientIp,
      'Authorization': `Bearer ${localStorage.getItem('accessToken') || ''}`
    }
  });

  stompClient.onConnect = () => {
    isConnected = true;
    console.log('🟢 WebSocket 연결 완료');

    stompClient?.subscribe('/topic/public', (message) => {
      const body = JSON.parse(message.body);
      onMessage(body);
    });

    stompClient?.subscribe('/user/queue/init', (message) => {
      const history = JSON.parse(message.body);
      if (Array.isArray(history)) {
        history.forEach(onMessage);
      }
    });

    stompClient?.publish({ destination: '/app/chat.init', body: '' });
  };

  stompClient.activate();
};

export const sendMessage = (message: any) => {
  if (stompClient?.connected) {
    const clientIp = sessionStorage.getItem('clientIp') || 'unknown';
    stompClient.publish({ 
      destination: '/app/chat.send', 
      body: JSON.stringify(message),
      headers: {
        'X-Client-IP': clientIp
      }
    });
  } else {
    console.warn('❌ WebSocket 연결되지 않음');
  }
};

export const disconnectChat = () => {
  if (stompClient?.connected) {
    stompClient.onDisconnect = () => {
      console.log('🔌 WebSocket 연결 해제');
      isConnected = false;
    };
    stompClient.deactivate();
  }
};
