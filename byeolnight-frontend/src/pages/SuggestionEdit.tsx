import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';
import axios from '../lib/axios';
import { getErrorMessage } from '../types/api';

export default function SuggestionEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { t } = useTranslation();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('FEATURE');
  const [isPublic, setIsPublic] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const getCategories = () => ({
    FEATURE: t('suggestion.categories.FEATURE'),
    BUG: t('suggestion.categories.BUG'),
    UI_UX: t('suggestion.categories.UI_UX'),
    CONTENT: t('suggestion.categories.CONTENT'),
    OTHER: t('suggestion.categories.OTHER')
  });

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }
    fetchSuggestion();
  }, [id, user]);

  const fetchSuggestion = async () => {
    try {
      const response = await axios.get(`/member/suggestions/${id}`);
      console.log('건의사항 조회 응답:', response.data);
      const suggestion = response.data.data;
      
      console.log('건의사항 데이터:', suggestion);
      console.log('현재 사용자:', user);
      console.log('작성자 비교:', suggestion.authorNickname, 'vs', user?.nickname);
      
      // 작성자만 수정 가능
      if (suggestion.authorNickname !== user?.nickname) {
        setError(t('suggestion.no_edit_permission'));
        return;
      }
      
      setTitle(suggestion.title);
      setContent(suggestion.content);
      setCategory(suggestion.category);
      setIsPublic(suggestion.isPublic);
    } catch (error) {
      console.error('건의사항 조회 실패:', error);
      setError(t('suggestion.cannot_load'));
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!title.trim() || !content.trim()) {
      setError(t('suggestion.validation_error'));
      return;
    }

    try {
      await axios.put(`/member/suggestions/${id}`, {
        title: title.trim(),
        content: content.trim(),
        category,
        isPublic
      });
      
      navigate(`/suggestions/${id}`);
    } catch (error: unknown) {
      setError(getErrorMessage(error));
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 flex items-center justify-center">
        <div className="text-white text-lg">{t('suggestion.loading')}</div>
      </div>
    );
  }

  if (error && !title) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 flex items-center justify-center">
        <div className="text-center">
          <div className="text-red-400 text-lg mb-4">{error}</div>
          <button 
            onClick={() => navigate('/suggestions')}
            className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded transition"
          >
            {t('suggestion.back_to_list')}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white">
      <div className="relative overflow-hidden bg-gradient-to-r from-purple-900/50 to-pink-900/50 border-b border-purple-500/20">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/10 to-pink-600/10"></div>
        <div className="relative max-w-4xl mx-auto px-4 sm:px-6 py-8 sm:py-16">
          <div className="text-center">
            <div className="w-12 h-12 sm:w-16 sm:h-16 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-2xl sm:text-3xl mx-auto mb-4 sm:mb-6 shadow-lg">
              ✏️
            </div>
            <h1 className="text-2xl sm:text-4xl md:text-5xl font-bold bg-gradient-to-r from-white via-purple-200 to-white bg-clip-text text-transparent mb-4">
              {t('suggestion.edit_title')}
            </h1>
            <p className="text-base sm:text-xl text-gray-300">{t('suggestion.edit_subtitle')}</p>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 sm:px-6 py-4 sm:py-8">
        <div className="bg-gradient-to-br from-slate-800/50 to-purple-900/30 backdrop-blur-md rounded-xl sm:rounded-2xl p-4 sm:p-8 border border-purple-500/20 shadow-2xl">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* 카테고리 선택 - 모바일 최적화 */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">{t('suggestion.category')}</label>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-2 sm:gap-3">
                {Object.entries(getCategories()).map(([key, label]) => (
                  <button
                    key={key}
                    type="button"
                    onClick={() => setCategory(key)}
                    className={`p-3 rounded-lg border-2 transition-all text-center min-h-[48px] touch-manipulation ${
                      category === key
                        ? 'border-purple-500 bg-purple-500/20 text-purple-300'
                        : 'border-gray-600 bg-gray-600/10 text-gray-300 hover:border-gray-500 hover:bg-gray-600/20 active:bg-gray-600/30'
                    }`}
                  >
                    <div className="text-sm font-medium">{label}</div>
                  </button>
                ))}
              </div>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">{t('suggestion.title_label')}</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder={t('suggestion.title_placeholder')}
                required
                className="w-full px-4 py-3 rounded-xl bg-slate-700/50 text-white border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent placeholder-gray-400 transition-all duration-200 text-base min-h-[48px]"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">{t('suggestion.content_label')}</label>
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder={t('suggestion.content_placeholder')}
                required
                rows={window.innerWidth < 640 ? 8 : 10}
                className="w-full px-4 py-3 rounded-xl bg-slate-700/50 text-white border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent placeholder-gray-400 transition-all duration-200 resize-none text-base"
              />
            </div>

            <div className="flex items-center gap-3 p-3 bg-gray-600/10 rounded-lg border border-gray-600/30">
              <input
                type="checkbox"
                id="isPublic"
                checked={isPublic}
                onChange={(e) => setIsPublic(e.target.checked)}
                className="w-5 h-5 text-purple-600 bg-slate-700 border-slate-600 rounded focus:ring-purple-500 touch-manipulation"
              />
              <label htmlFor="isPublic" className="text-sm sm:text-base text-gray-300 cursor-pointer flex-1">
                {t('suggestion.public_setting')}
              </label>
            </div>

            {error && (
              <div className="p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm">
                {error}
              </div>
            )}

            <div className="flex flex-col sm:flex-row gap-3 sm:gap-4">
              <button
                type="button"
                onClick={() => navigate(`/suggestions/${id}`)}
                className="w-full sm:flex-1 bg-slate-600 hover:bg-slate-700 active:bg-slate-800 text-white font-semibold py-4 rounded-xl transition-all duration-200 min-h-[48px] touch-manipulation"
              >
                {t('suggestion.cancel')}
              </button>
              <button
                type="submit"
                className="w-full sm:flex-1 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 active:from-purple-800 active:to-pink-800 text-white font-semibold py-4 rounded-xl transition-all duration-200 transform hover:scale-105 active:scale-95 shadow-lg hover:shadow-purple-500/25 min-h-[48px] touch-manipulation"
              >
                ✏️ {t('suggestion.edit_complete')}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}