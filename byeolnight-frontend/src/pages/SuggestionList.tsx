import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';
import { getSuggestions } from '../lib/api/suggestion';
import type { Suggestion, SuggestionCategory, SuggestionStatus } from '../types/suggestion';

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

export default function SuggestionList() {
  const { user } = useAuth();
  const { t } = useTranslation();
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState<SuggestionCategory | 'ALL'>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<SuggestionStatus | 'ALL'>('ALL');

  // 다국어 지원을 위해 함수로 변경
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

  useEffect(() => {
    fetchSuggestions();
  }, [selectedCategory, selectedStatus]);

  const fetchSuggestions = async () => {
    try {
      setLoading(true);
      const response = await getSuggestions({
        category: selectedCategory === 'ALL' ? undefined : selectedCategory,
        status: selectedStatus === 'ALL' ? undefined : selectedStatus,
        page: 0,
        size: 20,
        sort: 'createdAt',
        direction: 'desc'
      });
      console.log('건의사항 API 응답:', response);
      setSuggestions(response?.suggestions || response?.content || []);
    } catch (error: any) {
      console.error('건의사항 목록 조회 실패:', error);
      if (error?.response?.status === 401) {
        console.log('인증 오류 - 로그인 상태 확인 필요');
      }
      setSuggestions([]);
    } finally {
      setLoading(false);
    }
  };

  // API에서 이미 필터링된 데이터를 받으므로 추가 필터링 불필요
  const filteredSuggestions = suggestions;

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0f1419] via-[#1a1f2e] to-[#2d1b69] flex items-center justify-center">
        <div className="text-white text-xl">{t('suggestion.loading')}</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0f1419] via-[#1a1f2e] to-[#2d1b69] py-4 sm:py-8">
      <div className="max-w-6xl mx-auto px-4 sm:px-6">
        {/* 헤더 - 모바일 최적화 */}
        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-6 sm:mb-8">
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-white mb-2">{t('suggestion.title')}</h1>
            <p className="text-gray-300 text-sm sm:text-base">{t('suggestion.subtitle')}</p>
          </div>
          
          {user && (
            <Link
              to="/suggestions/new"
              className="w-full sm:w-auto flex items-center justify-center gap-2 px-4 sm:px-6 py-3 bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 active:from-purple-800 active:to-blue-800 text-white font-medium rounded-lg shadow-lg hover:shadow-xl transition-all duration-200 transform hover:scale-105 active:scale-95 min-h-[48px] touch-manipulation"
            >
              {t('suggestion.write_suggestion')}
            </Link>
          )}
        </div>

        {/* 필터 - 모바일 최적화 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-sm rounded-xl p-4 sm:p-6 mb-6 border border-purple-500/20">
          <div className="space-y-4">
            {/* 카테고리 필터 */}
            <div className="space-y-2">
              <span className="text-gray-300 font-medium text-sm">{t('suggestion.category')}:</span>
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => setSelectedCategory('ALL')}
                  className={`px-3 py-2 rounded-full text-xs sm:text-sm transition-all min-h-[36px] touch-manipulation ${
                    selectedCategory === 'ALL' 
                      ? 'bg-purple-600 text-white' 
                      : 'bg-gray-600/30 text-gray-300 hover:bg-gray-600/50 active:bg-gray-600/70'
                  }`}
                >
                  {t('suggestion.all')}
                </button>
                {Object.entries(getCategories()).map(([key, label]) => (
                  <button
                    key={key}
                    onClick={() => setSelectedCategory(key as SuggestionCategory)}
                    className={`px-3 py-2 rounded-full text-xs sm:text-sm transition-all min-h-[36px] touch-manipulation ${
                      selectedCategory === key 
                        ? 'bg-purple-600 text-white' 
                        : 'bg-gray-600/30 text-gray-300 hover:bg-gray-600/50 active:bg-gray-600/70'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            {/* 상태 필터 */}
            <div className="space-y-2">
              <span className="text-gray-300 font-medium text-sm">{t('suggestion.status')}:</span>
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => setSelectedStatus('ALL')}
                  className={`px-3 py-2 rounded-full text-xs sm:text-sm transition-all min-h-[36px] touch-manipulation ${
                    selectedStatus === 'ALL' 
                      ? 'bg-purple-600 text-white' 
                      : 'bg-gray-600/30 text-gray-300 hover:bg-gray-600/50 active:bg-gray-600/70'
                  }`}
                >
                  {t('suggestion.all')}
                </button>
                {Object.entries(getStatuses()).map(([key, label]) => (
                  <button
                    key={key}
                    onClick={() => setSelectedStatus(key as SuggestionStatus)}
                    className={`px-3 py-2 rounded-full text-xs sm:text-sm transition-all min-h-[36px] touch-manipulation ${
                      selectedStatus === key 
                        ? 'bg-purple-600 text-white' 
                        : 'bg-gray-600/30 text-gray-300 hover:bg-gray-600/50 active:bg-gray-600/70'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* 건의사항 목록 */}
        <div className="space-y-4">
          {filteredSuggestions.length === 0 ? (
            <div className="text-center py-12">
              <div className="text-4xl sm:text-6xl mb-4">📝</div>
              <p className="text-gray-400 text-base sm:text-lg mb-4">{t('suggestion.no_suggestions')}</p>
              {user && (
                <Link
                  to="/suggestions/new"
                  className="inline-flex items-center justify-center gap-2 px-6 py-3 bg-purple-600 hover:bg-purple-700 active:bg-purple-800 text-white rounded-lg transition-colors min-h-[48px] touch-manipulation"
                >
                  ✏️ {t('suggestion.write_first_suggestion')}
                </Link>
              )}
            </div>
          ) : (
            filteredSuggestions.map((suggestion) => {
              const categories = getCategories();
              const statuses = getStatuses();
              return (
              <div
                key={suggestion.id}
                className="bg-[#1f2336]/80 backdrop-blur-sm rounded-xl p-4 sm:p-6 border border-purple-500/20 hover:border-purple-400/40 transition-all duration-200 hover:shadow-lg"
              >
                <div className="space-y-3 sm:space-y-4">
                  <div className="flex flex-wrap gap-2 mb-3">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${CATEGORY_COLORS[suggestion.category]}`}>
                      {categories[suggestion.category]}
                    </span>
                    <span className={`px-2 py-1 rounded-full text-xs font-medium border ${STATUS_COLORS[suggestion.status]}`}>
                      {statuses[suggestion.status]}
                    </span>
                    {!suggestion.isPublic && (
                      <span className="px-2 py-1 rounded-full text-xs font-medium bg-gray-500/20 text-gray-300 border border-gray-500/30">
                        {t('suggestion.private')}
                      </span>
                    )}
                  </div>
                  
                  {/* 제목: 공개/비공개 및 권한에 따라 Link 또는 span */}
                  {(suggestion.isPublic || (user && (user.role === 'ADMIN' || user.id === suggestion.authorId))) ? (
                    <Link
                      to={`/suggestions/${suggestion.id}`}
                      className="block text-lg sm:text-xl font-bold text-white hover:text-purple-300 active:text-purple-200 transition-colors leading-tight touch-manipulation"
                    >
                      {suggestion.isPublic ? suggestion.title : t('suggestion.private_suggestion')}
                    </Link>
                  ) : (
                    <span
                      className="block text-lg sm:text-xl font-bold text-gray-500 cursor-not-allowed opacity-60 select-none leading-tight"
                      title={t('suggestion.private_suggestion_tooltip')}
                    >
                      {t('suggestion.private_suggestion')}
                    </span>
                  )}
                </div>

                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2 sm:gap-4 text-xs sm:text-sm text-gray-400 mt-4">
                  <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-4">
                    <span>{t('suggestion.author')} {suggestion.authorNickname}</span>
                    <span className="hidden sm:inline">•</span>
                    <span>{new Date(suggestion.createdAt).toLocaleDateString()}</span>
                  </div>
                  
                  {suggestion.adminResponse && (
                    <div className="flex items-center gap-1 text-green-400">
                      <span>✅</span>
                      <span className="hidden sm:inline">{t('suggestion.admin_response_completed')}</span>
                      <span className="sm:hidden">답변완료</span>
                    </div>
                  )}
                </div>
              </div>
            )})
          )}
        </div>
      </div>
    </div>
  );
}