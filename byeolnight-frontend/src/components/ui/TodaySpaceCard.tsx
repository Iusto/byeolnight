import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useIssObservation, useWeatherObservation } from '../../hooks/useWeatherData';
import type { Post } from '../../types/post';

interface TodaySpaceCardProps {
  latestNews?: Post;
  latestCinema?: Post;
}

const DEFAULT_COORDINATES = { lat: 37.5665, lon: 126.978 };

const qualityStyles: Record<string, string> = {
  EXCELLENT: 'border-emerald-400/40 bg-emerald-400/15 text-emerald-200',
  GOOD: 'border-sky-400/40 bg-sky-400/15 text-sky-200',
  FAIR: 'border-amber-400/40 bg-amber-400/15 text-amber-200',
  POOR: 'border-rose-400/40 bg-rose-400/15 text-rose-200',
};

const directionI18nKeys: Record<string, string> = {
  NORTH: 'weather.compass_north',
  NORTHEAST: 'weather.compass_northeast',
  EAST: 'weather.compass_east',
  SOUTHEAST: 'weather.compass_southeast',
  SOUTH: 'weather.compass_south',
  SOUTHWEST: 'weather.compass_southwest',
  WEST: 'weather.compass_west',
  NORTHWEST: 'weather.compass_northwest',
};

interface ScoreBarProps {
  icon: string;
  label: string;
  score: number;
  maxScore: number;
}

function ScoreBar({ icon, label, score, maxScore }: ScoreBarProps) {
  const width = `${Math.min(100, Math.max(0, (score / maxScore) * 100))}%`;

  return (
    <div>
      <div className="mb-1 flex items-center justify-between text-[11px] text-slate-300">
        <span>{icon} {label}</span>
        <span className="font-semibold text-white">{score}/{maxScore}</span>
      </div>
      <div className="h-1.5 overflow-hidden rounded-full bg-white/10">
        <div className="h-full rounded-full bg-gradient-to-r from-cyan-400 to-violet-400" style={{ width }} />
      </div>
    </div>
  );
}

/**
 * 홈에서 오늘 확인할 우주 정보를 한 번에 보여준다.
 * 날씨·ISS는 위치 기반 API를 사용하고, 뉴스·시네마는 홈이 이미 조회한 게시글을 재사용한다.
 */
