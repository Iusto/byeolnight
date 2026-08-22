interface ChatConnectorCallbacks {
  onMessage: (msg: any) => void;
  onConnect: () => void;
  onDisconnect: (willReconnect: boolean) => void;
  onError: () => void;
  onBanNotification?: (banData: any) => void;
}

class ChatConnector {
  private ws: WebSocket | null = null;
  private isConnected = false;
  private callbacks: ChatConnectorCallbacks | null = null;
  private retryCount = 0;
  private maxRetries = 5;
  private readonly stableConnectionResetMs = 120000;
  private userNickname?: string;
  private reconnectTimeout?: NodeJS.Timeout;
  private stableConnectionTimeout?: NodeJS.Timeout;
  private heartbeatInterval?: NodeJS.Timeout;
  private missedHeartbeats = 0;
  private maxMissedHeartbeats = 3;

  async connect(callbacks: ChatConnectorCallbacks, userNickname?: string) {
    // 연결 중(CONNECTING)인 소켓도 중복 연결로 간주해야 소켓이 두 개 생기지 않음
    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
      console.log('이미 연결되어 있음');
      return;
    }

    this.clearReconnectTimeout();
    this.callbacks = callbacks;
    this.userNickname = userNickname;

    const wsUrl = import.meta.env.VITE_WS_URL ||
      (window.location.hostname === 'localhost' ? 'ws://localhost:8080/ws' :
       `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`);

    console.log('🔌 WebSocket 연결 시도:', { wsUrl, userNickname, hasToken: document.cookie.includes('accessToken') });

    try {
      const socket = new WebSocket(wsUrl);
      this.ws = socket;

      // 이전 소켓의 이벤트가 뒤늦게 도착해 현재 연결 상태를 덮어쓰지 않도록
      // 모든 핸들러에서 자기 소켓인지 확인한다
      socket.onopen = () => {
        if (this.ws !== socket) return;
        this.handleConnect();
      };
      socket.onmessage = (event) => {
        if (this.ws !== socket) return;
        this.handleMessage(event);
      };
      socket.onerror = (error) => {
        if (this.ws !== socket) return;
        console.error('❌ WebSocket 에러:', error);
        this.handleError();
      };
      socket.onclose = (event) => {
        if (this.ws !== socket) return;
        console.log('🔌 WebSocket 연결 종료:', event.code, event.reason);
        this.ws = null;
        this.handleDisconnect();
      };
    } catch (error) {
      console.error('❌ WebSocket 생성 실패:', error);
      this.ws = null;
      this.isConnected = false;
      this.callbacks?.onError();
      this.scheduleReconnect();
    }
  }

  private handleConnect() {
    console.log('✅ WebSocket 연결 성공');
    this.isConnected = true;
    this.missedHeartbeats = 0;
    this.clearReconnectTimeout();
    this.scheduleRetryCountReset();
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

  // onerror 뒤에는 항상 onclose가 이어지므로 재연결 예약은 handleDisconnect에서만 한다
  private handleError() {
    console.error('WebSocket 연결 오류 발생');
    this.isConnected = false;
  }

  private handleDisconnect() {
    console.log('🔌 연결 종료 감지');
    this.isConnected = false;
    this.stopHeartbeat();
    this.clearStableConnectionTimeout();

    const willReconnect = !!this.callbacks && this.retryCount < this.maxRetries;
    this.callbacks?.onDisconnect(willReconnect);
    this.scheduleReconnect();
  }

  private scheduleReconnect() {
    if (!this.callbacks) return;

    // 재연결 타이머는 항상 하나만 유지 (중복 예약 시 소켓이 여러 개 생김)
    this.clearReconnectTimeout();

    if (this.retryCount >= this.maxRetries) {
      console.error('최대 재연결 시도 횟수 초과');
      this.callbacks.onError();
      return;
    }

    this.retryCount++;
    const delay = Math.min(3000 * this.retryCount, 15000);
    console.log(`자동 재연결 시도 ${this.retryCount}/${this.maxRetries} (${delay}ms 후)`);

    this.reconnectTimeout = setTimeout(() => {
      this.reconnectTimeout = undefined;
      if (this.callbacks) {
        this.connect(this.callbacks, this.userNickname);
      }
    }, delay);
  }

  private clearReconnectTimeout() {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = undefined;
    }
  }

  private scheduleRetryCountReset() {
    this.clearStableConnectionTimeout();

    // 소켓이 열리자마자 재시도 횟수를 초기화하면
    // 연결 즉시 종료되는 장애에서 영원히 재연결하게 된다.
    this.stableConnectionTimeout = setTimeout(() => {
      if (this.connected) {
        this.retryCount = 0;
      }
      this.stableConnectionTimeout = undefined;
    // 하트비트 실패 판정(최대 90초)보다 길게 유지된 연결만 정상 복구로 본다.
    }, this.stableConnectionResetMs);
  }

  private clearStableConnectionTimeout() {
    if (this.stableConnectionTimeout) {
      clearTimeout(this.stableConnectionTimeout);
      this.stableConnectionTimeout = undefined;
    }
  }

  // 소켓을 버릴 때 핸들러를 먼저 떼어내야 뒤늦게 오는 close 이벤트가
  // 새 연결 상태를 건드리지 않는다
  private discardSocket() {
    const socket = this.ws;
    this.ws = null;
    this.isConnected = false;

    if (!socket) return;
    socket.onopen = null;
    socket.onmessage = null;
    socket.onerror = null;
    socket.onclose = null;
    socket.close();
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
    this.clearReconnectTimeout();
    this.clearStableConnectionTimeout();
    this.discardSocket();
    this.callbacks = null;
  }

  get connected() {
    return this.isConnected && this.ws?.readyState === WebSocket.OPEN;
  }

  async retryConnection() {
    console.log('ChatConnector.retryConnection 호출');
    this.retryCount = 0;
    this.stopHeartbeat();
    this.clearReconnectTimeout();
    this.clearStableConnectionTimeout();
    this.discardSocket();

    if (this.callbacks) {
      await this.connect(this.callbacks, this.userNickname);
    }
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
