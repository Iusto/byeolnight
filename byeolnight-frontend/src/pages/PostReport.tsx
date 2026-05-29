import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import { getErrorMessage, isAxiosError } from '../types/api';

const reportReasons = [
  { value: 'SPAM', label: '스팸/광고' },
  { value: 'INAPPROPRIATE', label: '부적절한 내용' },
  { value: 'HARASSMENT', label: '괴롭힘/욕설' },
  { value: 'COPYRIGHT', label: '저작권 침해' },
  { value: 'MISINFORMATION', label: '허위정보' },
  { value: 'OTHER', label: '기타' }
];

export default function PostReport() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  const [selectedReason, setSelectedReason] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);

  if (!user) {
    return (
      <div className="min-h-screen bg-space-gradient text-white py-12 px-6">
        <div className="max-w-2xl mx-auto bg-[#1f2336]/80 backdrop-blur-md p-8 rounded-xl shadow-xl">
          <div className="text-center">
            <h1 className="text-2xl font-bold mb-4">로그인이 필요합니다</h1>
            <button 
              onClick={() => navigate('/login')}
              className="px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded transition"
            >
              로그인하기
            </button>
          </div>
        </div>
      </div>
    );
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedReason) {
      alert('신고 사유를 선택해주세요.');
      return;
    }

    setLoading(true);
    try {
      await axios.post(`/member/posts/${id}/report`, {
        reason: selectedReason,
        description: description.trim() || null
      });
      
      alert('신고가 접수되었습니다. 검토 후 조치하겠습니다.');
      navigate(`/posts/${id}`);
    } catch (err: unknown) {
      console.error('신고 실패:', err);

      // 409 에러 (중복 신고) 처리
      if (isAxiosError(err) && err.response?.status === 409) {
        alert('이미 신고한 게시글입니다.');
        navigate(`/posts/${id}`);
        return;
      }

      // 500 에러에서도 중복 신고 메시지 체크 (하위 호환성)
      const errorMessage = getErrorMessage(err);
      if (isAxiosError(err) && err.response?.status === 500 && errorMessage.includes('이미 신고한')) {
        alert('이미 신고한 게시글입니다.');
        navigate(`/posts/${id}`);
        return;
      }

      alert(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-space-gradient text-white py-12 px-6">
      <div className="max-w-2xl mx-auto bg-[#1f2336]/80 backdrop-blur-md p-8 rounded-xl shadow-xl">
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate(`/posts/${id}`)}
            className="text-gray-400 hover:text-white transition"
          >
            ← 돌아가기
          </button>
          <h1 className="text-2xl font-bold">🚨 게시글 신고</h1>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium mb-3">신고 사유</label>
            <div className="space-y-2">
              {reportReasons.map((reason) => (
                <label key={reason.value} className="flex items-center gap-3 p-3 bg-[#2a2e45] rounded-lg hover:bg-[#323654] transition cursor-pointer">
                  <input
                    type="radio"
                    name="reason"
                    value={reason.value}
                    checked={selectedReason === reason.value}
                    onChange={(e) => setSelectedReason(e.target.value)}
                    className="text-purple-600"
                  />
                  <span>{reason.label}</span>
                </label>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium mb-2">상세 설명 (선택사항)</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={4}
              placeholder="신고 사유에 대한 자세한 설명을 입력해주세요..."
              className="w-full p-3 rounded bg-[#2a2e45] text-white focus:outline-none focus:ring-2 focus:ring-purple-500"
              maxLength={500}
            />
            <div className="text-xs text-gray-400 mt-1">
              {description.length}/500자
            </div>
          </div>

          <div className="flex gap-4">
            <button
              type="button"
              onClick={() => navigate(`/posts/${id}`)}
              className="flex-1 px-4 py-2 bg-gray-600 hover:bg-gray-700 rounded transition"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={loading || !selectedReason}
              className={`flex-1 px-4 py-2 rounded transition ${
                loading || !selectedReason
                  ? 'bg-gray-500 cursor-not-allowed'
                  : 'bg-red-600 hover:bg-red-700'
              }`}
            >
              {loading ? '신고 중...' : '신고하기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}