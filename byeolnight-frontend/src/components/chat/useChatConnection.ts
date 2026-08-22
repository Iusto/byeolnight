import { useEffect, useRef, useState } from 'react';
import chatConnector from './ChatConnector';
import type { BanStatus, ChatMessage } from './chatTypes';

interface UseChatConnectionOptions {
  nickname?: string;
  connectionFailedMessage: string;
  bannedMessage: string;
  onBanStatus: (status: BanStatus | null) => void;
  onErrorMessage: (message: string) => void;
}

/** WebSocket 생명주기와 화면의 연결 상태 표시를 한곳에서 동기화한다. */
export function useChatConnection(options: UseChatConnectionOptions) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [connectionError, setConnectionError] = useState('');
  const [connecting, setConnecting] = useState(true);
  const [connected, setConnected] = useState(false);
  const optionsRef = useRef(options);
  optionsRef.current = options;

  useEffect(() => {
    const connect = async () => {
      setConnecting(true);
      await chatConnector.connect({
        onMessage: (message) => setMessages(previous => [...previous.slice(-50), message]),
        onConnect: () => {
          setConnecting(false);
          setConnected(true);
          setConnectionError('');
        },
        onDisconnect: (willReconnect) => {
          setConnected(false);
          // 자동 재연결 중에는 실패 대신 '연결 중' 상태를 유지한다.
          setConnecting(willReconnect);
        },
        onError: () => {
          setConnecting(false);
          setConnected(false);
          setConnectionError(optionsRef.current.connectionFailedMessage);
        },
        onBanNotification: (banData) => {
          if (!banData.banned) {
            optionsRef.current.onBanStatus(null);
            optionsRef.current.onErrorMessage('');
            return;
          }
          const endTime = Date.now() + banData.duration * 60 * 1000;
          optionsRef.current.onBanStatus({
            banned: true,
            reason: banData.reason,
            duration: banData.duration,
            bannedUntil: new Date(endTime).toISOString(),
          });
          optionsRef.current.onErrorMessage(
            `${optionsRef.current.bannedMessage}: ${banData.reason}`,
          );
        },
      }, optionsRef.current.nickname);
    };

    void connect();
    return () => chatConnector.disconnect();
  }, []);

  const retryConnection = () => {
    setConnectionError('');
    setConnecting(true);
    setConnected(false);
    void chatConnector.retryConnection();
  };

  return {
    messages,
    setMessages,
    connectionError,
    connecting,
    connected,
    retryConnection,
  };
}
