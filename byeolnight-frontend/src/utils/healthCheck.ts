// 서버 헬스체크 유틸리티

// maintenance.html 리다이렉트 비활성화 여부
// - 개발 환경(vite dev)에서는 기본적으로 비활성화: 백엔드가 꺼져 있어도 화면/디자인 확인 가능
// - 운영 빌드(vite build)에서는 정상 동작
// - VITE_DISABLE_MAINTENANCE 로 명시적 제어 가능 ('true'=강제 비활성화, 'false'=강제 활성화)
const maintenanceFlag = import.meta.env.VITE_DISABLE_MAINTENANCE;
const MAINTENANCE_DISABLED =
  maintenanceFlag === 'true' || (import.meta.env.DEV && maintenanceFlag !== 'false');

export const checkServerHealth = async (): Promise<boolean> => {
  // 비활성화 시 헬스체크를 건너뛰어 정상으로 간주 (AuthContext가 로딩에서 멈추지 않도록)
  if (MAINTENANCE_DISABLED) {
    return true;
  }

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
  if (MAINTENANCE_DISABLED) {
    console.warn('[HealthCheck] maintenance 리다이렉트 비활성화 상태 — 이동하지 않음');
    return;
  }

  console.log('[HealthCheck] 서버 다운 확인 -> maintenance 페이지로 이동');
  window.location.replace('/maintenance.html?t=' + Date.now());
};
