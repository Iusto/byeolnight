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
    
    // HttpOnly 쿠키 방식이므로 토큰 헤더 제거, 쿠키 자동 전달
    this.client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      // connectHeaders 제거 - 쿠키가 자동으로 전달됨
      onConnect: () => this.handleConnect(userNickname),
      onStompError: () => this.handleError(),
      onWebSocketError: () => this.handleError(),
      onDisconnect: () => this.handleDisconnect()
    });

    this.client.activate();
  }

  private handleConnect(userNickname?: string) {
    this.isConnected = true;
    this.retryCount = 0;
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
    
    // 사용자 닉네임 저장
    this.userNickname = userNickname;
  }

  private handleError() {
    this.isConnected = false;
    this.callbacks?.onError();
    
    if (this.retryCount < this.maxRetries) {
      this.retryCount++;
      setTimeout(() => {
        if (this.callbacks) {
          this.connect(this.callbacks);
        }
      }, 3000 * this.retryCount);
    }
  }

  private handleDisconnect() {
    this.isConnected = false;
    this.callbacks?.onDisconnect();
  }

  sendMessage(message: { roomId: string; sender: string; message: string }) {
    if (!this.client?.connected) {
      throw new Error('WebSocket이 연결되어 있지 않습니다.');
    }

    this.client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(message)
    });
  }

  disconnect() {
    if (this.client?.connected) {
      this.client.deactivate();
    }
    this.isConnected = false;
    this.callbacks = null;
  }

  get connected() {
    return this.isConnected && this.client?.connected;
  }

  retryConnection() {
    this.retryCount = 0;
    this.disconnect(); // 기존 연결 완전 종료
    if (this.callbacks) {
      this.connect(this.callbacks, this.userNickname);
    }
  }
}

export default new ChatConnector();
