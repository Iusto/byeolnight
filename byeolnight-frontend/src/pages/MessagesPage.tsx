import React, { useState } from 'react';
import MessageList from '../components/message/MessageList';
import MessageForm from '../components/message/MessageForm';

const MessagesPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'received' | 'sent' | 'compose'>('received');

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="bg-white rounded-lg shadow">
        {/* 탭 헤더 */}
        <div className="border-b border-gray-200">
          <nav className="flex space-x-8 px-6">
            <button
              onClick={() => setActiveTab('received')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'received'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              받은 쪽지
            </button>
            <button
              onClick={() => setActiveTab('sent')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'sent'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              보낸 쪽지
            </button>
            <button
              onClick={() => setActiveTab('compose')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'compose'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              쪽지 쓰기
            </button>
          </nav>
        </div>

        {/* 탭 내용 */}
        <div className="p-6">
          {activeTab === 'received' && <MessageList type="received" />}
          {activeTab === 'sent' && <MessageList type="sent" />}
          {activeTab === 'compose' && (
            <MessageForm 
              onSuccess={() => {
                setActiveTab('sent');
              }}
            />
          )}
        </div>
      </div>
    </div>
  );
};

export default MessagesPage;