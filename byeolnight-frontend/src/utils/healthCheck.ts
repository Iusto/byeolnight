// 서버 헬스체크 유틸리티
export const checkServerHealth = async (): Promise<boolean> => {
  try {
    const response = await fetch('/api/public/posts/hot?size=1', {
      method: 'HEAD',
      signal: AbortSignal.timeout(3000),
      credentials: 'omit'
    });
    
    // 502, 503, 504는 서버 다운으로 간주
    if (response.status >= 502 && response.status <= 504) {
      return false;
    }
    
    return response.ok;
  } catch {
    return false;
  }
};

export const redirectToMaintenance = () => {
  console.log('[HealthCheck] 서버 다운 확인 -> maintenance 페이지로 이동');
  window.location.replace('/maintenance.html?t=' + Date.now());
};
