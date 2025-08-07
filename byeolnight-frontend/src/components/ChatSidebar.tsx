import { useContext, useEffect, useRef, useState } from 'react';
import { AuthContext } from '../contexts/AuthContext';
import axios from '../lib/axios';
import AdminChatControls from './AdminChatControls';
import AdminChatModal from './AdminChatModal';
import chatConnector from './ChatConnector';

interface ChatMessage {
  id?: string;
  sender: string;
  message: string;
  timestamp: string;
  isBlinded?: boolean;
}

interface BanStatus {
  banned: boolean;
  reason?: string;
  duration?: number;
  bannedUntil?: string;
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
  const [banStatus, setBanStatus] = useState<BanStatus | null>(null);
  const [remainingTime, setRemainingTime] = useState<number>(0);
  const scrollContainerRef = useRef<HTMLDivElement | null>(null);
  const statusIntervalRef = useRef<NodeJS.Timeout | null>(null);

  // WebSocket 연결 및 콜백 설정
  const initializeWebSocket = () => {
    setConnecting(true);
    
    chatConnector.connect({
      onMessage: (msg) => {
        setMessages(prev => [...prev.slice(-50), msg]);
      },
      onConnect: () => {
        setConnecting(false);
        setConnected(true);
        setError('');
      },
      onDisconnect: () => {
        setConnected(false);
      },
      onError: () => {
        setConnecting(false);
        setConnected(false);
        setError('채팅 서버 연결 실패');
      },
      onBanNotification: (banData) => {
        if (banData.banned) {
          const endTime = new Date().getTime() + (banData.duration * 60 * 1000);
          const newBanStatus: BanStatus = {
            banned: true,
            reason: banData.reason,
            duration: banData.duration,
            bannedUntil: new Date(endTime).toISOString()
          };
          setBanStatus(newBanStatus);
          setError(`채팅이 제한되었습니다: ${banData.reason}`);
        } else {
          setBanStatus(null);
          setError('');
          setRemainingTime(0);
        }
      },
      onAdminUpdate: (data) => {
        if (data.type === 'MESSAGE_UNBLINDED') {
          setMessages(prev => 
            prev.map(msg => 
              msg.id === data.messageId ? { ...msg, isBlinded: false } : msg
            )
          );
        } else if (data.type === 'MESSAGE_BLINDED') {
          setMessages(prev => 
            prev.map(msg => 
              msg.id === data.messageId ? { ...msg, isBlinded: true } : msg
            )
          );
        }
      }
    }, user?.nickname);
  };

  const handleRetryConnection = () => {
    setError('');
    chatConnector.retryConnection();
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
        setHasMoreHistory(history.length === 20);
      } else {
        setMessages([]);
        setHasMoreHistory(false);
      }
    } catch (err) {
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
      setHasMoreHistory(false);
    } finally {
      setLoadingHistory(false);
    }
  };

  const checkBanStatus = async () => {
    if (!user) {
      setBanStatus(null);
      return;
    }
    
    try {
      const response = await axios.get('/member/chat/ban-status');
      const banData = response.data?.data || response.data;
      
      if (banData && banData.banned === true) {
        const newBanStatus: BanStatus = {
          banned: true,
          reason: banData.reason || '관리자에 의한 제재',
          duration: banData.remainingMinutes || 0,
          bannedUntil: banData.bannedUntil
        };
        setBanStatus(newBanStatus);
        setError(`채팅이 제한되었습니다.`);
      } else {
        setBanStatus(null);
        setError('');
      }
    } catch (error) {
      setBanStatus(null);
    }
  };

  const sendMessage = async () => {
    if (!input.trim() || !user) return;

    if (banStatus?.banned || bannedUsers.has(user.nickname)) {
      const reason = banStatus?.reason || '채팅이 제한되었습니다.';
      setError(reason + ' 관리자에게 문의하세요.');
      return;
    }

    try {
      chatConnector.sendMessage({
        roomId: 'public',
        sender: user.nickname,
        message: input
      });
      setInput('');
      setError('');
    } catch (error) {
      setError('메시지 전송에 실패했습니다.');
    }
  };

  // 스크롤 자동 이동
  const prevMessagesLength = useRef(messages.length);
  useEffect(() => {
    const container = scrollContainerRef.current;
    if (container && messages.length > prevMessagesLength.current) {
      container.scrollTop = container.scrollHeight;
    }
    prevMessagesLength.current = messages.length;
  }, [messages]);

  // 밴 타이머
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

  const formatTime = (seconds: number) => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  const isAdmin = user?.role === 'ADMIN' || user?.isAdmin;

  const handleMessageBlind = (messageId: string) => {
    setMessages(prev =>
      prev.map(msg =>
        msg.id === messageId ? { ...msg, isBlinded: true } : msg
      )
    );
  };

  const handleMessageUnblind = (messageId: string) => {
    // WebSocket에서 처리됨
  };

  const handleUserBan = (username: string) => {
    setBannedUsers(prev => new Set([...prev, username]));
  };

  useEffect(() => {
    loadInitialMessages();
    initializeWebSocket();
    
    if (user) {
      checkBanStatus();
      statusIntervalRef.current = setInterval(checkBanStatus, 30000);
    } else {
      setBanStatus(null);
    }

    return () => {
      if (statusIntervalRef.current) {
        clearInterval(statusIntervalRef.current);
      }
      chatConnector.disconnect();
    };
  }, [user]);

  return (
    <div className="space-y-4">
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