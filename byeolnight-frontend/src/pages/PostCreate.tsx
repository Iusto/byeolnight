import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import { PostForm } from '../components/post';

interface FileDto {
  originalName: string;
  s3Key: string;
  url: string;
}

export default function PostCreate() {
  const navigate = useNavigate();
  const { user, refreshUserInfo } = useAuth();
  const [searchParams] = useSearchParams();
  const { t } = useTranslation();
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  // URL 파라미터에서 originTopic 추출
  const originTopicId = searchParams.get('originTopic');
  
  // URL 파라미터에서 고정 카테고리 설정
  const fixedCategory = searchParams.get('fixedCategory');
  const isFixedCategory = fixedCategory && ['DISCUSSION', 'IMAGE', 'REVIEW', 'FREE', 'NOTICE', 'NEWS', 'STARLIGHT_CINEMA'].includes(fixedCategory);
  const handleSubmit = async (data: {
    title: string;
    content: string;
    category: string;
    images: FileDto[];
  }) => {
    setError('');
    setIsSubmitting(true);

    try {
      if (!user) {
        setError(t('home.login_required'));
        return;
      }
      
      const response = await axios.post('/member/posts', {
        title: data.title,
        content: data.content,
        category: data.category,
        images: data.images,
        originTopicId: originTopicId ? parseInt(originTopicId) : null
      });
      
      console.log('게시글 작성 완료:', response.data);
      
      // 공지글인 경우 알림 생성 확인
      if (data.category === 'NOTICE') {
        console.log('공지글 작성 완료 - 알림 생성 대기 중...');
        setTimeout(async () => {
          try {
            const notificationResponse = await axios.get('/member/notifications/unread/count');
            console.log('공지글 작성 후 알림 개수:', notificationResponse.data);
          } catch (err) {
            console.error('알림 확인 실패:', err);
          }
        }, 3000);
      }
      
      // 사용자 정보 새로고침 (포인트 업데이트)
      await refreshUserInfo();
      
      // 해당 카테고리 게시판으로 이동
      navigate(`/posts?category=${data.category}&sort=recent`);
    } catch (err: any) {
      const msg = err?.response?.data?.message || '게시글 작성 실패';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  // 로그인 검증
  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center text-white">
        <div className="text-center">
          <p className="text-lg mb-4">{t('home.login_required_desc')}</p>
          <button 
            onClick={() => navigate('/login')}
            className="bg-purple-600 hover:bg-purple-700 px-4 py-2 rounded"
          >
            {t('home.go_to_login')}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen min-h-screen-safe bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 sm:from-slate-900 sm:via-purple-900 sm:to-slate-900 mobile-bright text-white mobile-optimized">

      {/* 헤더 섹션 - 모바일 최적화 */}
      <div className="relative overflow-hidden bg-gradient-to-r from-purple-900/50 to-pink-900/50 border-b border-purple-500/20">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/10 to-pink-600/10"></div>
        <div className="relative max-w-4xl mx-auto px-3 sm:px-6 py-4 sm:py-16 mobile-header">
          <div className="text-center">
            <div className="w-12 h-12 sm:w-16 sm:h-16 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-2xl sm:text-3xl mx-auto mb-3 sm:mb-6 shadow-lg">
              📝
            </div>
            <h1 className="text-xl sm:text-4xl md:text-5xl font-bold bg-gradient-to-r from-white via-purple-200 to-white bg-clip-text text-transparent mb-3 sm:mb-4 mobile-text mobile-title px-2">
              {t('home.post_create')}
            </h1>
            {originTopicId && (
              <div className="inline-flex items-center gap-2 sm:gap-3 px-3 py-2 sm:px-6 sm:py-3 bg-blue-600/20 border border-blue-400/30 rounded-full text-blue-200 backdrop-blur-sm text-sm sm:text-base mobile-card-compact">
                <span className="text-blue-400">💬</span>
                <span className="mobile-text">오늘의 토론 주제에 대한 의견글을 작성 중입니다</span>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-3 sm:px-6 py-3 sm:py-8 mobile-optimized">
        <div className="mobile-section bg-gradient-to-br from-slate-800/50 to-purple-900/30 backdrop-blur-md rounded-2xl p-3 sm:p-8 border border-purple-500/20 shadow-2xl mobile-card-compact">
          <PostForm
            initialCategory={isFixedCategory ? fixedCategory : 'DISCUSSION'}
            isFixedCategory={isFixedCategory}
            fixedCategory={fixedCategory}
            onSubmit={handleSubmit}
            submitButtonText={`🚀 ${t('home.submit_post')}`}
            isSubmitting={isSubmitting}
            error={error}
          />
        </div>
      </div>
    </div>
  );
}