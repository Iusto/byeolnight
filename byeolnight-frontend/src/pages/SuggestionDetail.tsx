import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { getSuggestion, deleteSuggestion } from '../lib/api/suggestion';
import type { Suggestion } from '../types/suggestion';

const CATEGORIES = {
  FEATURE: '기능 개선',
  BUG: '버그 신고',
  UI_UX: 'UI/UX 개선',
  CONTENT: '콘텐츠 관련',
  OTHER: '기타'
} as const;

const STATUS = {
  PENDING: '검토 중',
  IN_PROGRESS: '진행 중',
  COMPLETED: '완료',
  REJECTED: '거절'
} as const;

const STATUS_COLORS = {
  PENDING: 'bg-yellow-500/20 text-yellow-300 border-yellow-500/30',
  IN_PROGRESS: 'bg-blue-500/20 text-blue-300 border-blue-500/30',
  COMPLETED: 'bg-green-500/20 text-green-300 border-green-500/30',
  REJECTED: 'bg-red-500/20 text-red-300 border-red-500/30'
};

const CATEGORY_COLORS = {
  FEATURE: 'bg-purple-500/20 text-purple-300',
  BUG: 'bg-red-500/20 text-red-300',
  UI_UX: 'bg-blue-500/20 text-blue-300',
  CONTENT: 'bg-green-500/20 text-green-300',
  OTHER: 'bg-gray-500/20 text-gray-300'
};



export default function SuggestionDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [suggestion, setSuggestion] = useState<Suggestion | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) {
      navigate('/suggestions');
      return;
    }

    fetchSuggestion();
  }, [id, navigate]);

  const fetchSuggestion = async () => {
    try {
      setLoading(true);
      const data = await getSuggestion(Number(id));
      setSuggestion(data);
    } catch (error) {
      console.error('건의사항 조회 실패:', error);
      setSuggestion(null);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!suggestion || !window.confirm('정말로 삭제하시겠습니까?')) {
      return;
    }

    try {
      await deleteSuggestion(suggestion.id);
      alert('건의사항이 삭제되었습니다.');
      navigate('/suggestions');
    } catch (error: any) {
      console.error('건의사항 삭제 실패:', error);
      const errorMessage = error.response?.data?.message || '건의사항 삭제에 실패했습니다.';
      alert(errorMessage);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0f1419] via-[#1a1f2e] to-[#2d1b69] flex items-center justify-center">
        <div className="text-white text-xl">로딩 중...</div>
      </div>
    );
  }

  if (!suggestion) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0f1419] via-[#1a1f2e] to-[#2d1b69] flex items-center justify-center">
        <div className="text-center">
          <div className="text-6xl mb-4">😕</div>
          <p className="text-gray-400 text-lg mb-4">건의사항을 찾을 수 없습니다.</p>
          <Link
            to="/suggestions"
            className="px-6 py-3 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors"
          >
            목록으로 돌아가기
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0f1419] via-[#1a1f2e] to-[#2d1b69] py-8">
      <div className="max-w-4xl mx-auto px-6">
        {/* 뒤로가기 버튼 */}
        <div className="mb-6">
          <Link
            to="/suggestions"
            className="inline-flex items-center gap-2 text-purple-300 hover:text-purple-200 transition-colors"
          >
            <span>←</span>
            <span>목록으로 돌아가기</span>
          </Link>
        </div>

        {/* 건의사항 상세 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-sm rounded-xl border border-purple-500/20 overflow-hidden">
          {/* 헤더 */}
          <div className="p-8 border-b border-purple-500/20">
            <div className="flex items-start justify-between mb-4">
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-3">
                  <span className={`px-3 py-1 rounded-full text-sm font-medium ${CATEGORY_COLORS[suggestion.category]}`}>
                    {CATEGORIES[suggestion.category]}
                  </span>
                  <span className={`px-3 py-1 rounded-full text-sm font-medium border ${STATUS_COLORS[suggestion.status]}`}>
                    {STATUS[suggestion.status]}
                  </span>
                </div>
                <h1 className="text-2xl font-bold text-white mb-4">{suggestion.title}</h1>
              </div>
            </div>

            <div className="flex items-center gap-6 text-sm text-gray-400">
              <div className="flex items-center gap-2">
                <span>👤</span>
                <span>{suggestion.authorNickname}</span>
              </div>
              <div className="flex items-center gap-2">
                <span>📅</span>
                <span>{new Date(suggestion.createdAt).toLocaleString('ko-KR')}</span>
              </div>
              {suggestion.updatedAt !== suggestion.createdAt && (
                <div className="flex items-center gap-2">
                  <span>✏️</span>
                  <span>수정: {new Date(suggestion.updatedAt).toLocaleString('ko-KR')}</span>
                </div>
              )}
            </div>
          </div>

          {/* 내용 */}
          <div className="p-8">
            <div className="prose prose-invert max-w-none">
              <div className="text-gray-300 whitespace-pre-wrap leading-relaxed">
                {suggestion.content}
              </div>
            </div>
          </div>

          {/* 관리자 답변 */}
          {suggestion.adminResponse && (
            <div className="mx-8 mb-8 bg-green-500/10 border border-green-500/30 rounded-lg overflow-hidden">
              <div className="bg-green-500/20 px-6 py-3 border-b border-green-500/30">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="text-green-400">✅</span>
                    <span className="text-green-300 font-medium">관리자 답변</span>
                  </div>
                  <div className="text-sm text-green-400">
                    {suggestion.adminNickname} • {new Date(suggestion.adminResponseAt!).toLocaleString('ko-KR')}
                  </div>
                </div>
              </div>
              <div className="p-6">
                <div className="text-green-200 whitespace-pre-wrap leading-relaxed">
                  {suggestion.adminResponse}
                </div>
              </div>
            </div>
          )}

          {/* 액션 버튼 */}
          {user && user.id === suggestion.authorId && suggestion.status === 'PENDING' && (
            <div className="p-8 border-t border-purple-500/20">
              <div className="flex gap-4">
                <Link 
                  to={`/suggestions/${suggestion.id}/edit`}
                  className="px-6 py-2 bg-blue-600/20 hover:bg-blue-600/40 text-blue-300 border border-blue-500/30 rounded-lg transition-all text-center"
                >
                  수정하기
                </Link>
                <button 
                  onClick={handleDelete}
                  className="px-6 py-2 bg-red-600/20 hover:bg-red-600/40 text-red-300 border border-red-500/30 rounded-lg transition-all"
                >
                  삭제하기
                </button>
              </div>
            </div>
          )}
        </div>

        {/* 관련 건의사항 */}
        <div className="mt-8">
          <h2 className="text-xl font-bold text-white mb-4">관련 건의사항</h2>
          <div className="text-center py-8 text-gray-400">
            관련 건의사항이 없습니다.
          </div>
        </div>
      </div>
    </div>
  );
}