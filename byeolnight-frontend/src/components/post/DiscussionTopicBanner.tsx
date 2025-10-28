import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import axios from '../lib/axios';
import MarkdownRenderer from './MarkdownRenderer';

interface DiscussionTopic {
  id: number;
  title: string;
  content: string;
  writer: string;
  createdAt: string;
  likeCount: number;
  viewCount: number;
  commentCount: number;
}

export default function DiscussionTopicBanner() {
  const [todayTopic, setTodayTopic] = useState<DiscussionTopic | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const { t } = useTranslation();

  useEffect(() => {
    fetchTodayTopic();
  }, []);

  const fetchTodayTopic = async () => {
    try {
      const response = await axios.get('/public/discussions/today');
      setTodayTopic(response.data);
    } catch (error) {
      console.error('오늘의 토론 주제 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="bg-gradient-to-r from-purple-900/30 to-blue-900/30 backdrop-blur-md rounded-xl p-6 mb-8 animate-pulse">
        <div className="h-6 bg-purple-400/20 rounded mb-4"></div>
        <div className="h-4 bg-purple-400/20 rounded mb-2"></div>
        <div className="h-4 bg-purple-400/20 rounded w-3/4"></div>
      </div>
    );
  }

  if (!todayTopic) {
    return (
      <div className="bg-gradient-to-r from-gray-800/50 to-gray-700/50 backdrop-blur-md rounded-xl p-6 mb-8 border border-gray-600/30">
        <div className="text-center text-gray-400">
          <div className="text-2xl mb-2">🤖</div>
          <p>{t('home.today_topic_preparing')}</p>
          <div className="mt-3 p-3 bg-blue-900/20 rounded-lg border border-blue-500/20">
            <p className="text-blue-300 font-medium mb-1">🤖 {t('home.auto_topic_selection')}</p>
            <p className="text-sm text-blue-200">{t('home.discussion_auto_detail')}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="relative overflow-hidden bg-gradient-to-r from-purple-900/40 via-blue-900/40 to-indigo-900/40 backdrop-blur-md rounded-xl p-6 mb-8 border border-purple-500/20 shadow-2xl">
      {/* 배경 효과 */}
      <div className="absolute inset-0 bg-gradient-to-r from-purple-600/10 to-blue-600/10"></div>
      <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-purple-500 via-blue-500 to-indigo-500"></div>
      
      {/* 별빛 효과 */}
      <div className="absolute top-4 right-4 w-2 h-2 bg-yellow-300 rounded-full animate-pulse"></div>
      <div className="absolute top-8 right-12 w-1 h-1 bg-blue-300 rounded-full animate-pulse delay-300"></div>
      <div className="absolute top-6 right-20 w-1.5 h-1.5 bg-purple-300 rounded-full animate-pulse delay-700"></div>

      <div className="relative z-10">
        {/* 헤더 */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <span className="text-2xl">🔥</span>
              <span className="bg-gradient-to-r from-orange-400 to-red-400 bg-clip-text text-transparent font-bold text-lg">
                {t('home.today_discussion')}
              </span>
            </div>
            <div className="px-3 py-1 bg-purple-600/30 border border-purple-400/30 rounded-full text-xs text-purple-200">
              {t('home.ai_generated_topic')}
            </div>
          </div>
          <div className="text-xs text-gray-400">
            {new Date(todayTopic.createdAt).toLocaleDateString()}
          </div>
        </div>

        {/* 제목 */}
        <h2 
          className="text-2xl font-bold text-white mb-4 cursor-pointer hover:text-purple-200 transition-colors duration-200"
          onClick={() => navigate(`/posts/${todayTopic.id}`)}
        >
          {todayTopic.title}
        </h2>

        {/* 내용 */}
        <div className="text-gray-200 mb-6 leading-relaxed">
          <MarkdownRenderer content={todayTopic.content} />
        </div>

        {/* 통계 및 액션 버튼 */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-6 text-sm text-gray-300">
            <span className="flex items-center gap-1">
              <span>👁️</span>
              {todayTopic.viewCount}
            </span>
            <span className="flex items-center gap-1">
              <span>❤️</span>
              {todayTopic.likeCount}
            </span>
            <span className="flex items-center gap-1">
              <span>💬</span>
              {todayTopic.commentCount}
            </span>
          </div>

          <div className="flex gap-3">
            <button
              onClick={() => navigate(`/posts/${todayTopic.id}`)}
              className="px-4 py-2 bg-purple-600/80 hover:bg-purple-600 text-white rounded-lg transition-all duration-200 hover:scale-105 shadow-lg"
            >
              💬 {t('home.join_discussion')}
            </button>
            <button
              onClick={() => navigate(`/posts/new?originTopic=${todayTopic.id}`)}
              className="px-4 py-2 bg-blue-600/80 hover:bg-blue-600 text-white rounded-lg transition-all duration-200 hover:scale-105 shadow-lg"
            >
              ✍️ {t('home.write_opinion')}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}