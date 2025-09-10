import React, { useState, useEffect, useMemo, useCallback } from 'react';
import axios from 'axios';
import { useAuth } from '../contexts/AuthContext';

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

interface IssLocation {
  latitude: string;
  longitude: string;
  timestamp: number;
}

// 로그 새니타이징 함수
const sanitizeForLog = (input: any): string => {
  if (typeof input === 'object') {
    return JSON.stringify(input).replace(/[\r\n]/g, ' ');
  }
  return String(input).replace(/[\r\n]/g, ' ');
};

const WeatherWidget: React.FC = () => {
  const [weather, setWeather] = useState<WeatherData | null>(null);
  const [events, setEvents] = useState<AstronomyEvent[]>([]);
  const [issLocation, setIssLocation] = useState<IssLocation | null>(null);
  const [loading, setLoading] = useState(true);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [weatherError, setWeatherError] = useState<string | null>(null);
  const [eventsError, setEventsError] = useState<string | null>(null);
  const [issError, setIssError] = useState<string | null>(null);
  const [collectingAstronomy, setCollectingAstronomy] = useState(false);
  const [requestingLocation, setRequestingLocation] = useState(false);
  const { user } = useAuth();

  // 30일 전 날짜 메모이제이션
  const thirtyDaysAgo = useMemo(() => {
    return new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
  }, []);

  useEffect(() => {
    // 병렬로 모든 데이터 로드 시작
    Promise.allSettled([
      getCurrentLocationWithTimeout(),
      fetchAstronomyEvents(),
      fetchIssLocation()
    ]).finally(() => {
      setLoading(false);
    });
    
    // ISS 위치 5분마다 업데이트 (성능 최적화)
    const issInterval = setInterval(fetchIssLocation, 5 * 60 * 1000);
    
    return () => clearInterval(issInterval);
  }, []);

  const getCurrentLocationWithTimeout = () => {
    return new Promise((resolve) => {
      if (!navigator.geolocation) {
        setLocationError('브라우저에서 위치 서비스를 지원하지 않습니다.');
        fetchWeatherData(37.5665, 126.9780);
        resolve(null);
        return;
      }

      const timeoutId = setTimeout(() => {
        setLocationError('위치 요청 시간 초과. 서울 기준으로 표시합니다.');
        fetchWeatherData(37.5665, 126.9780);
        resolve(null);
      }, 5000); // 5초 타임아웃

      setRequestingLocation(true);
      navigator.geolocation.getCurrentPosition(
        (position) => {
          clearTimeout(timeoutId);
          const { latitude, longitude } = position.coords;
          setLocationError(null);
          fetchWeatherData(latitude, longitude);
          setRequestingLocation(false);
          resolve(position);
        },
        (error) => {
          clearTimeout(timeoutId);
          console.error('위치 정보 오류:', sanitizeForLog(error.message || 'Unknown error'));
          setLocationError('위치 정보를 가져올 수 없습니다. 서울 기준으로 표시합니다.');
          fetchWeatherData(37.5665, 126.9780);
          setRequestingLocation(false);
          resolve(null);
        },
        {
          timeout: 5000,
          enableHighAccuracy: false, // 모바일에서 빠른 응답을 위해
          maximumAge: 300000 // 5분간 캐시된 위치 사용
        }
      );
    });
  };
  
  // ISS 위치 변경 시 이벤트 목록 업데이트
  useEffect(() => {
    setEvents(prevEvents => {
      if (prevEvents.length === 0) return prevEvents;
      const astronomyEvents = prevEvents.filter(event => event.eventType !== 'ISS_LOCATION');
      return updateEventsWithIss(astronomyEvents, issLocation);
    });
  }, [issLocation]);

  const getCurrentLocation = () => {
    getCurrentLocationWithTimeout();
  };

  const handleLocationRequest = () => {
    setLocationError(null);
    getCurrentLocation();
  };

  const fetchWeatherData = useCallback(async (latitude: number, longitude: number) => {
    try {
      setWeatherError(null);
      console.log('날씨 데이터 요청:', sanitizeForLog({ latitude, longitude }));
      const response = await axios.get(`/api/weather/observation`, {
        params: { latitude, longitude }
      });
      console.log('날씨 데이터 응답 수신 완료');
      setWeather(response.data);
    } catch (error: any) {
      const errorMessage = '날씨 데이터를 불러올 수 없습니다.';
      console.error('날씨 데이터 조회 실패:', sanitizeForLog(error.message || 'Unknown error'));
      setWeatherError(errorMessage);
    }
  }, []);

  const fetchIssLocation = useCallback(async () => {
    try {
      setIssError(null);
      console.log('ISS 위치 업데이트 시작:', new Date().toLocaleTimeString());
      const response = await axios.get('/api/weather/iss');
      const data = response.data;
      console.log('ISS 데이터 수신 완료');
      if (data.iss_position) {
        setIssLocation({
          latitude: data.iss_position.latitude,
          longitude: data.iss_position.longitude,
          timestamp: data.timestamp
        });
        console.log('ISS 위치 업데이트 완료');
      }
    } catch (error: any) {
      const errorMessage = 'ISS 위치 정보를 불러올 수 없습니다.';
      console.error('ISS 위치 조회 실패:', sanitizeForLog(error.message || 'Unknown error'));
      setIssError(errorMessage);
    }
  }, []);

  const updateEventsWithIss = (astronomyEvents: AstronomyEvent[], currentIssLocation: IssLocation | null) => {
    let selectedEvents = astronomyEvents.slice(0, 4);
    
    // ISS 실시간 데이터 추가
    if (currentIssLocation) {
      const issEvent: AstronomyEvent = {
        id: 0,
        eventType: 'ISS_LOCATION',
        title: 'ISS 실시간 위치',
        description: `국제우주정거장 현재 위치: ${parseFloat(currentIssLocation.latitude).toFixed(1)}°, ${parseFloat(currentIssLocation.longitude).toFixed(1)}°`,
        eventDate: new Date().toISOString(),
        peakTime: new Date().toISOString(),
        visibility: 'WORLDWIDE',
        magnitude: 'MEDIUM',
        isActive: true
      };
      selectedEvents = [issEvent, ...selectedEvents].slice(0, 5);
    }
    
    return selectedEvents;
  };

  const fetchAstronomyEvents = useCallback(async () => {
    try {
      setEventsError(null);
      console.log('천체 이벤트 요청 시작');
      const response = await axios.get('/api/weather/events');
      console.log('천체 이벤트 수신 완료:', sanitizeForLog(response.data.length + '개'));
      
      // 최근 30일 내 실제 발생한 천체 현상만 표시
      
      const recentEvents = response.data.filter((event: AstronomyEvent) => {
        const eventDate = new Date(event.eventDate);
        return eventDate >= thirtyDaysAgo;
      });
      
      // 최신순 정렬 후 타입별 최대 1개씩 선택
      const sortedEvents = recentEvents.sort((a: AstronomyEvent, b: AstronomyEvent) => {
        const aDate = new Date(a.eventDate);
        const bDate = new Date(b.eventDate);
        return bDate.getTime() - aDate.getTime(); // 최신순
      });
      
      const eventsByType = sortedEvents.reduce((acc: Record<string, AstronomyEvent>, event: AstronomyEvent) => {
        const typeGroup = event.eventType.includes('ASTEROID') ? 'ASTEROID' :
                         event.eventType.includes('SOLAR') ? 'SOLAR_FLARE' :
                         event.eventType.includes('GEOMAGNETIC') ? 'GEOMAGNETIC_STORM' :
                         event.eventType.includes('METEOR') || event.eventType.includes('LUNAR') || event.eventType.includes('PLANET') ? 'PREDICTED' : 'OTHER';
        
        if (!acc[typeGroup]) {
          acc[typeGroup] = event;
        }
        return acc;
      }, {});
      
      const astronomyEvents = Object.values(eventsByType);
      const finalEvents = updateEventsWithIss(astronomyEvents, issLocation);
      setEvents(finalEvents);
    } catch (error: any) {
      const errorMessage = '천체 이벤트 정보를 불러올 수 없습니다.';
      console.error('천체 이벤트 조회 실패:', sanitizeForLog(error.message || 'Unknown error'));
      setEventsError(errorMessage);
    }
  }, [thirtyDaysAgo, issLocation]);

  const handleCollectAstronomy = async () => {
    if (!confirm('NASA API로 천체 데이터를 수동 업데이트하시겠습니까?')) return;
    
    setCollectingAstronomy(true);
    try {
      await axios.post('/api/admin/scheduler/astronomy/manual');
      alert('천체 데이터 업데이트 완료! (NASA NeoWs/DONKI/Mars)');
      await fetchAstronomyEvents();
    } catch (error: any) {
      console.error('천체 데이터 수집 실패:', sanitizeForLog(error.message || 'Unknown error'));
      alert('천체 데이터 업데이트 실패');
    } finally {
      setCollectingAstronomy(false);
    }
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

  const getEventTypeIcon = (eventType: string) => {
    switch (eventType) {
      case 'ASTEROID': return '🪨';
      case 'SOLAR_FLARE': return '☀️';
      case 'GEOMAGNETIC_STORM': return '🌍';
      case 'ISS_LOCATION': return '🛰️';
      case 'MARS_WEATHER': return '🔴';
      case 'METEOR_SHOWER': return '☄️';
      case 'LUNAR_ECLIPSE': return '🌙';
      case 'BLOOD_MOON': return '🔴';
      case 'TOTAL_LUNAR_ECLIPSE': return '🌑';
      case 'SOLAR_ECLIPSE': return '☀️';
      case 'PLANET_CONJUNCTION': return '🪐';
      case 'COMET_OBSERVATION': return '✨';
      case 'SUPERMOON': return '🌕';
      case 'SPECIAL': return '🌠';
      default: return '⭐';
    }
  };
  
  const getEventTypeLabel = (eventType: string) => {
    switch (eventType) {
      case 'ASTEROID': return 'NASA 지구근접소행성';
      case 'SOLAR_FLARE': return 'NASA 태양플레어';
      case 'GEOMAGNETIC_STORM': return 'NASA 지자기폭풍';
      case 'ISS_LOCATION': return 'NASA 국제우주정거장';
      case 'MARS_WEATHER': return 'NASA 화성날씨';
      case 'METEOR_SHOWER': return '유성우';
      case 'LUNAR_ECLIPSE': return '월식';
      case 'BLOOD_MOON': return '블러드문';
      case 'TOTAL_LUNAR_ECLIPSE': return '개기월식';
      case 'SOLAR_ECLIPSE': return '일식';
      case 'PLANET_CONJUNCTION': return '행성근접';
      case 'COMET_OBSERVATION': return '혜성관측';
      default: return '천체 이벤트';
    }
  };
  
  // 로딩 상태
  if (loading) {
    return (
      <div className="space-y-6">
        {/* 날씨 관측 조건 로딩 */}
        <div className="bg-gradient-to-br from-indigo-900 via-purple-900 to-pink-900 rounded-xl p-6 text-white shadow-2xl border border-purple-500/20">
          <div className="animate-pulse">
            {/* 헤더 */}
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 bg-white/20 rounded-full"></div>
                <div className="h-6 bg-white/20 rounded w-32"></div>
              </div>
              <div className="w-20 h-6 bg-white/20 rounded-full"></div>
            </div>
            
            {/* 로딩 메시지 */}
            <div className="text-center py-8">
              <div className="flex items-center justify-center gap-3 mb-4">
                <div className="animate-spin w-6 h-6 border-2 border-blue-400 border-t-transparent rounded-full"></div>
                <span className="text-blue-200 font-medium">오늘의 날씨 정보를 불러오는 중...</span>
              </div>
              <p className="text-gray-300 text-sm">위치 기반 별 관측 조건을 분석하고 있습니다</p>
            </div>
          </div>
        </div>

        {/* 천체 이벤트 로딩 */}
        <div className="bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 rounded-xl p-6 text-white shadow-2xl border border-blue-500/20">
          <div className="animate-pulse">
            {/* 헤더 */}
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 bg-white/20 rounded-full"></div>
                <div className="h-6 bg-white/20 rounded w-40"></div>
              </div>
              <div className="w-24 h-8 bg-white/20 rounded-lg"></div>
            </div>
            
            {/* 로딩 메시지 */}
            <div className="text-center py-8">
              <div className="flex items-center justify-center gap-3 mb-4">
                <div className="animate-spin w-6 h-6 border-2 border-purple-400 border-t-transparent rounded-full"></div>
                <span className="text-purple-200 font-medium">NASA에서 천체 데이터를 불러오는 중...</span>
              </div>
              <p className="text-gray-300 text-sm mb-2">실시간 우주 정보를 수집하고 있습니다</p>
              <div className="flex items-center justify-center gap-4 text-xs text-gray-400">
                <span>🪨 소행성</span>
                <span>☀️ 태양 플레어</span>
                <span>🛰️ ISS 위치</span>
                <span>🌍 지자기 폭풍</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 날씨 관측 조건 */}
      <div className="bg-gradient-to-br from-indigo-900 via-purple-900 to-pink-900 rounded-xl p-6 text-white shadow-2xl border border-purple-500/20">
        <div className="flex items-center justify-between mb-4 sm:mb-6">
          <h3 className="text-lg sm:text-2xl font-bold flex items-center gap-2">
            🌟 별 관측 조건
            {requestingLocation && (
              <div className="animate-spin w-4 h-4 sm:w-5 sm:h-5 border-2 border-white border-t-transparent rounded-full"></div>
            )}
          </h3>
          {weather && (
            <span className={`px-3 py-1 rounded-full text-sm font-semibold ${getQualityColor(weather.observationQuality)} shadow-lg`}>
              {weather.observationQuality}
            </span>
          )}
        </div>

        {/* 위치 오류 표시 */}
        {locationError && (
          <div className="mb-4 p-4 bg-yellow-500/20 border border-yellow-400/30 rounded-lg backdrop-blur-sm">
            <p className="text-yellow-200 text-sm mb-2">{locationError}</p>
            <button
              onClick={handleLocationRequest}
              className="bg-gradient-to-r from-yellow-600 to-yellow-700 hover:from-yellow-700 hover:to-yellow-800 text-white px-4 py-2 rounded-lg text-sm font-semibold transition-all duration-200 shadow-lg hover:shadow-xl transform hover:scale-105"
            >
              📍 위치 재요청
            </button>
          </div>
        )}

        {/* 날씨 정보 */}
        {weatherError ? (
          <div className="p-4 bg-red-500/20 border border-red-400/30 rounded-lg backdrop-blur-sm">
            <p className="text-red-200 text-sm mb-2">{weatherError}</p>
            <button
              onClick={() => weather && fetchWeatherData(weather.latitude, weather.longitude)}
              className="bg-gradient-to-r from-red-600 to-red-700 hover:from-red-700 hover:to-red-800 text-white px-4 py-2 rounded-lg text-sm font-semibold transition-all duration-200 shadow-lg hover:shadow-xl"
            >
              🔄 재시도
            </button>
          </div>
        ) : weather ? (
          <div className="space-y-4">
            <div className="bg-white/10 rounded-lg p-3 sm:p-4 backdrop-blur-sm border border-white/20">
              <div className="space-y-3 sm:space-y-0 sm:grid sm:grid-cols-2 sm:gap-4 mb-4">
                <div className="flex justify-between items-center">
                  <span className="text-gray-300 text-xs sm:text-sm">📍 위치</span>
                  <span className="font-semibold text-white text-xs sm:text-sm truncate ml-2">{weather.location}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-gray-300 text-xs sm:text-sm">☁️ 구름량</span>
                  <span className="font-semibold text-white text-xs sm:text-sm">{weather.cloudCover.toFixed(0)}%</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-gray-300 text-xs sm:text-sm">👁️ 시정</span>
                  <span className="font-semibold text-white text-xs sm:text-sm">{weather.visibility.toFixed(1)}km</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-gray-300 text-xs sm:text-sm">🌙 달의 위상</span>
                  <span className="font-semibold text-white text-xs sm:text-sm">{weather.moonPhase}</span>
                </div>
              </div>
              
              <div className="p-2 sm:p-3 bg-white/5 rounded-lg border border-white/10">
                <p className="text-xs sm:text-sm text-gray-200 leading-relaxed">{weather.recommendation}</p>
              </div>
              
              <div className="text-xs text-gray-400 text-center sm:text-right mt-2">
                ⏰ {weather.observationTime}
              </div>
            </div>
          </div>
        ) : (
          <div className="text-center py-8">
            <div className="animate-pulse">
              <div className="text-4xl mb-4">🌌</div>
              <div className="flex items-center justify-center gap-3 mb-2">
                <div className="animate-spin w-5 h-5 border-2 border-blue-400 border-t-transparent rounded-full"></div>
                <p className="text-blue-200 font-medium">날씨 데이터를 불러오는 중...</p>
              </div>
              <p className="text-gray-300 text-sm">위치 정보를 분석하여 별 관측 조건을 계산합니다</p>
            </div>
          </div>
        )}
      </div>

      {/* 천체 이벤트 */}
      <div className="bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 rounded-xl p-6 text-white shadow-2xl border border-blue-500/20">
        <div className="flex items-center justify-between mb-4 sm:mb-6">
          <h3 className="text-lg sm:text-2xl font-bold flex items-center gap-2">
            🌌 최근 천체 현상
          </h3>
          {user?.role === 'ADMIN' && (
            <button
              onClick={handleCollectAstronomy}
              disabled={collectingAstronomy}
              className="bg-gradient-to-r from-indigo-600 to-indigo-700 hover:from-indigo-700 hover:to-indigo-800 disabled:from-gray-600 disabled:to-gray-700 text-white px-4 py-2 rounded-lg text-sm font-semibold transition-all duration-200 shadow-lg hover:shadow-xl flex items-center gap-2"
            >
              {collectingAstronomy ? (
                <>
                  <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
                  업데이트 중...
                </>
              ) : (
                <>
                  🔄 NASA 업데이트
                </>
              )}
            </button>
          )}
        </div>

        {eventsError ? (
          <div className="p-4 bg-red-500/20 border border-red-400/30 rounded-lg backdrop-blur-sm">
            <p className="text-red-200 text-sm mb-2">{eventsError}</p>
            <button
              onClick={fetchAstronomyEvents}
              className="bg-gradient-to-r from-red-600 to-red-700 hover:from-red-700 hover:to-red-800 text-white px-4 py-2 rounded-lg text-sm font-semibold transition-all duration-200 shadow-lg hover:shadow-xl"
            >
              🔄 재시도
            </button>
          </div>
        ) : events.length > 0 ? (
          <div className="space-y-3">
            {events.map((event, index) => (
              <div key={`${event.eventType}-${index}`} className="bg-white/10 rounded-lg p-3 sm:p-4 backdrop-blur-sm border border-white/20 hover:bg-white/15 transition-all duration-200">
                <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
                  <div className="flex items-start gap-2 sm:gap-3 flex-1">
                    <span className="text-lg sm:text-2xl flex-shrink-0">{getEventTypeIcon(event.eventType)}</span>
                    <div className="flex-1 min-w-0">
                      <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-2 mb-2">
                        <h4 className="font-semibold text-white text-sm sm:text-base truncate">{event.title}</h4>
                        <span className="text-xs px-2 py-1 bg-blue-500/30 text-blue-200 rounded-full border border-blue-400/30 self-start">
                          {getEventTypeLabel(event.eventType)}
                        </span>
                      </div>
                      <p className="text-xs sm:text-sm text-gray-300 leading-relaxed">{event.description}</p>
                    </div>
                  </div>
                  <div className="text-left sm:text-right text-xs sm:text-sm flex-shrink-0">
                    <div className="text-gray-300 font-medium">
                      {new Date(event.eventDate).toLocaleDateString('ko-KR')}
                    </div>
                    <div className="text-xs text-gray-400">
                      {new Date(event.peakTime).toLocaleTimeString('ko-KR', { 
                        hour: '2-digit', 
                        minute: '2-digit' 
                      })}
                    </div>
                  </div>
                </div>
              </div>
            ))
          </div>
        ) : (
          <div className="text-center py-8">
            <div className="text-4xl mb-4">🌌</div>
            <div className="flex items-center justify-center gap-3 mb-2">
              <div className="animate-spin w-5 h-5 border-2 border-purple-400 border-t-transparent rounded-full"></div>
              <p className="text-purple-200 font-medium">천체 이벤트를 불러오는 중...</p>
            </div>
            <p className="text-gray-300 text-sm mb-2">NASA API에서 최신 우주 정보를 가져오고 있습니다</p>
            <div className="flex items-center justify-center gap-3 text-xs text-gray-400">
              <span>🪨 NeoWs</span>
              <span>☀️ DONKI</span>
              <span>🛰️ ISS</span>
            </div>
          </div>
        )}

        {/* ISS 오류 표시 */}
        {issError && (
          <div className="mt-4 p-4 bg-red-500/20 border border-red-400/30 rounded-lg backdrop-blur-sm">
            <p className="text-red-200 text-sm mb-2">{issError}</p>
            <button
              onClick={fetchIssLocation}
              className="bg-gradient-to-r from-red-600 to-red-700 hover:from-red-700 hover:to-red-800 text-white px-4 py-2 rounded-lg text-sm font-semibold transition-all duration-200 shadow-lg hover:shadow-xl"
            >
              🛰️ ISS 위치 재시도
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default WeatherWidget;