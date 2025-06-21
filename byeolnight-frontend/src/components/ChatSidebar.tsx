import { useEffect, useState, useRef } from 'react'
import { useAuth } from '../contexts/AuthContext'
import SockJS from 'sockjs-client'
import { Client, Stomp } from '@stomp/stompjs'
import axios from '../lib/axios'

interface Message {
  sender: string
  content: string
}

export default function ChatSidebar() {
  const { user } = useAuth()
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const stompClientRef = useRef<Client | null>(null)

  useEffect(() => {
    if (!user) return

    axios.get('/api/chat/public')
      .then(res => setMessages(res.data))
      .catch(err => console.error('💬 채팅 메시지 불러오기 실패', err))

    const socket = new SockJS(`${import.meta.env.VITE_API_BASE_URL}/ws/chat`)
    const stomp = Stomp.over(socket)
    stomp.connect({}, () => {
      stomp.subscribe('/topic/public', message => {
        const msg = JSON.parse(message.body)
        setMessages(prev => [...prev, { sender: msg.sender, content: msg.message }])
      })
    })

    stompClientRef.current = stomp

    return () => {
      stomp.disconnect()
    }
  }, [user])

  const handleSend = () => {
    if (!user || !input.trim() || !stompClientRef.current?.connected) return

    const payload = {
      roomId: 'public',
      sender: user.nickname,
      message: input.trim(),
    }

    stompClientRef.current.send('/app/chat.send', {}, JSON.stringify(payload))
    setInput('')
  }

  return (
    <div className="h-full flex flex-col justify-between">
      <div className="overflow-y-auto h-[400px] space-y-2 mb-2 p-2 bg-[#1e293b] rounded">
        {messages.map((msg, idx) => (
          <div key={idx} className="text-sm">
            <span className="font-bold text-blue-400">{msg.sender}: </span>
            <span>{msg.content}</span>
          </div>
        ))}
      </div>

      {user ? (
        <div className="flex">
          <input
            type="text"
            className="flex-1 px-2 py-1 rounded-l bg-white text-black"
            placeholder="메시지를 입력하세요"
            value={input}
            onChange={e => setInput(e.target.value)}
          />
          <button
            onClick={handleSend}
            className="px-3 bg-blue-500 text-white rounded-r hover:bg-blue-600"
          >
            전송
          </button>
        </div>
      ) : (
        <div className="text-sm text-center text-gray-400 py-2 border-t border-gray-600">
          로그인 시 채팅 이용이 가능합니다
        </div>
      )}
    </div>
  )
}
