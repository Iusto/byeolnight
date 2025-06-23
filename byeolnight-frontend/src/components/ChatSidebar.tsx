import { useEffect, useRef, useState } from 'react';
import { connectChat, sendMessage, stompClient } from './ChatConnector';
import { useAuth } from '../contexts/AuthContext';
import axios from 'axios';

const ChatSidebar = () => {
  const [messages, setMessages] = useState<any[]>([]);
  const [input, setInput] = useState('');
  const { user } = useAuth();
  const chatBoxRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    connectChat((msg: any) => {
      setMessages(prev => [...prev, msg]);
    });

    // ✅ 기존 메시지 요청
    setTimeout(() => {
      if (stompClient?.connected) {
        stompClient.send('/app/chat.init', {}, JSON.stringify({ roomId: 'public' }));
      }
    }, 300);
  }, []);

  useEffect(() => {
    // 초기 채팅 불러오기
    axios.get('/api/public/chat?roomId=public')
      .then(res => setMessages(res.data))
      .catch(err => console.error('🔥 초기 채팅 불러오기 실패', err));

    // WebSocket 연결
    connectChat((msg: any) => {
      setMessages(prev => [...prev, msg]);
    });
  }, []);

  const handleSend = () => {
    if (!input.trim()) return;

    const message = {
      roomId: 'public',
      message: input.trim(),
      sender: user?.nickname || '익명의 우주인',
      timestamp: new Date().toISOString()
    };
    sendMessage(message);
    setInput('');
    // setMessages() 제거 → 서버 브로드캐스트로 처리
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleSend();
    }
  };

  const formatTime = (isoString: string) => {
    const date = new Date(isoString);
    return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <aside className="w-full md:w-80 bg-gradient-to-b from-[#1a1a3d] to-[#0b0b2a] text-white p-4 flex flex-col rounded-2xl shadow-xl border border-white/10 backdrop-blur">
      <h2 className="text-xl font-bold mb-3 text-center">🌌 우주 채팅방</h2>

      <div
        ref={chatBoxRef}
        className="h-[320px] overflow-y-auto bg-black/30 border border-white/10 rounded-xl p-3 space-y-2 scroll-smooth"
      >
        {messages.map((msg, idx) => {
          const isMine = msg.sender === user?.nickname;
          return (
            <div key={idx} className={`flex flex-col ${isMine ? 'items-end' : 'items-start'}`}>
              <div
                className={`max-w-[80%] px-3 py-2 rounded-xl text-sm shadow
                ${isMine ? 'bg-purple-600 text-white' : 'bg-white/10 text-white'}`}
              >
                <span className="block font-semibold text-sm">{msg.sender}</span>
                <span className="block break-words">{msg.message}</span>
                <span className="block text-xs text-gray-300 mt-1 text-right">
                  {msg.timestamp ? formatTime(msg.timestamp) : ''}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-4 flex gap-2">
        <input
          type="text"
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="별빛처럼 속삭이세요..."
          className="flex-1 px-3 py-2 rounded-xl bg-[#2b2b4a] text-white placeholder-gray-400 border border-white/10 focus:outline-none focus:ring-2 focus:ring-purple-500"
        />
        <button
          onClick={handleSend}
          className="bg-purple-600 hover:bg-purple-700 px-4 py-2 rounded-xl font-semibold transition shadow"
        >
          전송
        </button>
      </div>
    </aside>
  );
};

export default ChatSidebar;
