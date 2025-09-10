import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';
import { getSuggestion, deleteSuggestion, addAdminResponse, updateSuggestionStatus } from '../lib/api/suggestion';
import UserIconDisplay from '../components/UserIconDisplay';
import type { Suggestion } from '../types/suggestion';

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
  const { t } = useTranslation();
  
  const getCategories = () => ({
    FEATURE: t('suggestion.categories.FEATURE'),
    BUG: t('suggestion.categories.BUG'),
    UI_UX: t('suggestion.categories.UI_UX'),
    CONTENT: t('suggestion.categories.CONTENT'),
    OTHER: t('suggestion.categories.OTHER')
  });

  const getStatuses = () => ({
    PENDING: t('suggestion.statuses.PENDING'),
    IN_PROGRESS: t('suggestion.statuses.IN_PROGRESS'),
    COMPLETED: t('suggestion.statuses.COMPLETED'),
    REJECTED: t('suggestion.statuses.REJECTED')
  });
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
    if (!suggestion || !window.confirm(t('suggestion.delete_confirm'))) {
      return;
    }

    try {
      await deleteSuggestion(suggestion.id);
      alert(t('suggestion.delete_success'));
      navigate('/suggestions');
    } catch (error: any) {
      console.error('건의사항 삭제 실패:', error);
      const errorMessage = error.response?.data?.message || t('suggestion.delete_failed');
      alert(errorMessage);
    }
  };

  const handleAdminResponse = async () => {
    if (!adminResponse.trim()) {
      alert(t('suggestion.response_required'));
      return;
    }

    try {
      setSubmitting(true);
      await addAdminResponse(Number(id), {
        response: adminResponse,
        status: responseStatus
      });
      alert(t('suggestion.response_success'));
      setShowAdminResponse(false);
      setAdminResponse('');
      fetchSuggestion(); // 새로고침
    } catch (error: any) {
      console.error('관리자 답변 등록 실패:', error);
      const errorMessage = error.response?.data?.message || t('suggestion.response_failed');
      alert(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusChange = async (newStatus: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED') => {
    if (newStatus === suggestion?.status) return;

    try {
      await updateSuggestionStatus(Number(id), newStatus);
      alert(t('suggestion.status_change_success'));
      fetchSuggestion(); // 새로고침
    } catch (error: any) {
      console.error('상태 변경 실패:', error);
      const errorMessage = error.response?.data?.message || t('suggestion.status_change_failed');
      alert(errorMessage);
    }
  };

  const handleEditResponse = async () => {
    if (!editResponse.trim()) {
      alert(t('suggestion.response_required'));
      return;
    }

    try {
      setSubmitting(true);
      await addAdminResponse(Number(id), {
        response: editResponse,
        status: editStatus
      });
      alert(t('suggestion.response_edit_success'));
      setEditingResponse(false);
      setEditResponse('');
      fetchSuggestion(); // 새로고침
    } catch (error: any) {
      console.error('관리자 답변 수정 실패:', error);
      const errorMessage = error.response?.data?.message || t('suggestion.response_edit_failed');
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
        <div className="text-white text-xl">{t('suggestion.loading')}</div>
      </div>
    );
  }

  if (!loading && !user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0f1419] via-[#1a1f2e] to-[#2d1b69] flex items-center justify-center">
        <div className="text-center">
          <div className="text-6xl mb-4">🔐</div>
          <p className="text-gray-400 text-lg mb-4">{t('suggestion.login_required')}</p>
          <Link
            to="/login"
            className="px-6 py-3 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors"
          >
            {t('suggestion.go_to_login')}
          </Link>
        </div>
      </div>
    );
  }

  if (!suggestion) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0f1419] via-[#1a1f2e] to-[#2d1b69] flex items-center justify-center">
        <div className="text-center">
          <div className="text-6xl mb-4">😕</div>
          <p className="text-gray-400 text-lg mb-4">{t('suggestion.not_found')}</p>
          <Link
            to="/suggestions"
            className="px-6 py-3 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors"
          >
            {t('suggestion.back_to_list')}
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
          <p className="text-gray-400 text-lg mb-4">{t('suggestion.private_access_denied')}</p>
          <Link
            to="/suggestions"
            className="px-6 py-3 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors"
          >
            {t('suggestion.back_to_list')}
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0f1419] via-[#1a1f2e] to-[#2d1b69] py-4 sm:py-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6">
        {/* 뒤로가기 버튼 - 모바일 최적화 */}
        <div className="mb-3 sm:mb-6">
          <Link
            to="/suggestions"
            className="inline-flex items-center gap-2 text-purple-300 hover:text-purple-200 active:text-purple-100 transition-colors min-h-[44px] px-2 py-2 touch-manipulation"
          >
            <span className="text-lg">←</span>
            <span className="text-sm sm:text-base">{t('suggestion.back_to_list')}</span>
          </Link>
        </div>

        {/* 건의사항 상세 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-sm rounded-xl border border-purple-500/20 overflow-hidden">
          {/* 헤더 - 모바일 최적화 */}
          <div className="p-3 sm:p-8 border-b border-purple-500/20">
            <div className="space-y-3 sm:space-y-4">
              <div className="flex flex-wrap gap-1.5 sm:gap-2 mb-2 sm:mb-3">
                <span className={`px-2 sm:px-3 py-1 rounded-full text-xs font-medium ${CATEGORY_COLORS[suggestion.category]}`}>
                  {getCategories()[suggestion.category]}
                </span>
                <span className={`px-2 sm:px-3 py-1 rounded-full text-xs font-medium border ${STATUS_COLORS[suggestion.status]}`}>
                  {getStatuses()[suggestion.status]}
                </span>
                {!suggestion.isPublic && (
                  <span className="px-2 sm:px-3 py-1 rounded-full text-xs font-medium bg-gray-500/20 text-gray-300 border border-gray-500/30">
                    {t('suggestion.private')}
                  </span>
                )}
              </div>
              <h1 className="text-lg sm:text-2xl font-bold text-white leading-tight">{suggestion.title}</h1>
            </div>

            <div className="flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-6 text-xs sm:text-sm text-gray-400 mt-4">
              <div className="flex items-center gap-2">
                <UserIconDisplay iconName={suggestion.authorIcon} size="small" className="text-sm sm:text-lg" />
                <span className="whitespace-nowrap">{suggestion.authorNickname}</span>
              </div>
              <div className="flex items-center gap-2">
                <span>📅</span>
                <span className="hidden sm:inline">{new Date(suggestion.createdAt).toLocaleString('ko-KR')}</span>
                <span className="sm:hidden">{new Date(suggestion.createdAt).toLocaleDateString('ko-KR')}</span>
              </div>
              {suggestion.updatedAt !== suggestion.createdAt && (
                <div className="flex items-center gap-2">
                  <span>✏️</span>
                  <span className="hidden sm:inline">수정: {new Date(suggestion.updatedAt).toLocaleString('ko-KR')}</span>
                  <span className="sm:hidden">수정: {new Date(suggestion.updatedAt).toLocaleDateString('ko-KR')}</span>
                </div>
              )}
            </div>
          </div>

          {/* 내용 - 모바일 최적화 */}
          <div className="p-3 sm:p-8">
            <div className="prose prose-invert max-w-none">
              <div className="text-gray-300 whitespace-pre-wrap leading-relaxed text-sm sm:text-base">
                {suggestion.content}
              </div>
            </div>
          </div>

          {/* 관리자 답변 - 모바일 최적화 */}
          {suggestion.adminResponse && !editingResponse && (
            <div className="mx-3 sm:mx-8 mb-4 sm:mb-8 bg-green-500/10 border border-green-500/30 rounded-lg overflow-hidden">
              <div className="bg-green-500/20 px-3 sm:px-6 py-2 sm:py-3 border-b border-green-500/30">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 sm:gap-3">
                  <div className="flex items-center gap-2">
                    <span className="text-green-400 text-sm">✅</span>
                    <span className="text-green-300 font-medium text-sm">{t('suggestion.admin_response')}</span>
                  </div>
                  <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4">
                    <div className="text-xs text-green-400">
                      {suggestion.adminNickname} • {new Date(suggestion.adminResponseAt!).toLocaleDateString()}
                    </div>
                    {user && user.role === 'ADMIN' && (
                      <button
                        onClick={startEditResponse}
                        className="text-xs px-2 py-1 bg-blue-600/20 hover:bg-blue-600/40 active:bg-blue-600/60 text-blue-300 border border-blue-500/30 rounded transition-all min-h-[36px] touch-manipulation self-start sm:self-auto"
                      >
                        {t('suggestion.modify_response')}
                      </button>
                    )}
                  </div>
                </div>
              </div>
              <div className="p-3 sm:p-6">
                <div className="text-green-200 whitespace-pre-wrap leading-relaxed text-sm">
                  {suggestion.adminResponse}
                </div>
              </div>
            </div>
          )}

          {/* 관리자 답변 수정 */}
          {editingResponse && (
            <div className="mx-3 sm:mx-8 mb-4 sm:mb-8 bg-green-500/10 border border-green-500/30 rounded-lg p-3 sm:p-6">
              <div className="mb-4">
                <label className="block text-green-300 font-medium mb-2">{t('suggestion.response_status')}</label>
                <select
                  value={editStatus}
                  onChange={(e) => setEditStatus(e.target.value as 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED')}
                  className="w-full px-3 py-2 bg-[#1f2336] border border-green-500/30 rounded-lg text-white focus:outline-none focus:border-green-400"
                >
                  <option value="IN_PROGRESS">{t('suggestion.statuses.IN_PROGRESS')}</option>
                  <option value="COMPLETED">{t('suggestion.statuses.COMPLETED')}</option>
                  <option value="REJECTED">{t('suggestion.statuses.REJECTED')}</option>
                </select>
              </div>
              <div className="mb-4">
                <label className="block text-green-300 font-medium mb-2">{t('suggestion.response_content')}</label>
                <textarea
                  value={editResponse}
                  onChange={(e) => setEditResponse(e.target.value)}
                  placeholder={t('suggestion.response_placeholder')}
                  className="w-full h-32 px-3 py-2 bg-[#1f2336] border border-green-500/30 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-green-400 resize-none"
                />
              </div>
              <div className="flex gap-3">
                <button
                  onClick={handleEditResponse}
                  disabled={submitting}
                  className="px-6 py-2 bg-green-600 hover:bg-green-700 disabled:bg-green-600/50 text-white rounded-lg transition-all font-medium"
                >
                  {submitting ? t('suggestion.modifying') : t('suggestion.modify_complete')}
                </button>
                <button
                  onClick={() => {
                    setEditingResponse(false);
                    setEditResponse('');
                  }}
                  className="px-6 py-2 bg-gray-600/20 hover:bg-gray-600/40 text-gray-300 border border-gray-500/30 rounded-lg transition-all"
                >
                  {t('suggestion.cancel')}
                </button>
              </div>
            </div>
          )}

          {/* 관리자 상태 변경 */}
          {user && user.role === 'ADMIN' && suggestion.adminResponse && (
            <div className="mx-3 sm:mx-8 mb-4 sm:mb-8">
              <div className="bg-blue-500/10 border border-blue-500/30 rounded-lg p-3 sm:p-4">
                <div className="flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-4">
                  <label className="text-blue-300 font-medium text-sm">{t('suggestion.change_status')}</label>
                  <select
                    value={suggestion.status}
                    onChange={(e) => handleStatusChange(e.target.value as 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED')}
                    className="px-3 py-2 bg-[#1f2336] border border-blue-500/30 rounded-lg text-white text-sm focus:outline-none focus:border-blue-400 min-h-[44px]"
                  >
                    <option value="PENDING">{t('suggestion.statuses.PENDING')}</option>
                    <option value="IN_PROGRESS">{t('suggestion.statuses.IN_PROGRESS')}</option>
                    <option value="COMPLETED">{t('suggestion.statuses.COMPLETED')}</option>
                    <option value="REJECTED">{t('suggestion.statuses.REJECTED')}</option>
                  </select>
                </div>
              </div>
            </div>
          )}

          {/* 관리자 답변 작성 */}
          {user && user.role === 'ADMIN' && !suggestion.adminResponse && (
            <div className="mx-3 sm:mx-8 mb-4 sm:mb-8">
              {!showAdminResponse ? (
                <button
                  onClick={() => setShowAdminResponse(true)}
                  className="w-full py-3 bg-green-600/20 hover:bg-green-600/40 text-green-300 border border-green-500/30 rounded-lg transition-all font-medium"
                >
                  {t('suggestion.write_admin_response')}
                </button>
              ) : (
                <div className="bg-green-500/10 border border-green-500/30 rounded-lg p-3 sm:p-6">
                  <div className="mb-4">
                    <label className="block text-green-300 font-medium mb-2">{t('suggestion.response_status')}</label>
                    <select
                      value={responseStatus}
                      onChange={(e) => setResponseStatus(e.target.value as 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED')}
                      className="w-full px-3 py-2 bg-[#1f2336] border border-green-500/30 rounded-lg text-white focus:outline-none focus:border-green-400"
                    >
                      <option value="IN_PROGRESS">{t('suggestion.statuses.IN_PROGRESS')}</option>
                      <option value="COMPLETED">{t('suggestion.statuses.COMPLETED')}</option>
                      <option value="REJECTED">{t('suggestion.statuses.REJECTED')}</option>
                    </select>
                  </div>
                  <div className="mb-4">
                    <label className="block text-green-300 font-medium mb-2">{t('suggestion.response_content')}</label>
                    <textarea
                      value={adminResponse}
                      onChange={(e) => setAdminResponse(e.target.value)}
                      placeholder={t('suggestion.response_placeholder')}
                      className="w-full h-32 px-3 py-2 bg-[#1f2336] border border-green-500/30 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-green-400 resize-none"
                    />
                  </div>
                  <div className="flex gap-3">
                    <button
                      onClick={handleAdminResponse}
                      disabled={submitting}
                      className="px-6 py-2 bg-green-600 hover:bg-green-700 disabled:bg-green-600/50 text-white rounded-lg transition-all font-medium"
                    >
                      {submitting ? t('suggestion.registering_response') : t('suggestion.register_response')}
                    </button>
                    <button
                      onClick={() => {
                        setShowAdminResponse(false);
                        setAdminResponse('');
                      }}
                      className="px-6 py-2 bg-gray-600/20 hover:bg-gray-600/40 text-gray-300 border border-gray-500/30 rounded-lg transition-all"
                    >
                      {t('suggestion.cancel')}
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* 액션 버튼 - 모바일 최적화 */}
          {user && user.id === suggestion.authorId && suggestion.status === 'PENDING' && (
            <div className="p-3 sm:p-8 border-t border-purple-500/20">
              <div className="flex flex-col sm:flex-row gap-2 sm:gap-4">
                <Link 
                  to={`/suggestions/${suggestion.id}/edit`}
                  className="flex items-center justify-center gap-2 px-4 py-3 bg-blue-600/20 hover:bg-blue-600/40 active:bg-blue-600/60 text-blue-300 border border-blue-500/30 rounded-lg transition-all text-center min-h-[48px] touch-manipulation text-sm font-medium"
                >
                  <span className="text-sm">✏️</span> {t('suggestion.edit')}
                </Link>
                <button 
                  onClick={handleDelete}
                  className="flex items-center justify-center gap-2 px-4 py-3 bg-red-600/20 hover:bg-red-600/40 active:bg-red-600/60 text-red-300 border border-red-500/30 rounded-lg transition-all min-h-[48px] touch-manipulation text-sm font-medium"
                >
                  <span className="text-sm">🗑</span> {t('suggestion.delete')}
                </button>
              </div>
            </div>
          )}
        </div>


      </div>
    </div>
  );
}