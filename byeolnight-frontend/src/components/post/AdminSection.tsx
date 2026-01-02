import axios from '../../lib/axios';

interface AdminSectionProps {
  category: string;
}

export default function AdminSection({ category }: AdminSectionProps) {
  if (category === 'DISCUSSION') {
    return (
      <div className="mb-6 p-4 bg-blue-900/30 rounded-lg border border-blue-600/30">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-blue-200 mb-2">🤖 관리자 토론 주제 관리</h3>
            <p className="text-blue-200 text-sm">
              매일 오전 8시 자동 생성 | 스케줄 실패 시 수동 생성 가능
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={async () => {
                try {
                  const response = await axios.get('/admin/discussions/status');
                  const data = response.data?.data || response.data;
                  
                  if (typeof data === 'object' && data !== null) {
                    const { totalDiscussionPosts, lastUpdated, todayTopicExists, todayTopicTitle } = data;
                    const isHealthy = todayTopicExists || totalDiscussionPosts > 0;
                    const statusIcon = isHealthy ? '✅' : '⚠️';
                    const statusText = isHealthy ? '정상 작동 중' : '주의 필요';
                    
                    let warningMessage = '';
                    if (!todayTopicExists) {
                      warningMessage = '\n⚠️ 오늘의 토론 주제가 생성되지 않았습니다. 수동 생성을 고려해주세요.';
                    }
                    
                    const statusMessage = `📊 토론 게시판 상태\n\n` +
                      `• 총 토론 게시글: ${totalDiscussionPosts || 0}개\n` +
                      `• 오늘의 토론 주제: ${todayTopicExists ? '생성됨' : '없음'}\n` +
                      `${todayTopicTitle ? `• 주제: "${todayTopicTitle}"\n` : ''}` +
                      `• 마지막 업데이트: ${lastUpdated ? new Date(lastUpdated).toLocaleString('ko-KR') : '정보 없음'}\n\n` +
                      `${statusIcon} 토론 시스템 ${statusText}${warningMessage}`;
                    alert(statusMessage);
                  } else {
                    alert('⚠️ 토론 시스템 상태를 확인할 수 없습니다.');
                  }
                } catch (error) {
                  console.error('상태 확인 실패:', error);
                  alert('상태 확인에 실패했습니다.');
                }
              }}
              className="bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg transition-colors"
            >
              📊 상태 확인
            </button>
            <button
              onClick={async () => {
                if (!confirm('새로운 토론 주제를 생성하시겠습니까? 기존 주제는 비활성화됩니다.')) return;
                
                try {
                  await axios.post('/admin/discussions/generate-topic');
                  alert('토론 주제가 성공적으로 생성되었습니다!');
                  window.location.reload();
                } catch (error) {
                  console.error('토론 주제 생성 실패:', error);
                  alert('토론 주제 생성에 실패했습니다.');
                }
              }}
              className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors"
            >
              🎯 토론 주제 생성
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (category === 'STARLIGHT_CINEMA') {
    return (
      <div className="mb-6 p-4 bg-purple-900/30 rounded-lg border border-purple-600/30">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-purple-200 mb-2">🤖 관리자 시네마 관리</h3>
            <p className="text-purple-200 text-sm">
              매일 오후 8시 자동 생성 | 스케줄 실패 시 수동 생성 가능
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={async () => {
                try {
                  const response = await axios.get('/admin/cinema/status');
                  const data = response.data?.data || response.data;
                  
                  if (typeof data === 'object' && data !== null) {
                    const { totalCinemaPosts, lastUpdated, latestPostExists, latestPostTitle } = data;
                    const lastUpdateTime = lastUpdated ? new Date(lastUpdated) : null;
                    const daysSinceUpdate = lastUpdateTime ? Math.floor((new Date().getTime() - lastUpdateTime.getTime()) / (1000 * 60 * 60 * 24)) : 999;
                    
                    const isHealthy = latestPostExists && totalCinemaPosts > 0 && daysSinceUpdate < 2;
                    const statusIcon = isHealthy ? '✅' : '⚠️';
                    const statusText = isHealthy ? '정상 작동 중' : '주의 필요';
                    
                    let warningMessage = '';
                    if (!latestPostExists) {
                      warningMessage = '\n⚠️ 최신 시네마 포스트가 없습니다. 수동 생성을 고려해주세요.';
                    } else if (daysSinceUpdate >= 2) {
                      warningMessage = `\n⚠️ 마지막 업데이트가 ${daysSinceUpdate}일 전입니다. 스케줄러 확인이 필요합니다.`;
                    }
                    
                    const statusMessage = `🎬 별빛 시네마 상태\n\n` +
                      `• 총 시네마 게시글: ${totalCinemaPosts || 0}개\n` +
                      `• 최신 포스트: ${latestPostExists ? '있음' : '없음'}\n` +
                      `${latestPostTitle ? `• 제목: "${latestPostTitle}"\n` : ''}` +
                      `• 마지막 업데이트: ${lastUpdated ? new Date(lastUpdated).toLocaleString('ko-KR') : '정보 없음'}\n\n` +
                      `${statusIcon} 별빛 시네마 시스템 ${statusText}${warningMessage}`;
                    alert(statusMessage);
                  } else {
                    alert('⚠️ 별빛 시네마 시스템 상태를 확인할 수 없습니다.');
                  }
                } catch (error) {
                  console.error('상태 확인 실패:', error);
                  alert('상태 확인에 실패했습니다.');
                }
              }}
              className="bg-pink-600 hover:bg-pink-700 text-white px-4 py-2 rounded-lg transition-colors"
            >
              📊 상태 확인
            </button>
            <button
              onClick={async () => {
                if (!confirm('새로운 별빛 시네마 포스트를 생성하시겠습니까?')) return;
                
                try {
                  await axios.post('/admin/cinema/generate-post');
                  alert('별빛 시네마 포스트가 성공적으로 생성되었습니다!');
                  window.location.reload();
                } catch (error) {
                  console.error('별빛 시네마 생성 실패:', error);
                  alert('별빛 시네마 생성에 실패했습니다.');
                }
              }}
              className="bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg transition-colors"
            >
              🎬 시네마 포스트 생성
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (category === 'NEWS') {
    return (
      <div className="mb-6 p-4 bg-green-900/30 rounded-lg border border-green-600/30">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-green-200 mb-2">🤖 관리자 뉴스 관리</h3>
            <p className="text-green-200 text-sm">
              매일 오전 8시 자동 수집 | 스케줄 실패 시 수동 수집 가능
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={async () => {
                try {
                  const response = await axios.get('/admin/crawler/status');
                  alert(response.data.data || '뉴스 크롤러 시스템이 정상 작동 중입니다.');
                } catch (error) {
                  console.error('상태 확인 실패:', error);
                  alert('상태 확인에 실패했습니다.');
                }
              }}
              className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors"
            >
              📊 상태 확인
            </button>
            <button
              onClick={async () => {
                if (!confirm('NewsData.io API를 통해 최신 우주 뉴스를 수집하시겠습니까?')) return;
                
                try {
                  await axios.post('/admin/crawler/start');
                  alert('뉴스 수집이 성공적으로 완료되었습니다!');
                  window.location.reload();
                } catch (error) {
                  console.error('뉴스 수집 실패:', error);
                  alert('뉴스 수집에 실패했습니다.');
                }
              }}
              className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg transition-colors"
            >
              🚀 뉴스 수집
            </button>
          </div>
        </div>
      </div>
    );
  }

  return null;
}
