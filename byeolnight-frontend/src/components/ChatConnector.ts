import SockJS from 'sockjs-client';
import { Client, over } from 'stompjs';

export let stompClient: Client | null = null;
let isConnected = false; // ✅ 중복 연결 방지용 flag

export const connectChat = (onMessage: (msg: any) => void) => {
  if (stompClient && isConnected) {
    console.log('🟡 이미 WebSocket 연결됨');
    return;
  }

  const socket = new SockJS('http://localhost:8080/ws');
  stompClient = over(socket);

  stompClient.connect({}, () => {
    isConnected = true;
    console.log('🟢 WebSocket 연결 완료');

    // ✅ 구독 전에 기존 구독 제거할 수 없음 => 중복 호출 자체를 막아야 함
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

    stompClient.send('/app/chat.init', {}, '');
  });
};

export const sendMessage = (message: any) => {
  if (stompClient?.connected) {
    stompClient.send('/app/chat.send', {}, JSON.stringify(message));
  } else {
    console.warn('❌ WebSocket 연결되지 않음');
  }
};

export const disconnectChat = () => {
  if (stompClient?.connected) {
    stompClient.disconnect(() => {
      console.log('🔌 WebSocket 연결 해제');
      isConnected = false;
    });
  }
};
