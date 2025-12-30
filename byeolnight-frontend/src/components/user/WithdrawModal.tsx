import { useState } from 'react';
import { createPortal } from 'react-dom';

interface WithdrawModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (password: string, reason: string) => void;
  socialProvider?: string;
}

export default function WithdrawModal({ isOpen, onClose, onConfirm, socialProvider }: WithdrawModalProps) {
  const isSocialUser = !!socialProvider;
  const [password, setPassword] = useState('');
  const [reason, setReason] = useState('');
  const [agreed, setAgreed] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!isSocialUser && !password.trim()) {
      alert('비밀번호를 입력해주세요.');
      return;
    }
    if (!reason.trim()) {
      alert('탈퇴 사유를 입력해주세요.');
      return;
    }
    if (!agreed) {
      alert('탈퇴 안내사항에 동의해주세요.');
      return;
    }
    onConfirm(isSocialUser ? '' : password.trim(), reason.trim());
    handleClose();
  };

  const handleClose = () => {
    setPassword('');
    setReason('');
    setAgreed(false);
    onClose();
  };

  const modalContent = (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center p-2 sm:p-4">
      <div 
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={handleClose}
      />
      
      <div className="relative bg-gray-800 border border-gray-600 rounded-xl shadow-2xl p-4 sm:p-6 w-full max-w-[95vw] sm:max-w-[600px] max-h-[95vh] sm:max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-white">⚠️ 회원 탈퇴</h3>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-white text-xl"
          >
            ✕
          </button>
        </div>

        <div className="mb-6 p-4 bg-red-900/20 border border-red-500/30 rounded-lg">
          <h4 className="text-red-400 font-semibold mb-2">탈퇴 안내사항</h4>
          <ul className="text-sm text-gray-300 space-y-1">
            {isSocialUser ? (
              <>
                <li>• 소셜 로그인 연동 해제 시 즉시 탈퇴 처리됩니다.</li>
                <li>• <span className="text-yellow-400">탈퇴 후 30일 내 재로그인으로 복구 가능</span>합니다.</li>
                <li>• 30일 경과 후 개인정보 마스킹 처리되어 복구 불가능합니다.</li>
                <li>• <span className="text-red-400">2년 경과 후 계정 완전 삭제</span>됩니다.</li>
              </>
            ) : (
              <>
                <li>• 탈퇴 시 모든 개인정보가 삭제되며 복구할 수 없습니다.</li>
                <li>• <span className="text-red-400">2년 경과 후 계정 완전 삭제</span>됩니다.</li>
              </>
            )}
            <li>• 작성한 게시글과 댓글은 삭제되지 않습니다.</li>
            <li>• 보유하신 포인트와 인증서는 모두 소멸됩니다.</li>
            <li>• 동일한 이메일로 재가입이 가능합니다.</li>
          </ul>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {!isSocialUser && (
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                현재 비밀번호 확인
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="현재 비밀번호를 입력하세요"
                className="w-full p-3 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-red-500"
                required
              />
            </div>
          )}
          
          {isSocialUser && (
            <div className="p-3 bg-blue-900/20 border border-blue-500/30 rounded-lg">
              <p className="text-sm text-blue-300">
                🔗 소셜 로그인 사용자는 비밀번호 입력 없이 탈퇴가 가능합니다.
              </p>
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              탈퇴 사유 *
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="탈퇴 사유를 입력해주세요..."
              rows={4}
              className="w-full p-3 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-red-500"
              maxLength={200}
              required
            />
            <div className="text-xs text-gray-400 mt-1">
              {reason.length}/200자
            </div>
          </div>

          <div className="flex items-start gap-3">
            <input
              type="checkbox"
              id="agree"
              checked={agreed}
              onChange={(e) => setAgreed(e.target.checked)}
              className="mt-1"
            />
            <label htmlFor="agree" className="text-sm text-gray-300">
              위 안내사항을 모두 확인했으며, 회원 탈퇴에 동의합니다.
            </label>
          </div>

          <div className="flex flex-col sm:flex-row gap-3 justify-end pt-4">
            <button
              type="button"
              onClick={handleClose}
              className="px-4 py-3 bg-gray-600 hover:bg-gray-700 text-white rounded-lg transition order-2 sm:order-1"
            >
              취소
            </button>
            <button
              type="submit"
              className="px-4 py-3 bg-red-600 hover:bg-red-700 text-white rounded-lg transition order-1 sm:order-2"
            >
              탈퇴하기
            </button>
          </div>
        </form>
      </div>
    </div>
  );

  return createPortal(modalContent, document.body);
}