export default function TodaySpaceCard({ latestNews, latestCinema }: TodaySpaceCardProps) {
  const { t } = useTranslation();
  const [coordinates, setCoordinates] = useState(DEFAULT_COORDINATES);
  const [locationLoading, setLocationLoading] = useState(true);

  const { data: weather, isLoading: weatherLoading, error: weatherError } =
    useWeatherObservation(coordinates.lat, coordinates.lon);
  const { data: iss, isLoading: issLoading, error: issError } =
    useIssObservation(coordinates.lat, coordinates.lon);

  useEffect(() => {
    if (!navigator.geolocation) {
      setLocationLoading(false);
      return;
    }

    let settled = false;
    const finish = (position?: GeolocationPosition) => {
      if (settled) return;
      settled = true;
      if (position) {
        setCoordinates({ lat: position.coords.latitude, lon: position.coords.longitude });
      }
      setLocationLoading(false);
    };

    const timeoutId = window.setTimeout(() => finish(), 5_000);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        window.clearTimeout(timeoutId);
        finish(position);
      },
      () => {
        window.clearTimeout(timeoutId);
        finish();
      },
      { timeout: 10_000, enableHighAccuracy: true, maximumAge: 60_000 },
    );

    return () => window.clearTimeout(timeoutId);
  }, []);

  const scoreAvailable = weather?.dataStatus !== 'UNAVAILABLE' && weather?.observationScore != null;
  const quality = weather?.observationQuality ?? 'UNKNOWN';
  const directionKey = iss?.nextPassDirection ? directionI18nKeys[iss.nextPassDirection] : undefined;

  return (
    <section className="relative overflow-hidden rounded-2xl border border-cyan-400/20 bg-gradient-to-br from-[#071124]/95 via-[#111632]/95 to-[#20143a]/95 text-white shadow-2xl shadow-cyan-950/30">
      <div className="pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-violet-500/15 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-20 -left-12 h-52 w-52 rounded-full bg-cyan-500/10 blur-3xl" />

      <div className="relative border-b border-white/10 px-5 py-4">
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-cyan-300/70">
              {t('weather.today_space_subtitle')}
            </p>
            <h2 className="mt-1 text-xl font-bold">🌌 {t('weather.today_space_title')}</h2>
          </div>
          {weather?.dataStatus === 'STALE' && (
            <span className="rounded-full border border-amber-400/30 bg-amber-400/10 px-2 py-1 text-[10px] text-amber-200">
              {t('weather.stale_badge')}
            </span>
          )}
        </div>
      </div>

      <div className="relative space-y-4 p-5">
        {locationLoading || weatherLoading ? (
          <div className="animate-pulse py-8 text-center">
            <div className="mb-3 text-4xl">🔭</div>
            <p className="text-sm text-cyan-200/70">{t('weather.calculating_score')}</p>
          </div>
        ) : weatherError || !weather || weather.dataStatus === 'UNAVAILABLE' ? (
          <div className="rounded-xl border border-rose-400/20 bg-rose-400/10 p-4 text-sm text-rose-100">
            {t('weather.data_unavailable')}
          </div>
        ) : (
          <>
            <div className="grid grid-cols-[112px_1fr] gap-4">
              <div className="flex aspect-square flex-col items-center justify-center rounded-full border-4 border-cyan-300/30 bg-slate-950/40 shadow-inner shadow-cyan-400/10">
                <span className="text-4xl font-black tracking-tight text-cyan-200">
                  {scoreAvailable ? weather.observationScore : '-'}
                </span>
                <span className="text-[11px] text-slate-400">/ 100</span>
              </div>

              <div className="min-w-0 self-center">
                <div className="mb-2 flex flex-wrap items-center gap-2">
                  <span className="text-sm font-semibold text-slate-200">{t('weather.observation_score')}</span>
                  <span className={`rounded-full border px-2 py-0.5 text-[11px] font-bold ${qualityStyles[quality] ?? 'border-slate-400/30 bg-slate-400/10 text-slate-300'}`}>
                    {t(`weather.observation_quality.${quality}`, { defaultValue: quality })}
                  </span>
                </div>
                <p className="text-xs leading-relaxed text-slate-400">
                  {t('weather.score_basis')}
                </p>
                <p className="mt-2 truncate text-xs text-cyan-200/80">📍 {weather.location}</p>
              </div>
            </div>

            {scoreAvailable && weather.cloudScore != null && weather.visibilityScore != null && weather.moonScore != null && (
              <div className="grid gap-2.5 rounded-xl border border-white/[0.07] bg-white/[0.04] p-3">
                <ScoreBar icon="☁️" label={t('weather.score_cloud')} score={weather.cloudScore} maxScore={55} />
                <ScoreBar icon="👁️" label={t('weather.score_visibility')} score={weather.visibilityScore} maxScore={30} />
                <ScoreBar icon={weather.moonPhase} label={t('weather.score_moonlight')} score={weather.moonScore} maxScore={15} />
              </div>
            )}

            <div className="grid grid-cols-3 gap-2">
              <div className="rounded-lg bg-white/[0.04] p-2.5 text-center">
                <p className="text-[10px] text-slate-500">{t('weather.cloud_cover')}</p>
                <p className="mt-1 text-sm font-bold">{weather.cloudCover?.toFixed(0)}%</p>
              </div>
              <div className="rounded-lg bg-white/[0.04] p-2.5 text-center">
                <p className="text-[10px] text-slate-500">{t('weather.visibility')}</p>
                <p className="mt-1 text-sm font-bold">{weather.visibility?.toFixed(1)}km</p>
              </div>
              <div className="rounded-lg bg-white/[0.04] p-2.5 text-center">
                <p className="text-[10px] text-slate-500">{t('weather.moon_phase')}</p>
                <p className="mt-0.5 text-xl">{weather.moonPhase}</p>
              </div>
            </div>

            <p className="rounded-lg bg-violet-400/[0.07] px-3 py-2 text-xs leading-relaxed text-violet-100/80">
              {t(`weather.recommendations.${weather.recommendation.toLowerCase()}`, { defaultValue: '' })}
            </p>
          </>
        )}

        <div className="rounded-xl border border-cyan-400/15 bg-cyan-400/[0.05] p-3.5">
          <div className="mb-2 flex items-center justify-between">
            <h3 className="text-sm font-bold text-cyan-100">🛰️ {t('weather.iss_next_observation')}</h3>
            {iss?.visibilityQuality && (
              <span className="text-[10px] font-semibold text-cyan-300/70">
                {t(`weather.observation_quality.${iss.visibilityQuality}`, { defaultValue: iss.visibilityQuality })}
              </span>
            )}
          </div>
          {issLoading ? (
            <p className="animate-pulse text-xs text-slate-400">{t('weather.loading_iss_data')}</p>
          ) : issError || !iss ? (
            <p className="text-xs text-slate-400">{t('weather.iss_no_data')}</p>
          ) : iss.nextPassTime ? (
            <div className="flex items-end justify-between gap-3">
              <div>
                <p className="text-lg font-bold text-white">{iss.nextPassTime}</p>
                <p className="text-[11px] text-slate-400">{iss.nextPassDate}</p>
              </div>
              <p className="text-right text-xs text-cyan-200/80">
                {directionKey ? t(directionKey) : iss.nextPassDirection}
                {iss.estimatedDuration ? ` · ${iss.estimatedDuration}` : ''}
              </p>
            </div>
          ) : (
            <p className="text-xs text-slate-400">{t('weather.iss_no_upcoming_pass')}</p>
          )}
        </div>

        <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
          <ContentLink
            icon="🚀"
            label={t('weather.latest_space_news')}
            emptyText={t('weather.no_space_news')}
            post={latestNews}
            accentClass="hover:border-sky-400/40"
          />
          <ContentLink
            icon="🎬"
            label={t('weather.latest_space_cinema')}
            emptyText={t('weather.no_space_cinema')}
            post={latestCinema}
            accentClass="hover:border-violet-400/40"
          />
        </div>
      </div>
    </section>
  );
}

interface ContentLinkProps {
  icon: string;
  label: string;
  emptyText: string;
  post?: Post;
  accentClass: string;
}

function ContentLink({ icon, label, emptyText, post, accentClass }: ContentLinkProps) {
  const content = (
    <>
      <p className="mb-1 text-[10px] font-semibold uppercase tracking-wider text-slate-500">{icon} {label}</p>
      <p className="line-clamp-2 text-xs font-medium leading-relaxed text-slate-200">
        {post?.title ?? emptyText}
      </p>
    </>
  );

  if (!post) {
    return <div className="rounded-xl border border-white/[0.07] bg-white/[0.03] p-3">{content}</div>;
  }

  return (
    <Link
      to={`/posts/${post.id}`}
      className={`rounded-xl border border-white/[0.07] bg-white/[0.03] p-3 transition-colors hover:bg-white/[0.06] ${accentClass}`}
    >
      {content}
    </Link>
  );
}
