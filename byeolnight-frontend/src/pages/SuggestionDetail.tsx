import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { getSuggestion, deleteSuggestion, addAdminResponse, updateSuggestionStatus } from '../lib/api/suggestion';
import UserIconDisplay from '../components/UserIconDisplay';
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
  const [showAdminResponse, setShowAdminResponse] = useState(false);
  const [adminResponse, setAdminResponse] = useState('');
  const [responseStatus, setResponseStatus] = useState<'IN_PROGRESS' | 'COMPLETED' | 'REJECTED'>('IN_PROGRESS');
  const [submitting, setSubmitting] = useState(false);
  const [editingResponse, setEditingResponse] = useState(false);
  const [editResponse, setEditResponse] = useState('');
  const [editStatus, setEditStatus] = useState<'IN_PROGRESS' | 'COMPLETED' | 'REJECTED'>('IN_PROGRESS');

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

  const handleAdminResponse = async () => {
    if (!adminResponse.trim()) {
      alert('답변 내용을 입력해주세요.');
      return;
    }

    try {
      setSubmitting(true);
      await addAdminResponse(Number(id), {
        response: adminResponse,
        status: responseStatus
      });
      alert('관리자 답변이 등록되었습니다.');
      setShowAdminResponse(false);
      setAdminResponse('');
      fetchSuggestion(); // 새로고침
    } catch (error: any) {
      console.error('관리자 답변 등록 실패:', error);
      const errorMessage = error.response?.data?.message || '답변 등록에 실패했습니다.';
      alert(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusChange = async (newStatus: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED') => {
    if (newStatus === suggestion?.status) return;

    try {
      await updateSuggestionStatus(Number(id), newStatus);
      alert('상태가 변경되었습니다.');
      fetchSuggestion(); // 새로고침
    } catch (error: any) {
      console.error('상태 변경 실패:', error);
      const errorMessage = error.response?.data?.message || '상태 변경에 실패했습니다.';
      alert(errorMessage);
    }
  };

  const handleEditResponse = async () => {
    if (!editResponse.trim()) {
      alert('답변 내용을 입력해주세요.');
      return;
    }

    try {
      setSubmitting(true);
      await addAdminResponse(Number(id), {
        response: editResponse,
        status: editStatus
      });
      alert('관리자 답변이 수정되었습니다.');
      setEditingResponse(false);
      setEditResponse('');
      fetchSuggestion(); // 새로고침
    } catch (error: any) {
      console.error('관리자 답변 수정 실패:', error);
      const errorMessage = error.response?.data?.message || '답변 수정에 실패했습니다.';
      alert(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const startEditResponse = () => {
    if (suggestion?.adminResponse) {
      setEditResponse(suggestion.adminResponse);
      setEditStatus(suggestion.status as 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED');
      setEditingResponse(true);
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

  // 비공개 건의사항 접근 제한
  if (!suggestion.isPublic && user?.role !== 'ADMIN' && user?.id !== suggestion.authorId) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0f1419] via-[#1a1f2e] to-[#2d1b69] flex items-center justify-center">
        <div className="text-center">
          <div className="text-6xl mb-4">🔒</div>
          <p className="text-gray-400 text-lg mb-4">비공개 건의사항입니다.</p>
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
                  {!suggestion.isPublic && (
                    <span className="px-3 py-1 rounded-full text-sm font-medium bg-gray-500/20 text-gray-300 border border-gray-500/30">
                      🔒 비공개
                    </span>
                  )}
                </div>
                <h1 className="text-2xl font-bold text-white mb-4">{suggestion.title}</h1>
              </div>
            </div>

            <div className="flex items-center gap-6 text-sm text-gray-400">
              <div className="flex items-center gap-2">
                <UserIconDisplay iconName={suggestion.authorIcon} size="small" className="text-lg" />
                <span className="whitespace-nowrap">{suggestion.authorNickname}</span>
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
          {suggestion.adminResponse && !editingResponse && (
            <div className="mx-8 mb-8 bg-green-500/10 border border-green-500/30 rounded-lg overflow-hidden">
              <div className="bg-green-500/20 px-6 py-3 border-b border-green-500/30">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="text-green-400">✅</span>
                    <span className="text-green-300 font-medium">관리자 답변</span>
                  </div>
                  <div className="flex items-center gap-4">
                    <div className="text-sm text-green-400">
                      {suggestion.adminNickname} • {new Date(suggestion.adminResponseAt!).toLocaleString('ko-KR')}
                    </div>
                    {user && user.role === 'ADMIN' && (
                      <button
                        onClick={startEditResponse}
                        className="text-sm px-3 py-1 bg-blue-600/20 hover:bg-blue-600/40 text-blue-300 border border-blue-500/30 rounded transition-all"
                      >
                        수정
                      </button>
                    )}
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

          {/* 관리자 답변 수정 */}
          {editingResponse && (
            <div className="mx-8 mb-8 bg-green-500/10 border border-green-500/30 rounded-lg p-6">
              <div className="mb-4">
                <label className="block text-green-300 font-medium mb-2">답변 상태</label>
                <select
                  value={editStatus}
                  onChange={(e) => setEditStatus(e.target.value as 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED')}
                  className="w-full px-3 py-2 bg-[#1f2336] border border-green-500/30 rounded-lg text-white focus:outline-none focus:border-green-400"
                >
                  <option value="IN_PROGRESS">진행 중</option>
                  <option value="COMPLETED">완료</option>
                  <option value="REJECTED">거절</option>
                </select>
              </div>
              <div className="mb-4">
                <label className="block text-green-300 font-medium mb-2">답변 내용</label>
                <textarea
                  value={editResponse}
                  onChange={(e) => setEditResponse(e.target.value)}
                  placeholder="건의사항에 대한 답변을 작성해주세요..."
                  className="w-full h-32 px-3 py-2 bg-[#1f2336] border border-green-500/30 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-green-400 resize-none"
                />
              </div>
              <div className="flex gap-3">
                <button
                  onClick={handleEditResponse}
                  disabled={submitting}
                  className="px-6 py-2 bg-green-600 hover:bg-green-700 disabled:bg-green-600/50 text-white rounded-lg transition-all font-medium"
                >
                  {submitting ? '수정 중...' : '수정 완료'}
                </button>
                <button
                  onClick={() => {
                    setEditingResponse(false);
                    setEditResponse('');
                  }}
                  className="px-6 py-2 bg-gray-600/20 hover:bg-gray-600/40 text-gray-300 border border-gray-500/30 rounded-lg transition-all"
                >
                  취소
                </button>
              </div>
            </div>
          )}

          {/* 관리자 상태 변경 */}
          {user && user.role === 'ADMIN' && suggestion.adminResponse && (
            <div className="mx-8 mb-8">
              <div className="bg-blue-500/10 border border-blue-500/30 rounded-lg p-4">
                <div className="flex items-center gap-4">
                  <label className="text-blue-300 font-medium">상태 변경:</label>
                  <select
                    value={suggestion.status}
                    onChange={(e) => handleStatusChange(e.target.value as 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED')}
                    className="px-3 py-2 bg-[#1f2336] border border-blue-500/30 rounded-lg text-white focus:outline-none focus:border-blue-400"
                  >
                    <option value="PENDING">검토 중</option>
                    <option value="IN_PROGRESS">진행 중</option>
                    <option value="COMPLETED">완료</option>
                    <option value="REJECTED">거절</option>
                  </select>
                </div>
              </div>
            </div>
          )}

          {/* 관리자 답변 작성 */}
          {user && user.role === 'ADMIN' && !suggestion.adminResponse && (
            <div className="mx-8 mb-8">
              {!showAdminResponse ? (
                <button
                  onClick={() => setShowAdminResponse(true)}
                  className="w-full py-3 bg-green-600/20 hover:bg-green-600/40 text-green-300 border border-green-500/30 rounded-lg transition-all font-medium"
                >
                  📝 관리자 답변 작성
                </button>
              ) : (
                <div className="bg-green-500/10 border border-green-500/30 rounded-lg p-6">
                  <div className="mb-4">
                    <label className="block text-green-300 font-medium mb-2">답변 상태</label>
                    <select
                      value={responseStatus}
                      onChange={(e) => setResponseStatus(e.target.value as 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED')}
                      className="w-full px-3 py-2 bg-[#1f2336] border border-green-500/30 rounded-lg text-white focus:outline-none focus:border-green-400"
                    >
                      <option value="IN_PROGRESS">진행 중</option>
                      <option value="COMPLETED">완료</option>
                      <option value="REJECTED">거절</option>
                    </select>
                  </div>
                  <div className="mb-4">
                    <label className="block text-green-300 font-medium mb-2">답변 내용</label>
                    <textarea
                      value={adminResponse}
                      onChange={(e) => setAdminResponse(e.target.value)}
                      placeholder="건의사항에 대한 답변을 작성해주세요..."
                      className="w-full h-32 px-3 py-2 bg-[#1f2336] border border-green-500/30 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-green-400 resize-none"
                    />
                  </div>
                  <div className="flex gap-3">
                    <button
                      onClick={handleAdminResponse}
                      disabled={submitting}
                      className="px-6 py-2 bg-green-600 hover:bg-green-700 disabled:bg-green-600/50 text-white rounded-lg transition-all font-medium"
                    >
                      {submitting ? '등록 중...' : '답변 등록'}
                    </button>
                    <button
                      onClick={() => {
                        setShowAdminResponse(false);
                        setAdminResponse('');
                      }}
                      className="px-6 py-2 bg-gray-600/20 hover:bg-gray-600/40 text-gray-300 border border-gray-500/30 rounded-lg transition-all"
                    >
                      취소
                    </button>
                  </div>
                </div>
              )}
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


      </div>
    </div>
  );
}