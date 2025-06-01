import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const SOCKET_URL = 'http://localhost:8080/ws/chat';
const token = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwicm9sZSI6IlVTRVIiLCJleHAiOjE3NDg3NjU2ODd9.NflQwvPfHbTlLQSltdVu-A5i86ylJsOsalTmETiiXGE'

export const createStompClient = (token: string) => {
  const client = new Client({
    webSocketFactory: () => new SockJS(SOCKET_URL),
    connectHeaders: {
      Authorization: token, // 🔐 여기에 토큰 삽입 (Bearer 생략)
    },
    reconnectDelay: 5000,
    debug: (str) => console.log('[STOMP]', str),
  });

  return client;
};