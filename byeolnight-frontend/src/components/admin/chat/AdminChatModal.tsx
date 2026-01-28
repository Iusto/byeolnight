import React from 'react';
import { createPortal } from 'react-dom';
import AdminChatTable from './AdminChatTable';

interface AdminChatModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const AdminChatModal: React.FC<AdminChatModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  const modalContent = (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4">
      {/* 배경 오버레이 */}
      <div 
        className="absolute inset-0 bg-black/70 backdrop-blur-sm"
        onClick={onClose}
      />
      
      {/* 모달 컨텐츠 */}
      <div className="relative bg-[#1f2336] border border-gray-600 rounded-xl shadow-2xl w-full max-w-6xl h-[90vh] overflow-hidden">
        {/* 헤더 */}
        <div className="flex items-center justify-between p-6 border-b border-gray-600">
          <h2 className="text-2xl font-bold text-purple-300">🛡️ 채팅 관리 대시보드</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-white text-2xl transition-colors"
          >
            ✕
          </button>
        </div>

        {/* 컨텐츠 */}
        <div className="h-full overflow-auto p-6">
          <AdminChatTable />
        </div>
      </div>
    </div>
  );

  return createPortal(modalContent, document.body);
};

export default AdminChatModal;