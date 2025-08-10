import React, { useState } from 'react';
import { MessageDto } from '../../types/message';
import { messageApi } from '../../lib/api/message';

interface MessageFormProps {
  receiverId?: number;
  onSuccess?: () => void;
  onCancel?: () => void;
}

const MessageForm: React.FC<MessageFormProps> = ({ receiverId, onSuccess, onCancel }) => {
  const [formData, setFormData] = useState<MessageDto.Request>({
    receiverId: receiverId || 0,
    title: '',
    content: ''
  });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.receiverId || !formData.title.trim() || !formData.content.trim()) {
      alert('모든 필드를 입력해주세요');
      return;
    }

    try {
      setLoading(true);
      await messageApi.sendMessage(formData);
      alert('쪽지가 전송되었습니다');
      setFormData({ receiverId: receiverId || 0, title: '', content: '' });
      onSuccess?.();
    } catch (error) {
      console.error('쪽지 전송 실패:', error);
      alert('쪽지 전송에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {!receiverId && (
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            받는 사람 ID
          </label>
          <input
            type="number"
            value={formData.receiverId || ''}
            onChange={(e) => setFormData({ ...formData, receiverId: Number(e.target.value) })}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            required
          />
        </div>
      )}
      
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          제목
        </label>
        <input
          type="text"
          value={formData.title}
          onChange={(e) => setFormData({ ...formData, title: e.target.value })}
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          maxLength={100}
          required
        />
      </div>
      
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          내용
        </label>
        <textarea
          value={formData.content}
          onChange={(e) => setFormData({ ...formData, content: e.target.value })}
          rows={6}
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          required
        />
      </div>
      
      <div className="flex gap-2 justify-end">
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2 text-gray-600 border border-gray-300 rounded-md hover:bg-gray-50"
          >
            취소
          </button>
        )}
        <button
          type="submit"
          disabled={loading}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? '전송 중...' : '쪽지 보내기'}
        </button>
      </div>
    </form>
  );
};

export default MessageForm;