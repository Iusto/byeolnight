interface ChatConnectorCallbacks {
  onMessage: (msg: any) => void;
  onConnect: () => void;
  onDisconnect: () => void;
  onError: () => void;
  onBanNotification?: (banData: any) => void;
}

class ChatConnector {
  private ws: WebSocket | null = null;
  private isConnected = false;
  private callbacks: ChatConnectorCallbacks | null = null;
  private retryCount = 0;
  private maxRetries = 5;
  private userNickname?: string;
  private reconnectTimeout?: NodeJS.Timeout;
  private heartbeatInterval?: NodeJS.Timeout;
  private missedHeartbeats = 0;
  private maxMissedHeartbeats = 3;

  async connect(callbacks: ChatConnectorCallbacks, userNickname?: string) {
    if (this.ws && this.isConnected) {
      console.log('이미 연결되어 있음');
      return;
    }

    this.callbacks = callbacks;
    this.userNickname = userNickname;
    
    const wsUrl = import.meta.env.VITE_WS_URL || 
      (window.location.hostname === 'localhost' ? 'ws://localhost:8080/ws' : 
       `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`);
    
    console.log('🔌 WebSocket 연결 시도:', { wsUrl, userNickname, hasToken: document.cookie.includes('accessToken') });
    
    try {
      this.ws = new WebSocket(wsUrl);
      
      this.ws.onopen = () => this.handleConnect();
      this.ws.onmessage = (event) => this.handleMessage(event);
      this.ws.onerror = (error) => {
        console.error('❌ WebSocket 에러:', error);
        this.handleError();
      };
      this.ws.onclose = (event) => {
        console.log('🔌 WebSocket 연결 종료:', event.code, event.reason);
        this.handleDisconnect();
      };
    } catch (error) {
      console.error('❌ WebSocket 생성 실패:', error);
      this.handleError();
    }
  }

  private handleConnect() {
    console.log('✅ WebSocket 연결 성공');
    this.isConnected = true;
    this.retryCount = 0;
    this.missedHeartbeats = 0;
    this.callbacks?.onConnect();
    this.startHeartbeat();
  }

  private handleMessage(event: MessageEvent) {
    try {
      const message = JSON.parse(event.data);
      
      // pong 응답 처리
      if (message.type === 'pong') {
        this.missedHeartbeats = 0;
        return;
      }
      
      console.log('📨 메시지 수신:', message);
      
      if (message.error) {
        this.callbacks?.onBanNotification?.(message);
      } else {
        this.callbacks?.onMessage(message);
      }
    } catch (error) {
      console.error('❌ 메시지 파싱 실패:', error, event.data);
    }
  }

  private handleError() {
    console.error('WebSocket 연결 오류 발생');
    this.isConnected = false;
    this.callbacks?.onError();
    
    if (this.retryCount < this.maxRetries) {
      this.retryCount++;
      console.log(`재연결 시도 ${this.retryCount}/${this.maxRetries}`);
      this.reconnectTimeout = setTimeout(() => {
        if (this.callbacks) {
          this.connect(this.callbacks, this.userNickname);
        }
      }, 3000 * this.retryCount);
    } else {
      console.error('최대 재연결 시도 횟수 초과');
    }
  }

  private handleDisconnect() {
    console.log('🔌 연결 종료 감지, 재연결 시도...');
    this.isConnected = false;
    this.stopHeartbeat();
    this.callbacks?.onDisconnect();
    
    // 자동 재연결 (정상 종료가 아닌 경우)
    if (this.callbacks && this.retryCount < this.maxRetries) {
      this.retryCount++;
      console.log(`자동 재연결 시도 ${this.retryCount}/${this.maxRetries}`);
      this.reconnectTimeout = setTimeout(() => {
        this.connect(this.callbacks!, this.userNickname);
      }, Math.min(3000 * this.retryCount, 15000)); // 최대 15초
    }
  }

  sendMessage(message: { roomId: string; sender: string; message: string }) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.error('❌ WebSocket이 연결되어 있지 않습니다. readyState:', this.ws?.readyState);
      throw new Error('WebSocket이 연결되어 있지 않습니다.');
    }

    try {
      console.log('📤 메시지 전송:', message);
      this.ws.send(JSON.stringify(message));
    } catch (error) {
      console.error('❌ 메시지 전송 실패:', error);
      throw error;
    }
  }

  disconnect() {
    this.stopHeartbeat();
    
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = undefined;
    }
    
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.isConnected = false;
    this.callbacks = null;
  }

  get connected() {
    return this.isConnected && this.ws?.readyState === WebSocket.OPEN;
  }

  async retryConnection() {
    console.log('ChatConnector.retryConnection 호출');
    this.retryCount = 0;
    this.stopHeartbeat();
    
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = undefined;
    }
    
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.isConnected = false;
    
    setTimeout(async () => {
      if (this.callbacks) {
        await this.connect(this.callbacks, this.userNickname);
      }
    }, 500);
  }

  private startHeartbeat() {
    this.stopHeartbeat();
    this.heartbeatInterval = setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        try {
          this.ws.send(JSON.stringify({ type: 'ping' }));
          this.missedHeartbeats++;
          
          if (this.missedHeartbeats >= this.maxMissedHeartbeats) {
            console.warn('⚠️ 하트비트 응답 없음, 연결 재시도');
            this.ws.close();
          }
        } catch (error) {
          console.error('❌ 하트비트 전송 실패:', error);
        }
      }
    }, 30000); // 30초마다
  }

  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = undefined;
    }
    this.missedHeartbeats = 0;
  }
}

export default new ChatConnector();
