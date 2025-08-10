import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';
import { createSuggestion } from '../lib/api/suggestion';
import type { SuggestionCategory } from '../types/suggestion';

export default function SuggestionCreate() {
  const { user } = useAuth();
  const { t } = useTranslation();
  const navigate = useNavigate();
  
  const getCategories = () => ({
    FEATURE: t('suggestion.categories.FEATURE'),
    BUG: t('suggestion.categories.BUG'),
    UI_UX: t('suggestion.categories.UI_UX'),
    CONTENT: t('suggestion.categories.CONTENT'),
    OTHER: t('suggestion.categories.OTHER')
  });
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    content: '',
    category: 'FEATURE' as SuggestionCategory,
    isPublic: true
  });

  // 로그인하지 않은 사용자는 리다이렉트
  if (!user) {
    navigate('/login');
    return null;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.title.trim() || !formData.content.trim()) {
      alert(t('suggestion.validation_error'));
      return;
    }

    setLoading(true);
    
    try {
      await createSuggestion(formData);
      alert(t('suggestion.submit_success'));
      navigate('/suggestions');
      
    } catch (error: any) {
      console.error('건의사항 제출 실패:', error);
      const errorMessage = error.response?.data?.message || t('suggestion.submit_failed');
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
          <h1 className="text-3xl font-bold text-white mb-2">{t('suggestion.create_title')}</h1>
          <p className="text-gray-300">{t('suggestion.create_subtitle')}</p>
        </div>

        {/* 작성 폼 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-sm rounded-xl p-8 border border-purple-500/20">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* 카테고리 선택 */}
            <div>
              <label className="block text-white font-medium mb-3">{t('suggestion.category')}</label>
              <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                {Object.entries(getCategories()).map(([key, label]) => (
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
                {t('suggestion.title_label')} <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                id="title"
                value={formData.title}
                onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
                placeholder={t('suggestion.title_placeholder')}
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
                {t('suggestion.content_label')} <span className="text-red-400">*</span>
              </label>
              <textarea
                id="content"
                value={formData.content}
                onChange={(e) => setFormData(prev => ({ ...prev, content: e.target.value }))}
                placeholder={t('suggestion.content_placeholder')}
                className="w-full px-4 py-3 bg-[#2a2e45]/60 border border-purple-500/30 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-400/20 transition-all resize-none"
                rows={12}
                maxLength={2000}
                required
              />
              <div className="text-right text-sm text-gray-400 mt-1">
                {formData.content.length}/2000
              </div>
            </div>

            {/* 공개/비공개 설정 */}
            <div className="flex items-center gap-3">
              <input
                type="checkbox"
                id="isPublic"
                checked={formData.isPublic}
                onChange={(e) => setFormData(prev => ({ ...prev, isPublic: e.target.checked }))}
                className="w-4 h-4 text-purple-600 bg-[#2a2e45]/60 border border-purple-500/30 rounded focus:ring-purple-400 focus:ring-2"
              />
              <label htmlFor="isPublic" className="text-white text-sm">
                {t('suggestion.public_setting')}
              </label>
            </div>

            {/* 안내 메시지 */}
            <div className="bg-blue-500/10 border border-blue-500/30 rounded-lg p-4">
              <div className="flex items-start gap-3">
                <span className="text-blue-400 text-xl">💡</span>
                <div className="text-blue-300 text-sm">
                  <p className="font-medium mb-2">{t('suggestion.guide_title')}</p>
                  <ul className="space-y-1 text-blue-200">
                    {t('suggestion.guide_items', { returnObjects: true }).map((item: string, index: number) => (
                      <li key={index}>• {item}</li>
                    ))}
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
                {t('suggestion.cancel')}
              </button>
              <button
                type="submit"
                disabled={loading || !formData.title.trim() || !formData.content.trim()}
                className="flex-1 px-6 py-3 bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 disabled:from-gray-600 disabled:to-gray-600 text-white font-medium rounded-lg shadow-lg hover:shadow-xl transition-all duration-200 transform hover:scale-105 disabled:scale-100 disabled:cursor-not-allowed"
              >
                {loading ? t('suggestion.submitting') : t('suggestion.submit')}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}