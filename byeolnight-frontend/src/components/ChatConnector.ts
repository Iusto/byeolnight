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
    
    // SockJS에 withCredentials 옵션 설정
    const sockJS = new SockJS(wsUrl, null, {
      transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
      withCredentials: true
    });
    
    this.client = new Client({
      webSocketFactory: () => sockJS,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
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
    this.retryCount = 0;
    this.disconnect(); // 기존 연결 완전 종료
    if (this.callbacks) {
      this.connect(this.callbacks, this.userNickname);
    }
  }
}

export default new ChatConnector();
