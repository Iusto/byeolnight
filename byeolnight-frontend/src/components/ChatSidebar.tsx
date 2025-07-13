import { useContext, useEffect, useRef, useState } from 'react';
import { AuthContext } from '../contexts/AuthContext';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from '../lib/axios';
import AdminChatControls from './AdminChatControls';
import AdminChatModal from './AdminChatModal';

interface ChatMessage {
  id?: string;
  sender: string;
  message: string;
  timestamp: string;
  isBlinded?: boolean;
}

export default function ChatSidebar() {
  const { user } = useContext(AuthContext);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [error, setError] = useState('');
  const [connecting, setConnecting] = useState(true);
  const [connected, setConnected] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [hasMoreHistory, setHasMoreHistory] = useState(true);
  const [oldestMessageId, setOldestMessageId] = useState<string | null>(null);
  const [bannedUsers, setBannedUsers] = useState<Set<string>>(new Set());
  const [showAdminDashboard, setShowAdminDashboard] = useState(false);
  const [banStatus, setBanStatus] = useState<{banned: boolean, reason?: string, duration?: number, bannedUntil?: string} | null>(null);
  const [remainingTime, setRemainingTime] = useState<number>(0);
  const retryCount = useRef(0);
  const stompClientRef = useRef<Client | null>(null);
  const scrollContainerRef = useRef<HTMLDivElement | null>(null); // ✅ 채팅창 컨테이너 ref

  const connect = () => {
    try {
      // WebSocket URL 설정
      const wsUrl = import.meta.env.VITE_WS_URL || 'ws://byeolnight.com/ws';
      const socketUrl = wsUrl.replace('ws://', 'http://').replace('wss://', 'https://');
      console.log('WebSocket 연결 시도:', wsUrl);
      console.log('개발 모드:', import.meta.env.DEV);
      
      const socket = new SockJS(socketUrl);
      const token = localStorage.getItem('accessToken');
      const connectHeaders: Record<string, string> = {};
      
      // 토큰이 있을 때만 Authorization 헤더 추가
      if (token) {
        connectHeaders.Authorization = `Bearer ${token}`;
      }
      
      const client = new Client({
        webSocketFactory: () => socket,
        connectHeaders,
        debug: (str) => console.log('STOMP Debug:', str),
        onConnect: (frame) => {
          console.log('WebSocket 연결 성공:', frame);
          setConnecting(false);
          setConnected(true);
          setError('');
          retryCount.current = 0;

          client.subscribe('/topic/public', (message) => {
            const payload = JSON.parse(message.body);
            setMessages((prev) => [...prev.slice(-10), payload]); // 최신 10개 유지
          });
          
          // 관리자 액션 알림 구독 (블라인드 해제 등)
          client.subscribe('/topic/admin/chat-update', (message) => {
            const data = JSON.parse(message.body);
            if (data.type === 'MESSAGE_UNBLINDED') {
              // 블라인드 해제된 메시지 업데이트
              setMessages(prev => 
                prev.map(msg => 
                  msg.id === data.messageId ? { ...msg, isBlinded: false } : msg
                )
              );
            } else if (data.type === 'MESSAGE_BLINDED') {
              // 블라인드 처리된 메시지 업데이트
              setMessages(prev => 
                prev.map(msg => 
                  msg.id === data.messageId ? { ...msg, isBlinded: true } : msg
                )
              );
            }
          });
          
          // 개인 채팅 금지 알림 구독
          if (user) {
            client.subscribe(`/queue/user.${user.nickname}.ban`, (message) => {
              const banData = JSON.parse(message.body);
              console.log('WebSocket으로 받은 금지 알림:', banData);
              
              if (banData.banned) {
                // 금지 종료 시간 계산
                const endTime = new Date().getTime() + (banData.duration * 60 * 1000);
                const newBanStatus = {
                  banned: true,
                  reason: banData.reason,
                  duration: banData.duration,
                  bannedUntil: new Date(endTime).toISOString()
                };
                setBanStatus(newBanStatus);
                setError(`채팅이 제한되었습니다: ${banData.reason}`);
              } else {
                setError('');
                setBanStatus(null);
                setRemainingTime(0);
              }
            });
            
            // WebSocket 연결 후 즉시 금지 상태 확인
            checkBanStatus();
          }
        },
        onStompError: (frame) => {
          console.error('STOMP 오류:', frame);
          handleConnectionError();
        },
        onWebSocketError: (event) => {
          console.error('WebSocket 오류:', event);
          handleConnectionError();
        },
        onDisconnect: () => {
          console.log('WebSocket 연결 해제');
          setConnected(false);
        }
      });

      client.activate();
      stompClientRef.current = client;
    } catch (error) {
      console.error('WebSocket 연결 초기화 실패:', error);
      handleConnectionError();
    }
  };

  const handleConnectionError = () => {
    console.log('WebSocket 연결 오류 처리');
    setConnecting(false);
    setConnected(false);
    setError('채팅 서버 연결 실패');
    
    // 자동 재연결 시도 (최대 3회)
    if (retryCount.current < 3) {
      retryCount.current++;
      console.log(`재연결 시도 ${retryCount.current}/3`);
      setTimeout(() => {
        if (user) {
          setConnecting(true);
          connect();
        }
      }, 3000 * retryCount.current); // 3초, 6초, 9초 간격
    }
  };

  // 수동 재연결 함수
  const handleRetryConnection = () => {
    if (user) {
      setError('');
      setConnecting(true);
      retryCount.current = 0;
      connect();
    }
  };

  const loadInitialMessages = async () => {
    try {
      const res = await axios.get('/public/chat', {
        params: { roomId: 'public', limit: 20 },
      });
      
      const history = res.data?.data || res.data || [];
      
      if (Array.isArray(history) && history.length > 0) {
        const messagesWithBlindStatus = history.map(msg => ({
          ...msg,
          isBlinded: msg.isBlinded || false
        }));
        setMessages(messagesWithBlindStatus);
        setOldestMessageId(messagesWithBlindStatus[0]?.id || null);
        setHasMoreHistory(history.length === 20); // 20개 가득 불러왔으면 더 있을 가능성
      } else {
        setMessages([]);
        setHasMoreHistory(false);
      }
    } catch (err) {
      console.error('이전 채팅 내역 불러오기 실패:', err);
      setMessages([]);
      setHasMoreHistory(false);
    }
  };
  
  const loadMoreHistory = async () => {
    if (loadingHistory || !hasMoreHistory || !oldestMessageId) return;
    
    setLoadingHistory(true);
    try {
      const res = await axios.get('/public/chat/history', {
        params: { 
          roomId: 'public', 
          beforeId: oldestMessageId,
          limit: 20 
        },
      });
      
      const history = res.data?.data || res.data || [];
      
      if (Array.isArray(history) && history.length > 0) {
        const messagesWithBlindStatus = history.map(msg => ({
          ...msg,
          isBlinded: msg.isBlinded || false
        }));
        
        setMessages(prev => [...messagesWithBlindStatus, ...prev]);
        setOldestMessageId(messagesWithBlindStatus[0]?.id || null);
        setHasMoreHistory(history.length === 20);
      } else {
        setHasMoreHistory(false);
      }
    } catch (err) {
      console.error('이전 메시지 로드 실패:', err);
      setHasMoreHistory(false);
    } finally {
      setLoadingHistory(false);
    }
  };

  // ✅ 새 메시지가 추가될 때만 스크롤 (블라인드 처리 시에는 스크롤 안함)
  const prevMessagesLength = useRef(messages.length);
  useEffect(() => {
    const container = scrollContainerRef.current;
    if (container && messages.length > prevMessagesLength.current) {
      container.scrollTop = container.scrollHeight;
    }
    prevMessagesLength.current = messages.length;
  }, [messages]);

  // 채팅 금지 타이머
  useEffect(() => {
    let interval: NodeJS.Timeout;
    
    if (banStatus?.banned && banStatus.bannedUntil) {
      interval = setInterval(() => {
        const now = new Date().getTime();
        const endTime = new Date(banStatus.bannedUntil!).getTime();
        const remaining = Math.max(0, Math.floor((endTime - now) / 1000));
        
        setRemainingTime(remaining);
        
        if (remaining <= 0) {
          setBanStatus(null);
          setError('');
          clearInterval(interval);
        }
      }, 1000);
    } else if (banStatus?.banned && banStatus.duration) {
      // 백엔드에서 받은 남은 시간(분)을 초로 변환
      const initialSeconds = banStatus.duration * 60;
      setRemainingTime(initialSeconds);
      
      interval = setInterval(() => {
        setRemainingTime(prev => {
          const newTime = Math.max(0, prev - 1);
          if (newTime <= 0) {
            setBanStatus(null);
            setError('');
            clearInterval(interval);
          }
          return newTime;
        });
      }, 1000);
    }
    
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [banStatus]);

  // 시간 포맷 함수
  const formatTime = (seconds: number) => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  // 사용자 채팅 금지 상태 확인
  const checkBanStatus = async () => {
    if (!user) return;
    
    try {
      const response = await axios.get('/admin/chat/ban-status');
      const banData = response.data;
      
      console.log('채팅 금지 상태 확인:', banData); // 디버깅용
      
      if (banData.banned) {
        // 금지 종료 시간 설정
        setBanStatus({
          banned: true,
          reason: banData.reason,
          duration: banData.remainingMinutes,
          bannedUntil: banData.bannedUntil
        });
        setError(`채팅이 제한되었습니다.`);
      } else {
        setBanStatus(null);
        setError('');
      }
    } catch (error) {
      console.error('채팅 금지 상태 확인 실패:', error);
      // 에러가 발생해도 금지 상태는 초기화
      setBanStatus(null);
    }
  };

  useEffect(() => {
    // 채팅 내역은 모두 로드
    loadInitialMessages();

    // 모든 사용자에게 WebSocket 연결 시도 (비로그인 사용자도 채팅 읽기 가능)
    setConnecting(true);
    connect();
    
    // 로그인한 사용자만 채팅 금지 상태 확인
    if (user) {
      checkBanStatus();
      // 주기적으로 금지 상태 확인 (30초마다)
      const statusInterval = setInterval(checkBanStatus, 30000);
      return () => {
        clearInterval(statusInterval);
        if (stompClientRef.current) {
          stompClientRef.current.deactivate();
        }
      };
    } else {
      setBanStatus(null);
    }

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
      }
    };

    // cleanup은 위에서 처리됨
  }, [user]); // user 상태 변경 시 재실행

  // 관리자 권한 확인
  const isAdmin = user?.role === 'ADMIN' || user?.isAdmin;

  // 메시지 블라인드 처리
  const handleMessageBlind = (messageId: string) => {
    setMessages(prev =>
      prev.map(msg =>
        msg.id === messageId ? { ...msg, isBlinded: true } : msg
      )
    );
  };

  // 메시지 블라인드 해제 (실시간 업데이트는 WebSocket에서 처리)
  const handleMessageUnblind = (messageId: string) => {
    // WebSocket을 통해 실시간으로 업데이트되므로 여기서는 아무것도 하지 않음
  };

  // 사용자 제재 처리
  const handleUserBan = (username: string) => {
    setBannedUsers(prev => new Set([...prev, username]));
    // 메시지는 유지하고 제재 상태만 기록
  };

  const sendMessage = () => {
    if (!input.trim() || !user) return;

    // 제재된 사용자는 메시지 전송 불가
    if (banStatus?.banned || bannedUsers.has(user.nickname)) {
      const reason = banStatus?.reason || '채팅이 제한되었습니다.';
      setError(reason + ' 관리자에게 문의하세요.');
      return;
    }

    const client = stompClientRef.current;
    if (!client || !client.connected) {
      setError('채팅 서버에 연결되어 있지 않습니다.');
      return;
    }

    client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({
        roomId: 'public',
        sender: user.nickname,
        message: input,
      }),
    });

    setInput('');
  };

  return (
    <div className="space-y-4">
      {/* 관리자 대시보드 모달 */}
      <AdminChatModal 
        isOpen={showAdminDashboard} 
        onClose={() => setShowAdminDashboard(false)} 
      />

      <div className="bg-[#1f2336]/70 backdrop-blur-md p-4 rounded-xl h-[600px] flex flex-col">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-lg font-semibold text-purple-300">💬 실시간 채팅</h3>
          {isAdmin && (
            <button
              onClick={() => setShowAdminDashboard(!showAdminDashboard)}
              className="text-sm bg-purple-600 hover:bg-purple-700 text-white px-3 py-1 rounded transition-colors"
              title="관리자 대시보드"
            >
              🛡️ 관리
            </button>
          )}
        </div>

      <div className="flex-1 overflow-hidden">
        <div
          ref={scrollContainerRef}
          className="h-full overflow-y-auto pr-1 space-y-2 mb-3 scrollbar-thin scrollbar-thumb-purple-700 scrollbar-track-transparent"
          onScroll={(e) => {
            const { scrollTop } = e.currentTarget;
            // 위로 스크롤할 때 이전 메시지 로드
            if (scrollTop === 0 && hasMoreHistory && !loadingHistory) {
              loadMoreHistory();
            }
          }}
        >
          {loadingHistory && (
            <div className="text-center py-2">
              <div className="text-purple-400 text-sm">이전 메시지 로드 중...</div>
            </div>
          )}
          {messages.length === 0 ? (
            <div className="flex items-center justify-center h-full text-gray-400 text-sm">
              <div className="text-center">
                <div className="text-2xl mb-2">🌌</div>
                <p>아직 채팅 내역이 없습니다.</p>
                {user ? (
                  <p>첫 번째 메시지를 보내보세요!</p>
                ) : (
                  <p>로그인 후 채팅에 참여하세요!</p>
                )}
              </div>
            </div>
          ) : (
            messages.map((msg, idx) => {
              const isMe = msg.sender === user?.nickname;
              const timestamp = new Date(msg.timestamp).toLocaleTimeString('ko-KR', {
                hour: '2-digit',
                minute: '2-digit'
              });

              return (
                <div key={idx} className={`flex ${isMe ? 'justify-end' : 'justify-start'} group`}>
                  <div
                    className={`max-w-[70%] px-3 py-2 rounded text-sm relative ${
                      isMe
                        ? 'bg-purple-600 text-white rounded-br-none'
                        : 'bg-black/40 text-white rounded-bl-none'
                    } ${
                      msg.isBlinded ? 'opacity-50 bg-gray-600' : ''
                    }`}
                  >
                    {!isMe && (
                      <div className="flex items-center justify-between mb-1">
                        <div className="font-bold text-purple-300">{msg.sender}</div>
                        {isAdmin && !isMe && (
                          <AdminChatControls
                            messageId={msg.id || `${idx}`}
                            sender={msg.sender}
                            isBlinded={msg.isBlinded}
                            onMessageBlind={handleMessageBlind}
                            onMessageUnblind={handleMessageUnblind}
                            onUserBan={handleUserBan}
                          />
                        )}
                      </div>
                    )}
                    <div>
                      {msg.isBlinded ? (
                        <span className="text-gray-400 italic">
                          🙈 이 메시지는 관리자에 의해 블라인드 처리되었습니다.
                        </span>
                      ) : (
                        msg.message
                      )}
                    </div>
                    <div className={`text-xs mt-1 opacity-70 ${
                      isMe ? 'text-purple-200' : 'text-gray-400'
                    }`}>
                      {timestamp}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* 연결 상태 표시 */}
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2 text-sm">
          {connecting && (
            <>
              <div className="w-2 h-2 bg-yellow-300 rounded-full animate-pulse"></div>
              <span className="text-yellow-300">채팅 연결 중...</span>
            </>
          )}
          {connected && !connecting && !error && (
            <>
              <div className="w-2 h-2 bg-green-400 rounded-full"></div>
              <span className="text-green-400">채팅 연결 완료</span>
            </>
          )}
          {error && !connecting && (
            <>
              <div className="w-2 h-2 bg-red-400 rounded-full"></div>
              <span className="text-red-400">{error}</span>
            </>
          )}
        </div>
        {error && !connecting && (
          <button
            onClick={handleRetryConnection}
            className="text-xs bg-gray-600 hover:bg-gray-500 text-white px-2 py-1 rounded transition-colors"
            title="연결 재시도"
          >
            🔄
          </button>
        )}
      </div>

      {/* 채팅 금지 상태 표시 */}
      {(banStatus?.banned || bannedUsers.has(user?.nickname || '')) && (
        <div className="mb-2 p-3 bg-red-900/50 border border-red-500 rounded-lg animate-pulse">
          <div className="text-red-300 text-sm font-semibold flex items-center justify-between">
            <span>🚫 채팅이 제한되었습니다</span>
            {remainingTime > 0 && (
              <span className="text-orange-300 font-mono text-lg">
                {formatTime(remainingTime)}
              </span>
            )}
          </div>
          <div className="text-red-200 text-xs mt-1">
            사유: {banStatus?.reason || '관리자에 의한 제재'}
          </div>
          {remainingTime > 0 && (
            <div className="text-red-200 text-xs mt-1">
              남은 시간: {Math.floor(remainingTime / 60)}분 {remainingTime % 60}초
            </div>
          )}
        </div>
      )}

      {user ? (
        <div className="flex">
          <input
            type="text"
            className={`flex-1 px-3 py-2 rounded-l text-white placeholder-gray-400 ${
              banStatus?.banned || bannedUsers.has(user?.nickname || '')
                ? 'bg-red-900/30 border border-red-500 cursor-not-allowed' 
                : 'bg-black/30'
            }`}
            placeholder={(banStatus?.banned || bannedUsers.has(user?.nickname || '')) ? `채팅정지당한 상태입니다` : '별빛처럼 속삭이세요...'}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
            disabled={connecting || !!error || banStatus?.banned || bannedUsers.has(user?.nickname || '')}
          />
          <button
            onClick={sendMessage}
            className={`px-4 rounded-r transition ${
              banStatus?.banned || bannedUsers.has(user?.nickname || '')
                ? 'bg-red-600 cursor-not-allowed'
                : 'bg-purple-600 hover:bg-purple-700'
            } text-white`}
            disabled={connecting || !!error || banStatus?.banned || bannedUsers.has(user?.nickname || '')}
          >
            {(banStatus?.banned || bannedUsers.has(user?.nickname || '')) ? '금지됨' : '전송'}
          </button>
        </div>
      ) : (
        <input
          type="text"
          className="w-full px-3 py-2 rounded bg-black/30 text-gray-400 cursor-not-allowed"
          placeholder="메시지 전송은 로그인 후 가능합니다"
          disabled
        />
      )}
      </div>
    </div>
  );
}
