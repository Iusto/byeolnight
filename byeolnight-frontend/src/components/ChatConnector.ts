import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

interface ChatConnectorCallbacks {
  onMessage: (msg: any) => void;
  onConnect: () => void;
  onDisconnect: () => void;
  onError: () => void;
  onBanNotification?: (banData: any) => void;
  onAdminUpdate?: (data: any) => void;
}

class ChatConnector {
  private client: Client | null = null;
  private isConnected = false;
  private callbacks: ChatConnectorCallbacks | null = null;
  private retryCount = 0;
  private maxRetries = 3;
  private userNickname?: string;

  connect(callbacks: ChatConnectorCallbacks, userNickname?: string) {
    if (this.client && this.isConnected) {
      return;
    }

    this.callbacks = callbacks;
    const wsUrl = import.meta.env.VITE_WS_URL || '/ws';
    
    // 토큰 추출 (쿠키에서만 - HttpOnly 쿠키 방식)
    const token = this.getCookieValue('accessToken');
    const connectHeaders: any = {};
    
    console.log('WebSocket 연결 시도:', { 
      token: token ? '존재' : '없음', 
      userNickname,
      cookieExists: document.cookie.includes('accessToken')
    });
    
    if (token) {
      connectHeaders['Authorization'] = `Bearer ${token}`;
      console.log('Authorization 헤더 설정 완료');
    } else {
      console.warn('토큰을 찾을 수 없습니다. 비로그인 사용자로 연결됩니다.');
    }
    
    this.client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      connectHeaders,
      onConnect: () => this.handleConnect(userNickname),
      onStompError: () => this.handleError(),
      onWebSocketError: () => this.handleError(),
      onDisconnect: () => this.handleDisconnect()
    });

    this.client.activate();
  }

  private handleConnect(userNickname?: string) {
    console.log('WebSocket 연결 성공:', { userNickname });
    this.isConnected = true;
    this.retryCount = 0;
    this.userNickname = userNickname;
    this.callbacks?.onConnect();

    if (!this.client) return;

    // 공개 채팅 구독
    this.client.subscribe('/topic/public', (message) => {
      const body = JSON.parse(message.body);
      this.callbacks?.onMessage(body);
    });

    // 관리자 업데이트 구독
    this.client.subscribe('/topic/admin/chat-update', (message) => {
      const data = JSON.parse(message.body);
      this.callbacks?.onAdminUpdate?.(data);
    });

    // 사용자별 알림 구독
    if (userNickname) {
      this.client.subscribe(`/queue/user.${userNickname}.ban`, (message) => {
        const banData = JSON.parse(message.body);
        this.callbacks?.onBanNotification?.(banData);
      });

      this.client.subscribe(`/user/queue/ban-notification`, (message) => {
        const errorData = JSON.parse(message.body);
        this.callbacks?.onBanNotification?.(errorData);
      });
    }
  }

  private handleError() {
    console.error('WebSocket 연결 오류 발생');
    this.isConnected = false;
    this.callbacks?.onError();
    
    if (this.retryCount < this.maxRetries) {
      this.retryCount++;
      console.log(`재연결 시도 ${this.retryCount}/${this.maxRetries}`);
      setTimeout(() => {
        if (this.callbacks) {
          this.connect(this.callbacks, this.userNickname);
        }
      }, 3000 * this.retryCount);
    } else {
      console.error('최대 재연결 시도 횟수 초과');
      this.callbacks?.onError();
    }
  }

  private handleDisconnect() {
    this.isConnected = false;
    this.callbacks?.onDisconnect();
  }

  sendMessage(message: { roomId: string; sender: string; message: string }) {
    console.log('ChatConnector.sendMessage 호출:', {
      clientConnected: this.client?.connected,
      isConnected: this.isConnected,
      clientActive: this.client?.active,
      message
    });
    
    if (!this.client) {
      console.error('STOMP 클라이언트가 없습니다.');
      throw new Error('STOMP 클라이언트가 초기화되지 않았습니다.');
    }
    
    if (!this.client.connected || !this.client.active) {
      console.error('WebSocket이 연결되어 있지 않습니다.', {
        connected: this.client.connected,
        active: this.client.active
      });
      throw new Error('WebSocket이 연결되어 있지 않습니다.');
    }

    try {
      console.log('메시지 전송 시도:', message);
      this.client.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(message)
      });
      console.log('메시지 전송 성공');
    } catch (error) {
      console.error('메시지 전송 실패:', error);
      throw error;
    }
  }

  disconnect() {
    if (this.client?.connected) {
      this.client.deactivate();
    }
    this.isConnected = false;
    this.callbacks = null;
  }

  get connected() {
    return this.isConnected && this.client?.connected && this.client?.active;
  }

  retryConnection() {
    console.log('ChatConnector.retryConnection 호출');
    this.retryCount = 0;
    this.disconnect(); // 기존 연결 완전 종료
    
    // 잠시 대기 후 재연결 시도 (최신 토큰으로)
    setTimeout(() => {
      if (this.callbacks) {
        console.log('재연결 시도 시작 - 최신 토큰으로 연결');
        // 최신 토큰 상태로 재연결
        this.connect(this.callbacks, this.userNickname);
      }
    }, 1000);
  }

  private getCookieValue(name: string): string | null {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop()?.split(';').shift() || null;
    }
    return null;
  }
}

export default new ChatConnector();
