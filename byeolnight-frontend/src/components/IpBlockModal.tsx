import { useState } from 'react';

interface IpBlockModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (ip: string, duration: number) => void;
}

export default function IpBlockModal({ isOpen, onClose, onConfirm }: IpBlockModalProps) {
  const [ip, setIp] = useState('');
  const [duration, setDuration] = useState(60);
  const [error, setError] = useState('');

  const validateIp = (ipAddress: string) => {
    const ipRegex = /^(\d{1,3}\.){3}\d{1,3}$/;
    if (!ipRegex.test(ipAddress)) return false;
    
    const parts = ipAddress.split('.');
    return parts.every(part => {
      const num = parseInt(part);
      return num >= 0 && num <= 255;
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!ip.trim()) {
      setError('IP 주소를 입력해주세요.');
      return;
    }

    if (!validateIp(ip)) {
      setError('올바른 IP 주소 형식이 아닙니다. (예: 192.168.1.1)');
      return;
    }

    if (duration < 1 || duration > 10080) {
      setError('차단 시간은 1분 이상 7일(10080분) 이하로 설정해주세요.');
      return;
    }

    onConfirm(ip, duration);
    setIp('');
    setDuration(60);
    setError('');
    onClose();
  };

  const handleClose = () => {
    setIp('');
    setDuration(60);
    setError('');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={handleClose}>
      <div 
        className="bg-[#1f2336] text-white p-6 rounded-xl max-w-md w-full mx-4 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xl font-bold">🚫 IP 차단 추가</h3>
          <button 
            onClick={handleClose}
            className="text-gray-400 hover:text-white text-2xl"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-2">IP 주소</label>
            <input
              type="text"
              value={ip}
              onChange={(e) => setIp(e.target.value)}
              placeholder="예: 192.168.1.1"
              className="w-full px-3 py-2 bg-[#2a2e45] text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-2">차단 시간 (분)</label>
            <select
              value={duration}
              onChange={(e) => setDuration(parseInt(e.target.value))}
              className="w-full px-3 py-2 bg-[#2a2e45] text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            >
              <option value={60}>1시간 (60분)</option>
              <option value={360}>6시간 (360분)</option>
              <option value={720}>12시간 (720분)</option>
              <option value={1440}>1일 (1440분)</option>
              <option value={4320}>3일 (4320분)</option>
              <option value={10080}>7일 (10080분)</option>
            </select>
          </div>

          {error && (
            <div className="text-red-400 text-sm bg-red-900/20 p-2 rounded">
              {error}
            </div>
          )}

          <div className="flex gap-3">
            <button
              type="button"
              onClick={handleClose}
              className="flex-1 bg-gray-600 hover:bg-gray-700 text-white py-2 rounded-lg transition-colors"
            >
              취소
            </button>
            <button
              type="submit"
              className="flex-1 bg-red-600 hover:bg-red-700 text-white py-2 rounded-lg transition-colors font-medium"
            >
              차단 추가
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}