import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useAuth } from '../contexts/AuthContext';
import { useTranslation } from 'react-i18next';

interface WeatherData {
  location: string;
  latitude: number;
  longitude: number;
  cloudCover: number;
  visibility: number;
  moonPhase: string;
  observationQuality: string;
  recommendation: string;
  observationTime: string;
}

interface AstronomyEvent {
  id: number;
  eventType: string;
  title: string;
  description: string;
  eventDate: string;
  peakTime: string;
  visibility: string;
  magnitude: string;
  isActive: boolean;
}

interface IssData {
  message_key: string;
  friendly_message: string;
  current_altitude_km?: number;
  current_velocity_kmh?: number;
  next_pass_time?: string;
  next_pass_date?: string;
  next_pass_direction?: string;
  estimated_duration?: string;
  visibility_quality?: string;
}

const WeatherWidget: React.FC = () => {
  const { t } = useTranslation();
  const { user } = useAuth();
  
  const [weather, setWeather] = useState<WeatherData | null>(null);
  const [events, setEvents] = useState<AstronomyEvent[]>([]);
  const [issData, setIssData] = useState<IssData | null>(null);
  
  const [loading, setLoading] = useState(true);
  const [weatherError, setWeatherError] = useState<string | null>(null);
  const [eventsError, setEventsError] = useState<string | null>(null);
  const [issError, setIssError] = useState<string | null>(null);
  const [collectingAstronomy, setCollectingAstronomy] = useState(false);

  useEffect(() => {
    loadAllData();
  }, []);

  const loadAllData = async () => {
    setLoading(true);
    
    try {
      // 위치 정보 가져오기
      const position = await getCurrentPosition();
      const lat = position?.coords.latitude || 37.5665;
      const lon = position?.coords.longitude || 126.9780;
      
      // 병렬로 데이터 로드
      await Promise.allSettled([
        fetchWeatherData(lat, lon),
        fetchAstronomyEvents(),
        fetchIssData(lat, lon)
      ]);
    } catch (error) {
      console.error('데이터 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const getCurrentPosition = (): Promise<GeolocationPosition | null> => {
    return new Promise((resolve) => {
      if (!navigator.geolocation) {
        resolve(null);
        return;
      }

      const timeoutId = setTimeout(() => {
        resolve(null);
      }, 5000);

      navigator.geolocation.getCurrentPosition(
        (position) => {
          clearTimeout(timeoutId);
          resolve(position);
        },
        () => {
          clearTimeout(timeoutId);
          resolve(null);
        },
        { timeout: 10000, enableHighAccuracy: true, maximumAge: 60000 }
      );
    });
  };

  const fetchWeatherData = async (latitude: number, longitude: number) => {
    try {
      setWeatherError(null);
      const response = await axios.get('/api/weather/observation', {
        params: { latitude, longitude },
        timeout: 10000
      });
      setWeather(response.data);
    } catch (error: any) {
      console.error('날씨 데이터 조회 실패:', error);
      setWeatherError(t('weather.weather_error'));
    }
  };

  const fetchAstronomyEvents = async () => {
    try {
      setEventsError(null);
      const response = await axios.get('/api/weather/events', { timeout: 10000 });
      setEvents(response.data || []);
    } catch (error: any) {
      console.error('천체 이벤트 조회 실패:', error);
      setEventsError(t('weather.events_error'));
    }
  };

  const fetchIssData = async (latitude: number, longitude: number) => {
    try {
      setIssError(null);
      const response = await axios.get('/api/weather/iss', {
        params: { latitude, longitude },
        timeout: 10000
      });
      setIssData(response.data);
    } catch (error: any) {
      console.error('ISS 데이터 조회 실패:', error);
      setIssError(t('weather.iss_no_data'));
    }
  };

  const handleCollectAstronomy = async () => {
    if (!confirm(t('weather.confirm_nasa_update'))) return;
    
    setCollectingAstronomy(true);
    try {
      await axios.post('/api/admin/scheduler/astronomy/manual');
      alert(t('weather.nasa_update_success'));
      await fetchAstronomyEvents();
    } catch (error: any) {
      console.error('천체 데이터 수집 실패:', error);
      alert(t('weather.nasa_update_failed'));
    } finally {
      setCollectingAstronomy(false);
    }
  };

  const getEventTypeIcon = (eventType: string) => {
    switch (eventType) {
      case 'ASTEROID': return '🪨';
      case 'SOLAR_FLARE': return '☀️';
      case 'GEOMAGNETIC_STORM': return '🌍';
      case 'BLOOD_MOON': return '🔴';
      case 'SOLAR_ECLIPSE': return '🌑';
      case 'SUPERMOON': return '🌕';
      default: return '⭐';
    }
  };

  const getEventTypeLabel = (eventType: string) => {
    return t(`weather.event_types.${eventType}`) || t('weather.event_types.DEFAULT');
  };

  const formatDate = (dateStr: string) => {
    try {
      const date = new Date(dateStr);
      const locale = t('common.locale') || 'ko-KR';
      return date.toLocaleDateString(locale, {
        month: 'long',
        day: 'numeric'
      });
    } catch {
      return dateStr;
    }
  };

  const formatTime = (dateStr: string) => {
    try {
      const date = new Date(dateStr);
      const locale = t('common.locale') || 'ko-KR';
      return date.toLocaleTimeString(locale, {
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateStr;
    }
  };

  // 백엔드에서 이미 아이콘으로 반환하므로 그대로 사용
  const getMoonPhaseIcon = (moonPhase: string) => {
    // 백엔드에서 이미 정확한 달의 위상 아이콘을 계산해서 반환
    return moonPhase;
  };

  const getQualityColor = (quality: string) => {
    switch (quality) {
      case 'EXCELLENT': return 'text-green-600 bg-green-100';
      case 'GOOD': return 'text-blue-600 bg-blue-100';
      case 'FAIR': return 'text-yellow-600 bg-yellow-100';
      case 'POOR': return 'text-red-600 bg-red-100';
      default: return 'text-gray-600 bg-gray-100';
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="bg-gradient-to-br from-indigo-900 via-purple-900 to-pink-900 rounded-xl p-6 text-white shadow-2xl">
          <div className="animate-pulse text-center py-8">
            <div className="text-4xl mb-4">🌌</div>
            <p className="text-blue-200 font-medium">{t('weather.loading_weather_data')}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 별 관측 조건 */}
      <div className="bg-gradient-to-br from-indigo-900 via-purple-900 to-pink-900 rounded-xl p-6 text-white shadow-2xl border border-purple-500/20">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-2xl font-bold flex items-center gap-2">
            🌟 {t('weather.star_observation')}
          </h3>
          {weather && (
            <span className={`px-3 py-1 rounded-full text-sm font-semibold ${getQualityColor(weather.observationQuality)}`}>
              {weather.observationQuality}
            </span>
          )}
        </div>

        {weatherError ? (
          <div className="p-4 bg-red-500/20 border border-red-400/30 rounded-lg">
            <p className="text-red-200 text-sm">{weatherError}</p>
          </div>
        ) : weather ? (
          <div className="bg-white/10 rounded-lg p-4 backdrop-blur-sm">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-4">
              <div className="flex justify-between items-center min-w-0">
                <span className="text-gray-300 flex-shrink-0">📍 {t('weather.location')}</span>
                <span className="font-semibold truncate ml-2">{weather.location}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-300 flex-shrink-0">☁️ {t('weather.cloud_cover')}</span>
                <span className="font-semibold ml-2">{weather.cloudCover.toFixed(0)}%</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-300 flex-shrink-0">👁️ {t('weather.visibility')}</span>
                <span className="font-semibold ml-2">{weather.visibility.toFixed(1)}km</span>
              </div>
              <div className="flex justify-between items-center min-w-0">
                <span className="text-gray-300 flex-shrink-0">{t('weather.moon_phase')}</span>
                <span className="font-semibold flex items-center ml-2">
                  <span className="text-2xl">{getMoonPhaseIcon(weather.moonPhase)}</span>
                </span>
              </div>
            </div>
            <div className="p-3 bg-white/5 rounded-lg">
              <p className="text-sm text-gray-200">{t(`weather.recommendations.${weather.recommendation}`)}</p>
            </div>
          </div>
        ) : (
          <div className="text-center py-8">
            <p className="text-gray-300">{t('weather.loading_weather')}</p>
          </div>
        )}
      </div>

      {/* 최근 천체 현상 */}
      <div className="bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 rounded-xl p-6 text-white shadow-2xl border border-blue-500/20">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-2xl font-bold flex items-center gap-2">
            🌌 {t('weather.recent_astronomy_events')}
          </h3>
          {user?.role === 'ADMIN' && (
            <button
              onClick={handleCollectAstronomy}
              disabled={collectingAstronomy}
              className="bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-600 text-white px-4 py-2 rounded-lg text-sm font-semibold transition-all duration-200"
            >
              {collectingAstronomy ? t('weather.updating') : t('weather.nasa_update')}
            </button>
          )}
        </div>

        {eventsError ? (
          <div className="p-4 bg-red-500/20 border border-red-400/30 rounded-lg">
            <p className="text-red-200 text-sm">{eventsError}</p>
          </div>
        ) : events.length > 0 ? (
          <div className="space-y-4">
            {events.map((event, index) => (
              <div key={`${event.eventType}-${index}`} className="bg-white/10 rounded-lg p-4 backdrop-blur-sm">
                <div className="flex items-start gap-3">
                  <span className="text-2xl">{getEventTypeIcon(event.eventType)}</span>
                  <div className="flex-1">
                    <div className="flex flex-wrap gap-2 mb-2">
                      <span className="text-xs px-2 py-1 bg-blue-500/30 text-blue-200 rounded-full">
                        {getEventTypeLabel(event.eventType)}
                      </span>
                      <span className="text-xs px-2 py-1 bg-gray-500/30 text-gray-300 rounded-full">
                        {formatDate(event.eventDate)}
                      </span>
                      <span className="text-xs px-2 py-1 bg-purple-500/30 text-purple-200 rounded-full">
                        {formatTime(event.eventDate)}
                      </span>
                    </div>
                    <h4 className="font-semibold text-white mb-2">
                      {t(`weather.event_titles.${event.eventType}`) || event.title}
                    </h4>
                    <p className="text-sm text-gray-300">
                      {t(`weather.event_descriptions.${event.eventType}`) || event.description}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8">
            <div className="text-4xl mb-4">🌌</div>
            <p className="text-purple-200">{t('weather.loading_events')}</p>
          </div>
        )}
      </div>

      {/* ISS 관측 기회 */}
      <div className="bg-gradient-to-br from-gray-900 via-slate-900 to-gray-900 rounded-xl p-6 text-white shadow-2xl border border-gray-500/20">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-2xl font-bold flex items-center gap-2">
            🛰️ {t('weather.iss_observation_opportunity')}
          </h3>
        </div>

        {issError ? (
          <div className="p-4 bg-red-500/20 border border-red-400/30 rounded-lg">
            <p className="text-red-200 text-sm">{issError}</p>
          </div>
        ) : issData ? (
          <div className="bg-white/10 rounded-lg p-4 backdrop-blur-sm">
            <div className="flex items-start gap-3">
              <span className="text-2xl">🛰️</span>
              <div className="flex-1">
                <h4 className="font-semibold text-white mb-3">{t('weather.iss_current_status')}</h4>
                <p className="text-sm text-gray-300 mb-4">{issData.friendly_message}</p>
                
                {issData.current_altitude_km && (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-4">
                    <div className="flex justify-between p-2 bg-white/5 rounded-lg">
                      <span className="text-gray-300 text-sm">🚀 {t('weather.iss_altitude')}</span>
                      <span className="font-semibold text-white text-sm">{Math.round(issData.current_altitude_km).toLocaleString()}km</span>
                    </div>
                    {issData.current_velocity_kmh && (
                      <div className="flex justify-between p-2 bg-white/5 rounded-lg">
                        <span className="text-gray-300 text-sm">⚡ {t('weather.iss_velocity')}</span>
                        <span className="font-semibold text-white text-sm">{Math.round(issData.current_velocity_kmh).toLocaleString()}km/h</span>
                      </div>
                    )}
                  </div>
                )}
                
                {issData.next_pass_time && (
                  <div className="p-3 bg-blue-500/20 rounded-lg border border-blue-400/30">
                    <div className="flex items-start gap-2 mb-2">
                      <span className="text-lg">🔮</span>
                      <span className="text-sm font-medium text-blue-200">{t('weather.iss_next_observation')}</span>
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pl-7">
                      <div className="text-sm text-gray-300">
                        <span className="block font-medium text-white">{t('weather.iss_next_time')}</span>
                        <span>{issData.next_pass_date} {issData.next_pass_time}</span>
                      </div>
                      <div className="text-sm text-gray-300">
                        <span className="block font-medium text-white">{t('weather.iss_next_direction')}</span>
                        <span>{issData.next_pass_direction}</span>
                      </div>
                      <div className="text-sm text-gray-300 sm:col-span-2">
                        <span className="block font-medium text-white">{t('weather.iss_next_duration')}</span>
                        <span>{issData.estimated_duration}</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        ) : (
          <div className="text-center py-8">
            <div className="text-4xl mb-4">🛰️</div>
            <p className="text-gray-200">{t('weather.loading_iss_data')}</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default WeatherWidget;