/**
 * 브라우저 호환성 체크 유틸리티
 * 특정 브라우저에서 발생하는 "Failed to fetch" 문제를 진단하고 해결책을 제공
 */

export interface BrowserInfo {
  name: string;
  version: string;
  isSupported: boolean;
  issues: string[];
  recommendations: string[];
}

/**
 * 현재 브라우저 정보를 분석하고 호환성을 체크
 */
export const getBrowserInfo = (): BrowserInfo => {
  const userAgent = navigator.userAgent;
  const issues: string[] = [];
  const recommendations: string[] = [];
  
  let browserName = 'Unknown';
  let browserVersion = 'Unknown';
  let isSupported = true;

  // Chrome 계열
  if (userAgent.includes('Chrome') && !userAgent.includes('Edg')) {
    browserName = 'Chrome';
    const match = userAgent.match(/Chrome\/(\d+)/);
    browserVersion = match ? match[1] : 'Unknown';
    
    const version = parseInt(browserVersion);
    if (version < 80) {
      isSupported = false;
      issues.push('Chrome 버전이 너무 낮습니다');
      recommendations.push('Chrome을 최신 버전으로 업데이트하세요');
    }
  }
  // Firefox
  else if (userAgent.includes('Firefox')) {
    browserName = 'Firefox';
    const match = userAgent.match(/Firefox\/(\d+)/);
    browserVersion = match ? match[1] : 'Unknown';
    
    const version = parseInt(browserVersion);
    if (version < 75) {
      isSupported = false;
      issues.push('Firefox 버전이 너무 낮습니다');
      recommendations.push('Firefox를 최신 버전으로 업데이트하세요');
    }
  }
  // Safari
  else if (userAgent.includes('Safari') && !userAgent.includes('Chrome')) {
    browserName = 'Safari';
    const match = userAgent.match(/Version\/(\d+)/);
    browserVersion = match ? match[1] : 'Unknown';
    
    const version = parseInt(browserVersion);
    if (version < 13) {
      isSupported = false;
      issues.push('Safari 버전이 너무 낮습니다');
      recommendations.push('Safari를 최신 버전으로 업데이트하세요');
    }
    
    // Safari의 알려진 CORS 이슈
    issues.push('Safari에서 CORS 관련 문제가 발생할 수 있습니다');
    recommendations.push('시크릿 모드를 시도해보세요');
    recommendations.push('Safari 설정에서 "웹사이트 간 추적 방지" 기능을 일시적으로 비활성화해보세요');
  }
  // Edge
  else if (userAgent.includes('Edg')) {
    browserName = 'Edge';
    const match = userAgent.match(/Edg\/(\d+)/);
    browserVersion = match ? match[1] : 'Unknown';
    
    const version = parseInt(browserVersion);
    if (version < 80) {
      isSupported = false;
      issues.push('Edge 버전이 너무 낮습니다');
      recommendations.push('Edge를 최신 버전으로 업데이트하세요');
    }
  }
  // Internet Explorer (지원하지 않음)
  else if (userAgent.includes('MSIE') || userAgent.includes('Trident')) {
    browserName = 'Internet Explorer';
    isSupported = false;
    issues.push('Internet Explorer는 지원되지 않습니다');
    recommendations.push('Chrome, Firefox, Edge, Safari 등 최신 브라우저를 사용하세요');
  }

  // 인앱 브라우저 감지
  if (userAgent.includes('wv') || userAgent.includes('WebView')) {
    issues.push('인앱 브라우저에서 일부 기능이 제한될 수 있습니다');
    recommendations.push('기본 브라우저에서 열어보세요');
  }

  // 모바일 브라우저 특별 처리
  if (/Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(userAgent)) {
    if (userAgent.includes('SamsungBrowser')) {
      browserName = 'Samsung Internet';
      issues.push('Samsung Internet에서 CORS 문제가 발생할 수 있습니다');
      recommendations.push('Chrome 모바일 브라우저를 사용해보세요');
    }
    
    recommendations.push('모바일에서는 이미지 붙여넣기 대신 파일 선택을 사용하세요');
  }

  return {
    name: browserName,
    version: browserVersion,
    isSupported,
    issues,
    recommendations
  };
};

/**
 * 네트워크 연결 상태 체크
 */
export const checkNetworkConnection = async (): Promise<{
  isOnline: boolean;
  latency?: number;
  error?: string;
}> => {
  if (!navigator.onLine) {
    return { isOnline: false, error: '인터넷 연결이 끊어졌습니다' };
  }

  try {
    const startTime = Date.now();
    const response = await fetch('/actuator/health', {
      method: 'HEAD',
      cache: 'no-cache',
      signal: AbortSignal.timeout(5000)
    });
    const latency = Date.now() - startTime;

    return {
      isOnline: response.ok,
      latency,
      error: response.ok ? undefined : `서버 응답 오류: ${response.status}`
    };
  } catch (error: any) {
    return {
      isOnline: false,
      error: error.name === 'AbortError' ? '네트워크 응답 시간 초과' : error.message
    };
  }
};

