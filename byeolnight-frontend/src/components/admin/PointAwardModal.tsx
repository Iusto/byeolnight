import { useState } from 'react';

interface PointAwardModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (points: number, reason: string) => void;
}

export default function PointAwardModal({ isOpen, onClose, onConfirm }: PointAwardModalProps) {
  const [points, setPoints] = useState<number>(0);
  const [reason, setReason] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (points <= 0) {
      alert('포인트는 1 이상이어야 합니다.');
      return;
    }
    
    if (!reason.trim()) {
      alert('수여 사유를 입력해주세요.');
      return;
    }
    
    onConfirm(points, reason.trim());
    
    // 폼 초기화
    setPoints(0);
    setReason('');
  };

  const handleClose = () => {
    setPoints(0);
    setReason('');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-[#1f2336]/95 backdrop-blur-md rounded-xl border border-yellow-500/20 w-full max-w-md">
        {/* 헤더 */}
        <div className="flex items-center justify-between p-6 border-b border-yellow-500/20">
          <h2 className="text-xl font-bold text-white">⭐ 포인트 수여</h2>
          <button
            onClick={handleClose}
            className="p-2 hover:bg-yellow-600/20 rounded-lg transition-colors text-gray-400 hover:text-white"
          >
            ✕
          </button>
        </div>

        {/* 내용 */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-white font-medium mb-2">수여할 포인트</label>
            <input
              type="number"
              min="1"
              max="10000"
              value={points || ''}
              onChange={(e) => setPoints(parseInt(e.target.value) || 0)}
              placeholder="수여할 포인트를 입력하세요"
              className="w-full px-4 py-3 bg-[#2a2e45]/60 border border-yellow-500/30 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-yellow-400 focus:ring-2 focus:ring-yellow-400/20 transition-all"
              required
            />
            <p className="text-sm text-gray-400 mt-1">1 ~ 10,000 포인트까지 수여 가능합니다.</p>
          </div>
          
          <div>
            <label className="block text-white font-medium mb-2">수여 사유</label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="포인트 수여 사유를 입력하세요"
              className="w-full px-4 py-3 bg-[#2a2e45]/60 border border-yellow-500/30 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-yellow-400 focus:ring-2 focus:ring-yellow-400/20 transition-all resize-none"
              rows={3}
              maxLength={200}
              required
            />
            <p className="text-sm text-gray-400 mt-1">최대 200자까지 입력 가능합니다.</p>
          </div>
          
          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={handleClose}
              className="flex-1 px-6 py-3 bg-gray-600/30 hover:bg-gray-600/50 text-gray-300 font-medium rounded-lg transition-all duration-200"
            >
              취소
            </button>
            <button
              type="submit"
              className="flex-1 px-6 py-3 bg-gradient-to-r from-yellow-600 to-orange-600 hover:from-yellow-700 hover:to-orange-700 text-white font-medium rounded-lg shadow-lg hover:shadow-xl transition-all duration-200 transform hover:scale-105"
            >
              포인트 수여
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}