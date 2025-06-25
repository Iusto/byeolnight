import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import SockJS from 'sockjs-client';
import { Client, over } from 'stompjs';
import clsx from 'clsx';

let stompClient: Client | null = null;

interface ChatMessage {
  sender: string;
  content: string;
  timestamp: string;
}

export default function ChatRoom() {
  const { user } = useAuth();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const chatEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const socket = new SockJS('/ws/chat');
    stompClient = over(socket);
    stompClient.connect({}, () => {
      stompClient?.subscribe('/topic/public', (msg) => {
        const payload: ChatMessage = JSON.parse(msg.body);
        setMessages((prev) => [...prev, payload]);
      });
    });

    return () => {
      stompClient?.disconnect(() => {});
    };
  }, []);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = () => {
    if (stompClient && input.trim() && user) {
      stompClient.send(
        '/app/chat.send',
        {},
        JSON.stringify({ sender: user.nickname, content: input })
      );
      setInput('');
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') sendMessage();
  };

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white">
      <h2 className="text-2xl font-bold text-center mt-6 mb-4 drop-shadow-glow">💬 우주 커뮤니티 실시간 채팅방</h2>
      <div className="flex-1 overflow-y-auto px-6 pb-36">
        <ul className="space-y-2">
          {messages.map((msg, idx) => (
            <li
              key={idx}
              className={clsx(
                'max-w-[70%] p-3 rounded-lg shadow',
                msg.sender === user?.nickname
                  ? 'ml-auto bg-purple-600 text-white'
                  : 'mr-auto bg-[#2a2e45] text-gray-200'
              )}
            >
              <div className="text-sm font-semibold mb-1">{msg.sender}</div>
              <div className="text-sm">{msg.content}</div>
            </li>
          ))}
          <div ref={chatEndRef} />
        </ul>
      </div>

      <div className="fixed bottom-0 left-0 right-0 px-4 py-3 bg-[#1f2336]/90 backdrop-blur-md flex items-center gap-2">
        {user ? (
          <>
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="메시지를 입력하세요"
              className="flex-1 p-2 rounded-lg bg-[#2e334f] text-white outline-none"
            />
            <button
              onClick={sendMessage}
              className="bg-purple-500 hover:bg-purple-600 px-4 py-2 rounded-lg text-sm font-semibold"
            >
              전송
            </button>
          </>
        ) : (
          <p className="text-gray-400 text-sm">로그인 시 채팅 참여가 가능합니다.</p>
        )}
      </div>
    </div>
  );
}