/**
 * CORS 지원 여부 체크
 */
export const checkCorsSupport = (): boolean => {
  return 'withCredentials' in new XMLHttpRequest();
};

/**
 * Fetch API 지원 여부 체크
 */
export const checkFetchSupport = (): boolean => {
  return typeof fetch !== 'undefined';
};

/**
 * 브라우저 호환성 종합 진단
 */
export const diagnoseCompatibility = async (): Promise<{
  browserInfo: BrowserInfo;
  networkStatus: Awaited<ReturnType<typeof checkNetworkConnection>>;
  corsSupported: boolean;
  fetchSupported: boolean;
  overallStatus: 'good' | 'warning' | 'error';
  summary: string;
}> => {
  const browserInfo = getBrowserInfo();
  const networkStatus = await checkNetworkConnection();
  const corsSupported = checkCorsSupport();
  const fetchSupported = checkFetchSupport();

  let overallStatus: 'good' | 'warning' | 'error' = 'good';
  let summary = '모든 기능이 정상적으로 작동합니다.';

  if (!browserInfo.isSupported || !corsSupported || !fetchSupported) {
    overallStatus = 'error';
    summary = '브라우저가 일부 기능을 지원하지 않습니다. 브라우저를 업데이트하거나 다른 브라우저를 사용하세요.';
  } else if (!networkStatus.isOnline || browserInfo.issues.length > 0) {
    overallStatus = 'warning';
    summary = '일부 기능에 제한이 있을 수 있습니다. 네트워크 연결과 브라우저 설정을 확인하세요.';
  }

  return {
    browserInfo,
    networkStatus,
    corsSupported,
    fetchSupported,
    overallStatus,
    summary
  };
};

/**
 * 이미지 업로드 실패 시 진단 및 해결책 제공
 */
export const diagnoseUploadFailure = async (error: any): Promise<{
  diagnosis: string;
  solutions: string[];
  technicalDetails: string;
}> => {
  const browserInfo = getBrowserInfo();
  const networkStatus = await checkNetworkConnection();
  
  let diagnosis = '이미지 업로드 중 오류가 발생했습니다.';
  const solutions: string[] = [];
  let technicalDetails = `오류: ${error.message || error}`;

  // 네트워크 문제
  if (!networkStatus.isOnline) {
    diagnosis = '네트워크 연결 문제로 인해 업로드에 실패했습니다.';
    solutions.push('인터넷 연결을 확인하세요');
    solutions.push('Wi-Fi 또는 모바일 데이터 연결을 다시 시도하세요');
  }
  // CORS 문제
  else if (error.message?.includes('CORS') || error.message === 'Failed to fetch') {
    diagnosis = '브라우저 보안 정책으로 인해 업로드가 차단되었습니다.';
    solutions.push('다른 브라우저를 사용해보세요 (Chrome, Firefox 권장)');
    solutions.push('시크릿/프라이빗 모드를 사용해보세요');
    solutions.push('브라우저 확장 프로그램을 일시적으로 비활성화해보세요');
    
    if (browserInfo.name === 'Safari') {
      solutions.push('Safari 설정 → 개인정보 보호 → "웹사이트 간 추적 방지" 비활성화');
    }
  }
  // 타임아웃 문제
  else if (error.message?.includes('timeout') || error.name === 'AbortError') {
    diagnosis = '네트워크 응답 시간이 초과되었습니다.';
    solutions.push('이미지 파일 크기를 줄여보세요 (5MB 이하 권장)');
    solutions.push('네트워크 연결 상태를 확인하세요');
    solutions.push('잠시 후 다시 시도하세요');
  }
  // 파일 크기 문제
  else if (error.message?.includes('크기') || error.message?.includes('size')) {
    diagnosis = '파일 크기가 너무 큽니다.';
    solutions.push('이미지를 압축하거나 크기를 줄여주세요');
    solutions.push('10MB 이하의 이미지를 사용하세요');
  }
  // 브라우저 호환성 문제
  else if (!browserInfo.isSupported) {
    diagnosis = '현재 브라우저가 일부 기능을 지원하지 않습니다.';
    solutions.push('브라우저를 최신 버전으로 업데이트하세요');
    solutions.push('Chrome, Firefox, Edge 등 최신 브라우저를 사용하세요');
  }
  // 일반적인 해결책
  else {
    solutions.push('페이지를 새로고침하고 다시 시도하세요');
    solutions.push('다른 이미지 파일을 사용해보세요');
    solutions.push('브라우저 캐시를 삭제하고 다시 시도하세요');
  }

  // 기술적 세부사항 추가
  technicalDetails += `\n브라우저: ${browserInfo.name} ${browserInfo.version}`;
  technicalDetails += `\n네트워크: ${networkStatus.isOnline ? '연결됨' : '연결 안됨'}`;
  if (networkStatus.latency) {
    technicalDetails += ` (지연시간: ${networkStatus.latency}ms)`;
  }
  technicalDetails += `\nUser-Agent: ${navigator.userAgent}`;

  return {
    diagnosis,
    solutions,
    technicalDetails
  };
};