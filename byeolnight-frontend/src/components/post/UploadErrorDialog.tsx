import { useState, useEffect } from 'react';
import { diagnoseCompatibility, BrowserInfo } from '../../utils/browserCompatibility';

interface UploadErrorDialogProps {
  isOpen: boolean;
  onClose: () => void;
  error: string;
  onRetry?: () => void;
}

interface DiagnosisResult {
  browserInfo: BrowserInfo;
  networkStatus: {
    isOnline: boolean;
    latency?: number;
    error?: string;
  };
  corsSupported: boolean;
  fetchSupported: boolean;
  overallStatus: 'good' | 'warning' | 'error';
  summary: string;
}

export default function UploadErrorDialog({ isOpen, onClose, error, onRetry }: UploadErrorDialogProps) {
  const [diagnosis, setDiagnosis] = useState<DiagnosisResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showTechnicalDetails, setShowTechnicalDetails] = useState(false);

  useEffect(() => {
    if (isOpen && !diagnosis) {
      performDiagnosis();
    }
  }, [isOpen]);

  const performDiagnosis = async () => {
    setIsLoading(true);
    try {
      const result = await diagnoseCompatibility();
      setDiagnosis(result);
    } catch (err) {
      console.error('진단 실패:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'good': return '✅';
      case 'warning': return '⚠️';
      case 'error': return '❌';
      default: return '❓';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'good': return 'text-green-400';
      case 'warning': return 'text-yellow-400';
      case 'error': return 'text-red-400';
      default: return 'text-gray-400';
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-slate-800 rounded-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* 헤더 */}
        <div className="p-6 border-b border-slate-700">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              🚨 이미지 업로드 실패
            </h2>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-white transition-colors"
            >
              ✕
            </button>
          </div>
        </div>

        {/* 내용 */}
        <div className="p-6 space-y-6">
          {/* 오류 메시지 */}
          <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-4">
            <h3 className="text-red-400 font-medium mb-2">발생한 오류:</h3>
            <p className="text-red-300 text-sm whitespace-pre-line">{error}</p>
          </div>

          {/* 진단 결과 */}
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin w-8 h-8 border-2 border-blue-400 border-t-transparent rounded-full"></div>
              <span className="ml-3 text-gray-300">시스템 진단 중...</span>
            </div>
          ) : diagnosis ? (
            <div className="space-y-4">
              <h3 className="text-white font-medium">🔍 시스템 진단 결과:</h3>
              
              {/* 전체 상태 */}
              <div className={`p-4 rounded-lg border ${
                diagnosis.overallStatus === 'good' ? 'bg-green-500/10 border-green-500/20' :
                diagnosis.overallStatus === 'warning' ? 'bg-yellow-500/10 border-yellow-500/20' :
                'bg-red-500/10 border-red-500/20'
              }`}>
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-xl">{getStatusIcon(diagnosis.overallStatus)}</span>
                  <span className={`font-medium ${getStatusColor(diagnosis.overallStatus)}`}>
                    전체 상태: {diagnosis.overallStatus === 'good' ? '양호' : 
                              diagnosis.overallStatus === 'warning' ? '주의' : '문제'}
                  </span>
                </div>
                <p className="text-gray-300 text-sm">{diagnosis.summary}</p>
              </div>

              {/* 상세 진단 */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* 브라우저 정보 */}
                <div className="bg-slate-700/50 rounded-lg p-4">
                  <h4 className="text-white font-medium mb-2 flex items-center gap-2">
                    🌐 브라우저
                  </h4>
                  <div className="space-y-1 text-sm">
                    <p className="text-gray-300">
                      {diagnosis.browserInfo.name} {diagnosis.browserInfo.version}
                    </p>
                    <p className={diagnosis.browserInfo.isSupported ? 'text-green-400' : 'text-red-400'}>
                      {diagnosis.browserInfo.isSupported ? '✅ 지원됨' : '❌ 지원되지 않음'}
                    </p>
                  </div>
                </div>

                {/* 네트워크 상태 */}
                <div className="bg-slate-700/50 rounded-lg p-4">
                  <h4 className="text-white font-medium mb-2 flex items-center gap-2">
                    📡 네트워크
                  </h4>
                  <div className="space-y-1 text-sm">
                    <p className={diagnosis.networkStatus.isOnline ? 'text-green-400' : 'text-red-400'}>
                      {diagnosis.networkStatus.isOnline ? '✅ 연결됨' : '❌ 연결 안됨'}
                    </p>
                    {diagnosis.networkStatus.latency && (
                      <p className="text-gray-300">
                        지연시간: {diagnosis.networkStatus.latency}ms
                      </p>
                    )}
                    {diagnosis.networkStatus.error && (
                      <p className="text-red-400 text-xs">{diagnosis.networkStatus.error}</p>
                    )}
                  </div>
                </div>
              </div>

              {/* 브라우저 이슈 및 권장사항 */}
              {diagnosis.browserInfo.issues.length > 0 && (
                <div className="bg-yellow-500/10 border border-yellow-500/20 rounded-lg p-4">
                  <h4 className="text-yellow-400 font-medium mb-2">⚠️ 감지된 문제:</h4>
                  <ul className="text-yellow-300 text-sm space-y-1">
                    {diagnosis.browserInfo.issues.map((issue, index) => (
                      <li key={index}>• {issue}</li>
                    ))}
                  </ul>
                </div>
              )}

              {diagnosis.browserInfo.recommendations.length > 0 && (
                <div className="bg-blue-500/10 border border-blue-500/20 rounded-lg p-4">
                  <h4 className="text-blue-400 font-medium mb-2">💡 권장 해결책:</h4>
                  <ul className="text-blue-300 text-sm space-y-1">
                    {diagnosis.browserInfo.recommendations.map((rec, index) => (
                      <li key={index}>• {rec}</li>
                    ))}
                  </ul>
                </div>
              )}

              {/* 기술적 세부사항 */}
              <div className="border-t border-slate-700 pt-4">
                <button
                  onClick={() => setShowTechnicalDetails(!showTechnicalDetails)}
                  className="text-gray-400 hover:text-white text-sm flex items-center gap-2 transition-colors"
                >
                  {showTechnicalDetails ? '▼' : '▶'} 기술적 세부사항
                </button>
                
                {showTechnicalDetails && (
                  <div className="mt-3 bg-slate-900/50 rounded p-3 text-xs text-gray-400 font-mono">
                    <p>브라우저: {diagnosis.browserInfo.name} {diagnosis.browserInfo.version}</p>
                    <p>CORS 지원: {diagnosis.corsSupported ? '예' : '아니오'}</p>
                    <p>Fetch API 지원: {diagnosis.fetchSupported ? '예' : '아니오'}</p>
                    <p>User-Agent: {navigator.userAgent}</p>
                    <p>온라인 상태: {navigator.onLine ? '예' : '아니오'}</p>
                    <p>연결 타입: {(navigator as any).connection?.effectiveType || '알 수 없음'}</p>
                  </div>
                )}
              </div>
            </div>
          ) : null}
        </div>

        {/* 액션 버튼 */}
        <div className="p-6 border-t border-slate-700 flex gap-3 justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-400 hover:text-white transition-colors"
          >
            닫기
          </button>
          {onRetry && (
            <button
              onClick={() => {
                onRetry();
                onClose();
              }}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
            >
              다시 시도
            </button>
          )}
          <button
            onClick={() => window.open('https://www.google.com/chrome/', '_blank')}
            className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg transition-colors"
          >
            Chrome 다운로드
          </button>
        </div>
      </div>
    </div>
  );
}