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
      case 'ASTEROID': return '🪨'; // NASA NeoWs
      case 'SOLAR_FLARE': return '☀️'; // NASA DONKI
      case 'GEOMAGNETIC_STORM': return '🌍'; // NASA DONKI
      case 'ISS_LOCATION': return '🛰️'; // NASA ISS
      case 'MARS_WEATHER': return '🔴'; // NASA Mars

      case 'METEOR_SHOWER': return '☄️';
      case 'LUNAR_ECLIPSE': return '🌙';
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
      case 'PLANET_CONJUNCTION': return '행성근접';
      case 'COMET_OBSERVATION': return '혜성관측';

      default: return '천체 이벤트';
    }
  };
  
  // 컴포넌트 렌더링 부분 (오류 상태 표시 포함)
  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="animate-pulse">
          <div className="h-4 bg-gray-200 rounded w-3/4 mb-4"></div>
          <div className="h-4 bg-gray-200 rounded w-1/2 mb-2"></div>
          <div className="h-4 bg-gray-200 rounded w-2/3"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h3 className="text-lg font-semibold mb-4 flex items-center">
        🌟 실시간 별 관측 정보
        {requestingLocation && (
          <span className="ml-2 text-sm text-blue-600">위치 확인 중...</span>
        )}
      </h3>

      {/* 위치 오류 표시 */}
      {locationError && (
        <div className="mb-4 p-3 bg-yellow-100 border border-yellow-400 rounded">
          <p className="text-yellow-700 text-sm">{locationError}</p>
          <button
            onClick={handleLocationRequest}
            className="mt-2 px-3 py-1 bg-yellow-600 text-white rounded text-sm hover:bg-yellow-700"
          >
            위치 재요청
          </button>
        </div>
      )}

      {/* 날씨 정보 */}
      {weatherError ? (
        <div className="mb-4 p-3 bg-red-100 border border-red-400 rounded">
          <p className="text-red-700 text-sm">{weatherError}</p>
          <button
            onClick={() => weather && fetchWeatherData(weather.latitude, weather.longitude)}
            className="mt-2 px-3 py-1 bg-red-600 text-white rounded text-sm hover:bg-red-700"
          >
            재시도
          </button>
        </div>
      ) : weather && (
        <div className="mb-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-gray-600">{weather.location}</span>
            <span className={`px-2 py-1 rounded text-xs font-medium ${getQualityColor(weather.observationQuality)}`}>
              {weather.observationQuality}
            </span>
          </div>
          <p className="text-sm text-gray-700">{weather.recommendation}</p>
        </div>
      )}

      {/* 천체 이벤트 */}
      <div className="mb-4">
        <div className="flex items-center justify-between mb-2">
          <h4 className="font-medium">천체 이벤트</h4>
          {user?.role === 'ADMIN' && (
            <button
              onClick={handleCollectAstronomy}
              disabled={collectingAstronomy}
              className="px-2 py-1 bg-blue-600 text-white rounded text-xs hover:bg-blue-700 disabled:opacity-50"
            >
              {collectingAstronomy ? '수집 중...' : 'NASA 수동 수집'}
            </button>
          )}
        </div>

        {eventsError ? (
          <div className="p-3 bg-red-100 border border-red-400 rounded">
            <p className="text-red-700 text-sm">{eventsError}</p>
            <button
              onClick={fetchAstronomyEvents}
              className="mt-2 px-3 py-1 bg-red-600 text-white rounded text-sm hover:bg-red-700"
            >
              재시도
            </button>
          </div>
        ) : (
          <div className="space-y-2">
            {events.length > 0 ? (
              events.map((event, index) => (
                <div key={`${event.eventType}-${index}`} className="p-2 bg-gray-50 rounded">
                  <div className="flex items-center gap-2">
                    <span className="text-lg">{getEventTypeIcon(event.eventType)}</span>
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium">{event.title}</span>
                        <span className="text-xs text-gray-500">{getEventTypeLabel(event.eventType)}</span>
                      </div>
                      <p className="text-xs text-gray-600 mt-1">{event.description}</p>
                    </div>
                  </div>
                </div>
              ))
            ) : (
              <p className="text-sm text-gray-500">현재 활성 천체 이벤트가 없습니다.</p>
            )}
          </div>
        )}
      </div>

      {/* ISS 오류 표시 */}
      {issError && (
        <div className="p-3 bg-red-100 border border-red-400 rounded">
          <p className="text-red-700 text-sm">{issError}</p>
          <button
            onClick={fetchIssLocation}
            className="mt-2 px-3 py-1 bg-red-600 text-white rounded text-sm hover:bg-red-700"
          >
            ISS 위치 재시도
          </button>
        </div>
      )}
    </div>
  );
};

export default WeatherWidget;