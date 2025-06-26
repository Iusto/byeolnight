import { useContext, useEffect, useRef, useState } from 'react';
import { AuthContext } from '../contexts/AuthContext';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from '../lib/axios';

interface ChatMessage {
  sender: string;
  message: string;
  timestamp: string;
}

export default function ChatSidebar() {
  const { user } = useContext(AuthContext);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [error, setError] = useState('');
  const [connecting, setConnecting] = useState(true);
  const retryCount = useRef(0);
  const stompClientRef = useRef<Client | null>(null);
  const scrollContainerRef = useRef<HTMLDivElement | null>(null); // ✅ 채팅창 컨테이너 ref

  const connect = () => {
    const socket = new SockJS(import.meta.env.VITE_WS_URL || '/ws');
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${localStorage.getItem('accessToken') || ''}`,
      },
      onConnect: () => {
        setConnecting(false);
        setError('');
        retryCount.current = 0;

        client.subscribe('/topic/public', (message) => {
          const payload = JSON.parse(message.body);
          setMessages((prev) => [...prev.slice(-10), payload]); // 최신 10개 유지
        });
      },
      onStompError: () => handleConnectionError(),
      onWebSocketError: () => handleConnectionError(),
    });

    client.activate();
    stompClientRef.current = client;
  };

  const handleConnectionError = () => {
    setConnecting(true);
    retryCount.current += 1;

    if (retryCount.current <= 5) {
      setTimeout(() => {
        connect();
      }, 5000);
    } else {
      setError('채팅 서버에 연결할 수 없습니다.');
      setConnecting(false);
    }
  };

  const loadInitialMessages = async () => {
    try {
      const res = await axios.get('/public/chat', {
        params: { roomId: 'public' },
      });
      const history = res.data || [];
      setMessages(history.slice(-10));
    } catch (err) {
      console.error('초기 채팅 내역 불러오기 실패', err);
    }
  };

  // ✅ messages가 변경될 때 채팅창 하단으로 스크롤
  useEffect(() => {
    const container = scrollContainerRef.current;
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  }, [messages]);

  useEffect(() => {
    loadInitialMessages();
    connect();
    return () => stompClientRef.current?.deactivate();
  }, []);

  const sendMessage = () => {
    if (!input.trim() || !user) return;

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
    <div className="bg-[#1f2336]/70 backdrop-blur-md p-4 rounded-xl h-[600px] flex flex-col">
      <h3 className="text-lg font-semibold mb-2 text-purple-300">💬 실시간 채팅</h3>

      <div className="flex-1 overflow-hidden">
        <div
          ref={scrollContainerRef}
          className="h-full overflow-y-auto pr-1 space-y-2 mb-3 scrollbar-thin scrollbar-thumb-purple-700 scrollbar-track-transparent"
        >
          {messages.map((msg, idx) => {
            const isMe = msg.sender === user?.nickname;
            return (
              <div key={idx} className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={`max-w-[70%] px-3 py-2 rounded text-sm ${
                    isMe
                      ? 'bg-purple-600 text-white rounded-br-none'
                      : 'bg-black/40 text-white rounded-bl-none'
                  }`}
                >
                  {!isMe && (
                    <div className="font-bold text-purple-300 mb-1">{msg.sender}</div>
                  )}
                  {msg.message}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {error && <div className="text-sm text-red-400 mb-2">⚠️ {error}</div>}
      {connecting && !error && (
        <div className="text-sm text-yellow-300 mb-2">🔄 채팅 서버에 연결 중...</div>
      )}

      {user ? (
        <div className="flex">
          <input
            type="text"
            className="flex-1 px-3 py-2 rounded-l bg-black/30 text-white placeholder-gray-400"
            placeholder="별빛처럼 속삭이세요..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
            disabled={connecting || !!error}
          />
          <button
            onClick={sendMessage}
            className="bg-purple-600 text-white px-4 rounded-r hover:bg-purple-700"
            disabled={connecting || !!error}
          >
            전송
          </button>
        </div>
      ) : (
        <input
          type="text"
          className="w-full px-3 py-2 rounded bg-black/30 text-gray-400 cursor-not-allowed"
          placeholder="로그인 시 사용 가능합니다"
          disabled
        />
      )}
    </div>
  );
}
