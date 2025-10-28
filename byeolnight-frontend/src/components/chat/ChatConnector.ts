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
  private maxRetries = 3;
  private userNickname?: string;
  private reconnectTimeout?: NodeJS.Timeout;

  async connect(callbacks: ChatConnectorCallbacks, userNickname?: string) {
    if (this.ws && this.isConnected) {
      return;
    }

    this.callbacks = callbacks;
    this.userNickname = userNickname;
    
    const wsUrl = import.meta.env.VITE_WS_URL || 
      (window.location.hostname === 'localhost' ? 'ws://localhost:8080/ws' : 
       `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`);
    
    console.log('WebSocket 연결 시도:', { wsUrl, userNickname });
    
    try {
      this.ws = new WebSocket(wsUrl);
      
      this.ws.onopen = () => this.handleConnect();
      this.ws.onmessage = (event) => this.handleMessage(event);
      this.ws.onerror = () => this.handleError();
      this.ws.onclose = () => this.handleDisconnect();
    } catch (error) {
      console.error('WebSocket 생성 실패:', error);
      this.handleError();
    }
  }

  private handleConnect() {
    console.log('WebSocket 연결 성공');
    this.isConnected = true;
    this.retryCount = 0;
    this.callbacks?.onConnect();
  }

  private handleMessage(event: MessageEvent) {
    try {
      const message = JSON.parse(event.data);
      
      if (message.error) {
        this.callbacks?.onBanNotification?.(message);
      } else {
        this.callbacks?.onMessage(message);
      }
    } catch (error) {
      console.error('메시지 파싱 실패:', error);
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
    this.isConnected = false;
    this.callbacks?.onDisconnect();
  }

  sendMessage(message: { roomId: string; sender: string; message: string }) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.error('WebSocket이 연결되어 있지 않습니다.');
      throw new Error('WebSocket이 연결되어 있지 않습니다.');
    }

    try {
      this.ws.send(JSON.stringify(message));
    } catch (error) {
      console.error('메시지 전송 실패:', error);
      throw error;
    }
  }

  disconnect() {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
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
}

export default new ChatConnector();
