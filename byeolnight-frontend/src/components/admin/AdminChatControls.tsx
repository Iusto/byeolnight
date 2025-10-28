import React, { useState } from 'react';
import axios from '../lib/axios';
import AdminActionModal from './AdminActionModal';

interface AdminChatControlsProps {
  messageId: string;
  sender: string;
  isBlinded?: boolean;
  onMessageBlind: (messageId: string) => void;
  onMessageUnblind: (messageId: string) => void;
  onUserBan: (username: string) => void;
}

const AdminChatControls: React.FC<AdminChatControlsProps> = ({
  messageId,
  sender,
  isBlinded = false,
  onMessageBlind,
  onMessageUnblind,
  onUserBan
}) => {
  const [showModal, setShowModal] = useState(false);

  // 메시지 블라인드 처리
  const handleBlindMessage = async (messageId: string) => {
    try {
      await axios.post(`/admin/chat/blind/${messageId}`);
      onMessageBlind(messageId);
      alert('메시지가 블라인드 처리되었습니다.');
      console.log(`메시지 ${messageId} 블라인드 처리됨`);
    } catch (error) {
      console.error('메시지 블라인드 처리 실패:', error);
      alert('메시지 블라인드 처리에 실패했습니다.');
    }
  };

  // 메시지 블라인드 해제
  const handleUnblindMessage = async (messageId: string) => {
    try {
      await axios.delete(`/admin/chat/blind/${messageId}`);
      onMessageUnblind(messageId);
      alert('메시지 블라인드가 해제되었습니다.');
      console.log(`메시지 ${messageId} 블라인드 해제됨`);
    } catch (error) {
      console.error('메시지 블라인드 해제 실패:', error);
      alert('메시지 블라인드 해제에 실패했습니다.');
    }
  };

  // 사용자 채팅 금지
  const handleBanUser = async (username: string, duration: number, reason?: string) => {
    try {
      await axios.post('/admin/chat/ban', {
        username: username,
        duration: duration,
        reason: reason || `${duration}분간 채팅 금지`
      });
      
      onUserBan(username);
      console.log(`사용자 ${username} ${duration}분간 채팅 금지됨 - 사유: ${reason}`);
    } catch (error) {
      console.error('사용자 제재 실패:', error);
      alert('사용자 제재에 실패했습니다.');
    }
  };

  return (
    <>
      <button
        onClick={() => setShowModal(true)}
        className="text-red-400 hover:text-red-300 text-xs opacity-70 hover:opacity-100 transition-all"
        title="관리자 제재"
      >
        ⚠️
      </button>

      <AdminActionModal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        messageId={messageId}
        sender={sender}
        isBlinded={isBlinded}
        onMessageBlind={handleBlindMessage}
        onMessageUnblind={handleUnblindMessage}
        onUserBan={handleBanUser}
      />
    </>
  );
};

export default AdminChatControls;