import React from 'react';
import { useTranslation } from 'react-i18next';
import { useIssObservation } from '../../hooks/useWeatherData';

interface IssTrackerProps {
  latitude: number;
  longitude: number;
}

const directionMap: Record<string, { arrow: string; label: string }> = {
  NORTH:     { arrow: '↑', label: '북쪽' },
  NORTHEAST: { arrow: '↗', label: '북동쪽' },
  EAST:      { arrow: '→', label: '동쪽' },
  SOUTHEAST: { arrow: '↘', label: '남동쪽' },
  SOUTH:     { arrow: '↓', label: '남쪽' },
  SOUTHWEST: { arrow: '↙', label: '남서쪽' },
  WEST:      { arrow: '←', label: '서쪽' },
  NORTHWEST: { arrow: '↖', label: '북서쪽' },
};

const qualityConfig: Record<string, { color: string; bg: string; border: string; glow: string; label: string }> = {
  EXCELLENT: { color: 'text-emerald-300', bg: 'bg-emerald-500/15', border: 'border-emerald-500/30', glow: 'shadow-emerald-500/20', label: '최적' },
  GOOD:      { color: 'text-sky-300',     bg: 'bg-sky-500/15',     border: 'border-sky-500/30',     glow: 'shadow-sky-500/20',     label: '양호' },
  FAIR:      { color: 'text-amber-300',   bg: 'bg-amber-500/15',   border: 'border-amber-500/30',   glow: 'shadow-amber-500/20',   label: '보통' },
  POOR:      { color: 'text-red-400',     bg: 'bg-red-500/15',     border: 'border-red-500/30',     glow: 'shadow-red-500/20',     label: '불량' },
};

