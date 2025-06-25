import { useContext, useEffect, useRef, useState } from 'react';
import { AuthContext } from '../contexts/AuthContext';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface ChatMessage {
  sender: string;
  content: string;
  timestamp: string;
}

export default function ChatSidebar() {
  const { user } = useContext(AuthContext);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const stompClientRef = useRef<Client | null>(null);

  useEffect(() => {
    const socket = new SockJS(import.meta.env.VITE_WS_URL || '/ws');
    const client = new Client({
      webSocketFactory: () => socket,
      onConnect: () => {
        client.subscribe('/topic/public', (message) => {
          const body = JSON.parse(message.body);
          setMessages((prev) => [...prev.slice(-8), payload]);
        });
      },
    });
    client.activate();
    stompClientRef.current = client;

    return () => client.deactivate();
  }, []);

  const sendMessage = () => {
    if (!input.trim() || !user) return;
    stompClientRef.current?.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ sender: user.nickname, content: input }),
    });
    setInput('');
  };

  return (
    <div className="bg-[#1f2336]/70 backdrop-blur-md p-4 rounded-xl h-[500px] flex flex-col">
      <h3 className="text-lg font-semibold mb-2 text-purple-300">💬 실시간 채팅</h3>

      <div className="flex-1 overflow-y-auto space-y-2 mb-3 scrollbar-thin scrollbar-thumb-purple-700 scrollbar-track-transparent">
        {messages.map((msg, idx) => (
          <div key={idx} className="text-sm text-white bg-black/20 p-2 rounded">
            <span className="font-bold text-purple-400">{msg.sender}:</span> {msg.content}
          </div>
        ))}
      </div>

      {user ? (
        <div className="flex">
          <input
            type="text"
            className="flex-1 px-3 py-2 rounded-l bg-black/30 text-white placeholder-gray-400"
            placeholder="별빛처럼 속삭이세요..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
          />
          <button
            onClick={sendMessage}
            className="bg-purple-600 text-white px-4 rounded-r hover:bg-purple-700"
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
