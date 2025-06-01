import React, { useEffect, useRef, useState } from 'react';
import { createStompClient } from '../lib/socket';

type ChatMessage = {
  roomId: string;
  sender: string;
  message: string;
  timestamp?: string;
};

const ChatPage: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const clientRef = useRef<any>(null);

  // ✅ 여기에 실제 발급된 JWT Access Token 넣기
  const token = 'eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwicm9sZSI6IlVTRVIiLCJleHAiOjE3NDg3NjYyMjZ9.RV7gRUVvxdUkWwZBTynABWu3ozIZHo1qt1_gSecPc-U';

  useEffect(() => {
    const client = createStompClient(token);

    client.onConnect = () => {
      console.log('🔌 Connected to WebSocket');

      client.subscribe('/topic/public', (message) => {
        const msg: ChatMessage = JSON.parse(message.body);
        setMessages((prev) => [...prev, msg]);
      });
    };

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [token]);

  const handleSend = () => {
    if (!input.trim()) return;

    if (!clientRef.current || !clientRef.current.connected) {
      console.warn('❌ 아직 WebSocket 연결이 완료되지 않았습니다.');
      return;
    }

    const msg: ChatMessage = {
      roomId: 'main', // ✅ 서버 기본 채팅방 이름
      sender: '',     // 서버에서 principal로 자동 지정
      message: input,
    };

    clientRef.current.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(msg),
    });

    setInput('');
  };

  return (
    <div style={{ padding: '1rem' }}>
      <h2>🌌 실시간 공용 채팅방</h2>
      <div
        style={{
          border: '1px solid #ccc',
          padding: '1rem',
          height: '300px',
          overflowY: 'scroll',
          marginBottom: '1rem',
        }}
      >
        {messages.map((m, i) => (
          <div key={i}>
            <strong>{m.sender}</strong>: {m.message}
          </div>
        ))}
      </div>
      <input
        style={{ width: '70%', marginRight: '1rem' }}
        value={input}
        onChange={(e) => setInput(e.target.value)}
        placeholder="메시지를 입력하세요"
      />
      <button onClick={handleSend}>전송</button>
    </div>
  );
};

export default ChatPage;
