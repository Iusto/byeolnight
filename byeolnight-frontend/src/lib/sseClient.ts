/**
 * 이미지 검증 결과를 실시간으로 받기 위한 SSE(Server-Sent Events) 클라이언트
 */

import { v4 as uuidv4 } from 'uuid';

// 검증 결과 콜백 함수 타입
export type ValidationResultCallback = (result: ValidationResult) => void;

// 검증 결과 인터페이스
export interface ValidationResult {
  isValid: boolean;
  message: string;
  imageUrl: string;
  s3Key?: string;
  error?: string;
}

// SSE 클라이언트 클래스
class SseClient {
  private eventSource: EventSource | null = null;
  private clientId: string;
  private callbacks: Map<string, ValidationResultCallback[]> = new Map();
  private baseUrl: string;
  private isConnected: boolean = false;
  private reconnectAttempts: number = 0;
  private maxReconnectAttempts: number = 3;
  
  constructor() {
    // 고유 클라이언트 ID 생성
    this.clientId = localStorage.getItem('sseClientId') || uuidv4();
    localStorage.setItem('sseClientId', this.clientId);
    
    // API 기본 URL 설정
    this.baseUrl = '/api';
  }
  
  /**
   * SSE 연결 시작
   */
  connect(): Promise<boolean> {
    return new Promise((resolve, reject) => {
      if (this.eventSource) {
        this.disconnect();
      }
      
      try {
        const url = `${this.baseUrl}/files/validation-events?clientId=${this.clientId}`;
        this.eventSource = new EventSource(url);
        
        // 연결 성공 이벤트
        this.eventSource.addEventListener('connect', (event: any) => {
          console.log('SSE 연결 성공:', event.data);
          this.isConnected = true;
          this.reconnectAttempts = 0;
          resolve(true);
        });
        
        // 검증 결과 이벤트
        this.eventSource.addEventListener('validationResult', (event: any) => {
          try {
            const result: ValidationResult = JSON.parse(event.data);
            console.log('이미지 검증 결과 수신:', result);
            
            // 해당 이미지 URL에 등록된 모든 콜백 함수 호출
            if (result.imageUrl && this.callbacks.has(result.imageUrl)) {
              const callbacksForUrl = this.callbacks.get(result.imageUrl) || [];
              callbacksForUrl.forEach(callback => {
                try {
                  callback(result);
                } catch (callbackError) {
                  console.error('검증 결과 콜백 실행 중 오류:', callbackError);
                }
              });
              
              // 콜백 실행 후 제거 (일회성)
              this.callbacks.delete(result.imageUrl);
            }
          } catch (parseError) {
            console.error('검증 결과 파싱 오류:', parseError, event.data);
          }
        });
        
        // 오류 처리
        this.eventSource.onerror = (error) => {
          console.error('SSE 연결 오류:', error);
          this.isConnected = false;
          
          // 재연결 시도
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`SSE 재연결 시도 ${this.reconnectAttempts}/${this.maxReconnectAttempts}...`);
            setTimeout(() => this.connect(), 2000);
          } else {
            console.error('최대 재연결 시도 횟수 초과');
            reject(error);
          }
        };
      } catch (error) {
        console.error('SSE 연결 생성 실패:', error);
        this.isConnected = false;
        reject(error);
      }
    });
  }
  
  /**
   * SSE 연결 종료
   */
  disconnect() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      this.isConnected = false;
      console.log('SSE 연결 종료');
    }
  }
  
  /**
   * 이미지 검증 요청 및 콜백 등록
   * @param imageUrl 검증할 이미지 URL
   * @param callback 검증 결과를 받을 콜백 함수
   */
  validateImage(imageUrl: string, callback: ValidationResultCallback) {
    // 콜백 등록
    if (!this.callbacks.has(imageUrl)) {
      this.callbacks.set(imageUrl, []);
    }
    this.callbacks.get(imageUrl)?.push(callback);
    
    // 연결 확인 및 필요시 연결
    if (!this.isConnected) {
      this.connect().catch(error => {
        console.error('SSE 연결 실패:', error);
        // 연결 실패 시 콜백에 오류 전달
        callback({
          isValid: false,
          message: 'SSE 연결 실패로 실시간 검증 결과를 받을 수 없습니다.',
          imageUrl,
          error: 'CONNECTION_ERROR'
        });
      });
    }
    
    // 검증 요청 (fetch 또는 axios 사용)
    fetch(`${this.baseUrl}/files/validate-image?imageUrl=${encodeURIComponent(imageUrl)}&clientId=${this.clientId}`, {
      method: 'POST'
    }).catch(error => {
      console.error('이미지 검증 요청 실패:', error);
      // 요청 실패 시 콜백에 오류 전달
      callback({
        isValid: false,
        message: '이미지 검증 요청 실패',
        imageUrl,
        error: 'REQUEST_ERROR'
      });
    });
  }
  
  /**
   * 클라이언트 ID 반환
   */
  getClientId(): string {
    return this.clientId;
  }
  
  /**
   * 연결 상태 확인
   */
  isConnectedToServer(): boolean {
    return this.isConnected;
  }
}

// 싱글톤 인스턴스 생성
const sseClient = new SseClient();

export default sseClient;