const IssTracker: React.FC<IssTrackerProps> = ({ latitude, longitude }) => {
  const { t } = useTranslation();
  const { data: iss, isLoading, error } = useIssObservation(latitude, longitude);

  const quality = iss?.visibilityQuality ? qualityConfig[iss.visibilityQuality] : null;
  const direction = iss?.nextPassDirection ? directionMap[iss.nextPassDirection] : null;

  // 로딩 상태
  if (isLoading) {
    return (
      <div className="relative bg-gradient-to-br from-[#080d1f] via-[#0f1629] to-[#0a101e] rounded-xl p-6 text-white border border-cyan-500/10 overflow-hidden">
        <div className="absolute inset-0 pointer-events-none">
          <div className="absolute top-[40%] left-0 w-full h-px bg-gradient-to-r from-transparent via-cyan-500/10 to-transparent" />
        </div>
        <div className="relative animate-pulse text-center py-6">
          <div className="text-3xl mb-3">🛰️</div>
          <p className="text-cyan-300/60 text-sm">{t('weather.loading_iss_data')}</p>
        </div>
      </div>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <div className="bg-gradient-to-br from-[#080d1f] via-[#0f1629] to-[#0a101e] rounded-xl p-5 text-white border border-red-500/20">
        <div className="flex items-center gap-2 mb-3">
          <span className="text-lg">🛰️</span>
          <span className="font-bold text-sm">ISS</span>
        </div>
        <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg">
          <p className="text-red-300/80 text-xs">{t('weather.iss_no_data')}</p>
        </div>
      </div>
    );
  }

  if (!iss) return null;

  return (
    <div className="relative bg-gradient-to-br from-[#080d1f] via-[#0f1629] to-[#0a101e] rounded-xl overflow-hidden text-white border border-cyan-500/10 shadow-xl">

      {/* 배경 궤도선 */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[38%] left-0 w-full h-px bg-gradient-to-r from-transparent via-cyan-400/15 to-transparent" />
        <div className="absolute top-[42%] left-0 w-full h-px bg-gradient-to-r from-transparent via-cyan-400/8 to-transparent" />
        {/* 미세한 별 */}
        <div className="absolute top-3 right-8 w-0.5 h-0.5 bg-white/30 rounded-full" />
        <div className="absolute top-8 right-4 w-0.5 h-0.5 bg-white/20 rounded-full" />
        <div className="absolute bottom-12 left-6 w-0.5 h-0.5 bg-white/25 rounded-full" />
        <div className="absolute bottom-6 right-12 w-0.5 h-0.5 bg-white/15 rounded-full" />
      </div>

      {/* 헤더 */}
      <div className="relative px-5 pt-5 pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="relative">
              <div className="w-9 h-9 bg-cyan-500/10 border border-cyan-500/20 rounded-lg flex items-center justify-center">
                <span className="text-lg">🛰️</span>
              </div>
              <div className="absolute -top-0.5 -right-0.5 w-2 h-2 bg-emerald-400 rounded-full animate-pulse" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white leading-tight">ISS 실시간 추적</h3>
              <p className="text-[11px] text-cyan-400/50 leading-tight">International Space Station</p>
            </div>
          </div>
          {quality && (
            <span className={`px-2.5 py-1 rounded-full text-[11px] font-bold ${quality.bg} ${quality.color} ${quality.border} border`}>
              {quality.label}
            </span>
          )}
        </div>
      </div>

      {/* 상태 메시지 */}
      <div className="relative px-5 pb-3">
        <p className="text-[13px] text-gray-400 leading-relaxed">{iss.friendlyMessage}</p>
      </div>

      {/* 고도 / 속도 카드 */}
      {iss.currentAltitudeKm && (
        <div className="relative px-5 pb-4">
          <div className="grid grid-cols-2 gap-2">
            <div className="bg-white/[0.04] border border-white/[0.06] rounded-lg p-3 text-center">
              <div className="text-[11px] text-gray-500 mb-1 tracking-wide uppercase">고도</div>
              <div className="flex items-baseline justify-center gap-1">
                <span className="text-xl font-bold text-cyan-300">{Math.round(iss.currentAltitudeKm).toLocaleString()}</span>
                <span className="text-[11px] text-gray-500">km</span>
              </div>
            </div>
            {iss.currentVelocityKmh && (
              <div className="bg-white/[0.04] border border-white/[0.06] rounded-lg p-3 text-center">
                <div className="text-[11px] text-gray-500 mb-1 tracking-wide uppercase">속도</div>
                <div className="flex items-baseline justify-center gap-1">
                  <span className="text-xl font-bold text-cyan-300">{Math.round(iss.currentVelocityKmh).toLocaleString()}</span>
                  <span className="text-[11px] text-gray-500">km/h</span>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* 다음 관측 카드 */}
      {iss.nextPassTime && (
        <div className="relative mx-5 mb-5 rounded-lg border border-cyan-500/15 overflow-hidden bg-gradient-to-br from-cyan-500/[0.06] to-blue-500/[0.04]">
          {/* 카드 헤더 */}
          <div className="px-4 py-2.5 bg-cyan-500/[0.08] border-b border-cyan-500/10 flex items-center gap-2">
            <span className="text-sm">🔭</span>
            <span className="text-[13px] font-semibold text-cyan-200">다음 관측 기회</span>
          </div>

          {/* 카드 본문 */}
          <div className="p-4">
            {/* 시각 강조 표시 */}
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-cyan-500/10 border border-cyan-500/20 rounded-lg flex items-center justify-center flex-shrink-0">
                <span className="text-lg font-bold text-cyan-300">{iss.nextPassTime?.split(':')[0]}</span>
              </div>
              <div>
                <div className="text-lg font-bold text-white tracking-wide">{iss.nextPassTime}</div>
                <div className="text-[11px] text-gray-500">{iss.nextPassDate}</div>
              </div>
            </div>

            {/* 세부 정보 그리드 */}
            <div className="grid grid-cols-3 gap-2">
              {/* 방향 */}
              <div className="bg-white/[0.03] rounded-lg p-2.5 text-center">
                <div className="text-[10px] text-gray-500 mb-1.5 uppercase tracking-wider">방향</div>
                {direction ? (
                  <>
                    <div className="text-lg leading-none mb-0.5">{direction.arrow}</div>
                    <div className="text-[11px] font-medium text-gray-300">{direction.label}</div>
                  </>
                ) : (
                  <div className="text-xs text-gray-400">{iss.nextPassDirection}</div>
                )}
              </div>

              {/* 관측 시간 */}
              <div className="bg-white/[0.03] rounded-lg p-2.5 text-center">
                <div className="text-[10px] text-gray-500 mb-1.5 uppercase tracking-wider">지속</div>
                <div className="text-lg font-bold text-white leading-none mb-0.5">{iss.estimatedDuration?.replace('분', '')}</div>
                <div className="text-[11px] text-gray-400">분</div>
              </div>

              {/* 최대 고도각 */}
              <div className="bg-white/[0.03] rounded-lg p-2.5 text-center">
                <div className="text-[10px] text-gray-500 mb-1.5 uppercase tracking-wider">고도각</div>
                {iss.maxElevation != null ? (
                  <>
                    <div className="text-lg font-bold text-white leading-none mb-0.5">{iss.maxElevation.toFixed(0)}</div>
                    <div className="text-[11px] text-gray-400">도</div>
                  </>
                ) : (
                  <div className="text-xs text-gray-500">-</div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default IssTracker;
