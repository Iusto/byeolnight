import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { createSuggestion } from '../lib/api/suggestion';
import type { SuggestionCategory } from '../types/suggestion';

const CATEGORIES = {
  FEATURE: '기능 개선',
  BUG: '버그 신고',
  UI_UX: 'UI/UX 개선',
  CONTENT: '콘텐츠 관련',
  OTHER: '기타'
} as const;

export default function SuggestionCreate() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    content: '',
    category: 'FEATURE' as SuggestionCategory
  });

  // 로그인하지 않은 사용자는 리다이렉트
  if (!user) {
    navigate('/login');
    return null;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.title.trim() || !formData.content.trim()) {
      alert('제목과 내용을 모두 입력해주세요.');
      return;
    }

    setLoading(true);
    
    try {
      await createSuggestion(formData);
      alert('건의사항이 성공적으로 제출되었습니다!');
      navigate('/suggestions');
      
    } catch (error: any) {
      console.error('건의사항 제출 실패:', error);
      const errorMessage = error.response?.data?.message || '건의사항 제출에 실패했습니다. 다시 시도해주세요.';
      alert(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0f1419] via-[#1a1f2e] to-[#2d1b69] py-8">
      <div className="max-w-4xl mx-auto px-6">
        {/* 헤더 */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-white mb-2">✍️ 건의사항 작성</h1>
          <p className="text-gray-300">서비스 개선을 위한 소중한 의견을 들려주세요</p>
        </div>

        {/* 작성 폼 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-sm rounded-xl p-8 border border-purple-500/20">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* 카테고리 선택 */}
            <div>
              <label className="block text-white font-medium mb-3">카테고리</label>
              <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                {Object.entries(CATEGORIES).map(([key, label]) => (
                  <button
                    key={key}
                    type="button"
                    onClick={() => setFormData(prev => ({ ...prev, category: key as SuggestionCategory }))}
                    className={`p-3 rounded-lg border-2 transition-all text-center ${
                      formData.category === key
                        ? 'border-purple-500 bg-purple-500/20 text-purple-300'
                        : 'border-gray-600 bg-gray-600/10 text-gray-300 hover:border-gray-500 hover:bg-gray-600/20'
                    }`}
                  >
                    <div className="text-sm font-medium">{label}</div>
                  </button>
                ))}
              </div>
            </div>

            {/* 제목 입력 */}
            <div>
              <label htmlFor="title" className="block text-white font-medium mb-3">
                제목 <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                id="title"
                value={formData.title}
                onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
                placeholder="건의사항의 제목을 입력해주세요"
                className="w-full px-4 py-3 bg-[#2a2e45]/60 border border-purple-500/30 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-400/20 transition-all"
                maxLength={100}
                required
              />
              <div className="text-right text-sm text-gray-400 mt-1">
                {formData.title.length}/100
              </div>
            </div>

            {/* 내용 입력 */}
            <div>
              <label htmlFor="content" className="block text-white font-medium mb-3">
                내용 <span className="text-red-400">*</span>
              </label>
              <textarea
                id="content"
                value={formData.content}
                onChange={(e) => setFormData(prev => ({ ...prev, content: e.target.value }))}
                placeholder="건의사항의 상세 내용을 입력해주세요&#10;&#10;• 구체적인 상황이나 문제점을 설명해주세요&#10;• 개선 방안이 있다면 함께 제안해주세요&#10;• 스크린샷이나 예시가 있다면 더욱 도움이 됩니다"
                className="w-full px-4 py-3 bg-[#2a2e45]/60 border border-purple-500/30 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-400/20 transition-all resize-none"
                rows={12}
                maxLength={2000}
                required
              />
              <div className="text-right text-sm text-gray-400 mt-1">
                {formData.content.length}/2000
              </div>
            </div>

            {/* 안내 메시지 */}
            <div className="bg-blue-500/10 border border-blue-500/30 rounded-lg p-4">
              <div className="flex items-start gap-3">
                <span className="text-blue-400 text-xl">💡</span>
                <div className="text-blue-300 text-sm">
                  <p className="font-medium mb-2">건의사항 작성 가이드</p>
                  <ul className="space-y-1 text-blue-200">
                    <li>• 구체적이고 명확한 내용으로 작성해주세요</li>
                    <li>• 비슷한 건의사항이 있는지 먼저 확인해주세요</li>
                    <li>• 관리자가 검토 후 답변을 드립니다</li>
                    <li>• 부적절한 내용은 삭제될 수 있습니다</li>
                  </ul>
                </div>
              </div>
            </div>

            {/* 버튼 */}
            <div className="flex gap-4 pt-4">
              <button
                type="button"
                onClick={() => navigate('/suggestions')}
                className="flex-1 px-6 py-3 bg-gray-600/30 hover:bg-gray-600/50 text-gray-300 font-medium rounded-lg transition-all duration-200"
                disabled={loading}
              >
                취소
              </button>
              <button
                type="submit"
                disabled={loading || !formData.title.trim() || !formData.content.trim()}
                className="flex-1 px-6 py-3 bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 disabled:from-gray-600 disabled:to-gray-600 text-white font-medium rounded-lg shadow-lg hover:shadow-xl transition-all duration-200 transform hover:scale-105 disabled:scale-100 disabled:cursor-not-allowed"
              >
                {loading ? '제출 중...' : '건의사항 제출'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}