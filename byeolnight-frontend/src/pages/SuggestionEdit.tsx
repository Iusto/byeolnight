import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import axios from '../lib/axios';

export default function SuggestionEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('FEATURE');
  const [isPublic, setIsPublic] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const CATEGORIES = {
    FEATURE: '기능 개선',
    BUG: '버그 신고',
    UI_UX: 'UI/UX 개선',
    CONTENT: '콘텐츠 관련',
    OTHER: '기타'
  } as const;

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }
    fetchSuggestion();
  }, [id, user]);

  const fetchSuggestion = async () => {
    try {
      const response = await axios.get(`/suggestions/${id}`);
      console.log('건의사항 조회 응답:', response.data);
      const suggestion = response.data.data;
      
      console.log('건의사항 데이터:', suggestion);
      console.log('현재 사용자:', user);
      console.log('작성자 비교:', suggestion.authorNickname, 'vs', user?.nickname);
      
      // 작성자만 수정 가능
      if (suggestion.authorNickname !== user?.nickname) {
        setError('수정 권한이 없습니다.');
        return;
      }
      
      setTitle(suggestion.title);
      setContent(suggestion.content);
      setCategory(suggestion.category);
      setIsPublic(suggestion.isPublic);
    } catch (error) {
      console.error('건의사항 조회 실패:', error);
      setError('건의사항을 불러올 수 없습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!title.trim() || !content.trim()) {
      setError('제목과 내용을 모두 입력해주세요.');
      return;
    }

    try {
      await axios.put(`/suggestions/${id}`, {
        title: title.trim(),
        content: content.trim(),
        category,
        isPublic
      });
      
      navigate(`/suggestions/${id}`);
    } catch (error: any) {
      const msg = error?.response?.data?.message || '수정에 실패했습니다.';
      setError(msg);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 flex items-center justify-center">
        <div className="text-white text-lg">로딩 중...</div>
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
            목록으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white">
      <div className="relative overflow-hidden bg-gradient-to-r from-purple-900/50 to-pink-900/50 border-b border-purple-500/20">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/10 to-pink-600/10"></div>
        <div className="relative max-w-4xl mx-auto px-6 py-16">
          <div className="text-center">
            <div className="w-16 h-16 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-3xl mx-auto mb-6 shadow-lg">
              ✏️
            </div>
            <h1 className="text-4xl md:text-5xl font-bold bg-gradient-to-r from-white via-purple-200 to-white bg-clip-text text-transparent mb-4">
              건의사항 수정
            </h1>
            <p className="text-xl text-gray-300">소중한 의견을 수정해주세요</p>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-6 py-8">
        <div className="bg-gradient-to-br from-slate-800/50 to-purple-900/30 backdrop-blur-md rounded-2xl p-8 border border-purple-500/20 shadow-2xl">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* 카테고리 선택 */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">카테고리</label>
              <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                {Object.entries(CATEGORIES).map(([key, label]) => (
                  <button
                    key={key}
                    type="button"
                    onClick={() => setCategory(key)}
                    className={`p-3 rounded-lg border-2 transition-all text-center ${
                      category === key
                        ? 'border-purple-500 bg-purple-500/20 text-purple-300'
                        : 'border-gray-600 bg-gray-600/10 text-gray-300 hover:border-gray-500 hover:bg-gray-600/20'
                    }`}
                  >
                    <div className="text-sm font-medium">{label}</div>
                  </button>
                ))}
              </div>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">제목</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="제목을 입력하세요..."
                required
                className="w-full px-4 py-3 rounded-xl bg-slate-700/50 text-white border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent placeholder-gray-400 transition-all duration-200"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">내용</label>
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder="건의사항을 자세히 작성해주세요..."
                required
                rows={10}
                className="w-full px-4 py-3 rounded-xl bg-slate-700/50 text-white border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent placeholder-gray-400 transition-all duration-200 resize-none"
              />
            </div>

            <div className="flex items-center gap-3">
              <input
                type="checkbox"
                id="isPublic"
                checked={isPublic}
                onChange={(e) => setIsPublic(e.target.checked)}
                className="w-4 h-4 text-purple-600 bg-slate-700 border-slate-600 rounded focus:ring-purple-500"
              />
              <label htmlFor="isPublic" className="text-sm text-gray-300">
                공개 건의사항 (체크 해제 시 관리자만 볼 수 있습니다)
              </label>
            </div>

            {error && (
              <div className="p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm">
                {error}
              </div>
            )}

            <div className="flex gap-4">
              <button
                type="button"
                onClick={() => navigate(`/suggestions/${id}`)}
                className="flex-1 bg-slate-600 hover:bg-slate-700 text-white font-semibold py-4 rounded-xl transition-all duration-200"
              >
                취소
              </button>
              <button
                type="submit"
                className="flex-1 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 text-white font-semibold py-4 rounded-xl transition-all duration-200 transform hover:scale-105 shadow-lg hover:shadow-purple-500/25"
              >
                ✏️ 수정 완료
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}