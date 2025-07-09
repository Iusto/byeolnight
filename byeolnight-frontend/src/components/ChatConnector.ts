import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export let stompClient: Client | null = null;
let isConnected = false; // ✅ 중복 연결 방지용 flag

export const connectChat = (onMessage: (msg: any) => void) => {
  if (stompClient && isConnected) {
    console.log('🟡 이미 WebSocket 연결됨');
    return;
  }

  stompClient = new Client({
    webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_URL || '/ws'),
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
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
    stompClient.publish({ destination: '/app/chat.send', body: JSON.stringify(message) });
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
