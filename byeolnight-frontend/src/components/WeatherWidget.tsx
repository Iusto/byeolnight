import React, { useState, useEffect } from 'react';
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

const WeatherWidget: React.FC = () => {
  const [weather, setWeather] = useState<WeatherData | null>(null);
  const [events, setEvents] = useState<AstronomyEvent[]>([]);
  const [issLocation, setIssLocation] = useState<IssLocation | null>(null);
  const [loading, setLoading] = useState(true);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [collectingAstronomy, setCollectingAstronomy] = useState(false);
  const { user } = useAuth();

  useEffect(() => {
    getCurrentLocation();
    fetchAstronomyEvents();
    fetchIssLocation();
    
    // ISS 위치 1분마다 업데이트
    const issInterval = setInterval(fetchIssLocation, 60 * 1000);
    
    return () => clearInterval(issInterval);
  }, []);

  const getCurrentLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const { latitude, longitude } = position.coords;
          fetchWeatherData(latitude, longitude);
        },
        (error) => {
          console.error('위치 정보 오류:', error);
          setLocationError('위치 정보를 가져올 수 없습니다. 서울 기준으로 표시합니다.');
          // 서울 좌표로 기본값 설정
          fetchWeatherData(37.5665, 126.9780);
        }
      );
    } else {
      setLocationError('브라우저에서 위치 서비스를 지원하지 않습니다.');
      fetchWeatherData(37.5665, 126.9780);
    }
  };

  const fetchWeatherData = async (latitude: number, longitude: number) => {
    try {
      console.log('날씨 데이터 요청:', { latitude, longitude });
      const response = await axios.get(`/api/weather/observation`, {
        params: { latitude, longitude }
      });
      console.log('날씨 데이터 응답:', response.data);
      setWeather(response.data);
    } catch (error) {
      console.error('날씨 데이터 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchIssLocation = async () => {
    try {
      console.log('ISS 위치 업데이트 시작:', new Date().toLocaleTimeString());
      const response = await fetch('http://api.open-notify.org/iss-now.json');
      const data = await response.json();
      console.log('ISS 데이터 수신:', data);
      setIssLocation({
        latitude: data.iss_position.latitude,
        longitude: data.iss_position.longitude,
        timestamp: data.timestamp
      });
      console.log('ISS 위치 업데이트 완료');
    } catch (error) {
      console.error('ISS 위치 조회 실패:', error);
    }
  };

  const fetchAstronomyEvents = async () => {
    try {
      console.log('천체 이벤트 요청 시작');
      const response = await axios.get('/api/weather/events');
      console.log('천체 이벤트 응답:', response.data);
      // 미래 이벤트 우선, 과거 이벤트는 최근 3일 내만 표시
      const now = new Date();
      const threeDaysAgo = new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000);
      
      const filteredEvents = response.data.filter((event: AstronomyEvent) => {
        const eventDate = new Date(event.eventDate);
        return eventDate > now || eventDate > threeDaysAgo;
      });
      
      // 미래 이벤트 우선 정렬 후 타입별 최대 1개씩 선택
      const sortedEvents = filteredEvents.sort((a: AstronomyEvent, b: AstronomyEvent) => {
        const aDate = new Date(a.eventDate);
        const bDate = new Date(b.eventDate);
        const aFuture = aDate > now;
        const bFuture = bDate > now;
        
        if (aFuture && !bFuture) return -1;
        if (!aFuture && bFuture) return 1;
        
        return aDate.getTime() - bDate.getTime();
      });
      
      const eventsByType = sortedEvents.reduce((acc: Record<string, AstronomyEvent>, event: AstronomyEvent) => {
        const typeGroup = event.eventType.includes('ASTEROID') ? 'NEOWS' :
                         event.eventType.includes('SOLAR') || event.eventType.includes('GEOMAGNETIC') ? 'DONKI' :
                         event.eventType.includes('METEOR') || event.eventType.includes('LUNAR') || event.eventType.includes('PLANET') ? 'PREDICTED' : 'OTHER';
        
        if (!acc[typeGroup]) {
          acc[typeGroup] = event;
        }
        return acc;
      }, {});
      
      let selectedEvents = Object.values(eventsByType).slice(0, 4);
      
      // ISS 실시간 데이터 추가
      if (issLocation) {
        const issEvent: AstronomyEvent = {
          id: 0,
          eventType: 'ISS_LOCATION',
          title: 'ISS 실시간 위치',
          description: `국제우주정거장 현재 위치: ${parseFloat(issLocation.latitude).toFixed(1)}°, ${parseFloat(issLocation.longitude).toFixed(1)}°`,
          eventDate: new Date().toISOString(),
          peakTime: new Date().toISOString(),
          visibility: 'WORLDWIDE',
          magnitude: 'MEDIUM',
          isActive: true
        };
        selectedEvents = [issEvent, ...selectedEvents].slice(0, 5);
      }
      
      setEvents(selectedEvents);
    } catch (error) {
      console.error('천체 이벤트 조회 실패:', error);
    }
  };

  const handleCollectAstronomy = async () => {
    if (!confirm('NASA API로 천체 데이터를 수동 업데이트하시겠습니까?')) return;
    
    setCollectingAstronomy(true);
    try {
      await axios.post('/api/admin/scheduler/astronomy/manual');
      alert('천체 데이터 업데이트 완료! (NASA NeoWs/DONKI/Mars)');
      await fetchAstronomyEvents();
    } catch (error) {
      console.error('천체 데이터 수집 실패:', error);
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
  
  const getEventTypeBadgeColor = (eventType: string) => {
    switch (eventType) {
      case 'ASTEROID': return 'bg-orange-500/20 text-orange-300 border-orange-500/30';
      case 'SOLAR_FLARE': return 'bg-red-500/20 text-red-300 border-red-500/30';
      case 'GEOMAGNETIC_STORM': return 'bg-green-500/20 text-green-300 border-green-500/30';
      case 'ISS_LOCATION': return 'bg-blue-500/20 text-blue-300 border-blue-500/30';
      case 'MARS_WEATHER': return 'bg-red-500/20 text-red-300 border-red-500/30';
      case 'METEOR_SHOWER': return 'bg-purple-500/20 text-purple-300 border-purple-500/30';
      case 'LUNAR_ECLIPSE': return 'bg-indigo-500/20 text-indigo-300 border-indigo-500/30';
      case 'PLANET_CONJUNCTION': return 'bg-yellow-500/20 text-yellow-300 border-yellow-500/30';
      case 'COMET_OBSERVATION': return 'bg-pink-500/20 text-pink-300 border-pink-500/30';

      default: return 'bg-gray-500/20 text-gray-300 border-gray-500/30';
    }
  };

  if (loading) {
    return (
      <div className="bg-gradient-to-br from-indigo-900 via-purple-900 to-pink-900 rounded-lg p-6 text-white">
        <div className="animate-pulse">
          <div className="h-4 bg-white/20 rounded mb-4"></div>
          <div className="h-8 bg-white/20 rounded mb-2"></div>
          <div className="h-4 bg-white/20 rounded"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 날씨 관측 조건 */}
      <div className="bg-gradient-to-br from-indigo-900 via-purple-900 to-pink-900 rounded-lg p-6 text-white">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-xl font-bold flex items-center">
            🌟 별 관측 조건
          </h3>
          {weather && (
            <span className={`px-3 py-1 rounded-full text-sm font-medium ${getQualityColor(weather.observationQuality)}`}>
              {weather.observationQuality}
            </span>
          )}
        </div>

        {locationError && (
          <div className="bg-yellow-500/20 border border-yellow-500/30 rounded-lg p-3 mb-4">
            <p className="text-yellow-200 text-sm">{locationError}</p>
          </div>
        )}

        {weather ? (
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-gray-300">위치</span>
              <span className="font-medium">{weather.location}</span>
            </div>
            
            <div className="flex justify-between items-center">
              <span className="text-gray-300">구름량</span>
              <span className="font-medium">{weather.cloudCover.toFixed(0)}%</span>
            </div>
            
            <div className="flex justify-between items-center">
              <span className="text-gray-300">시정</span>
              <span className="font-medium">{weather.visibility.toFixed(1)}km</span>
            </div>
            
            <div className="flex justify-between items-center">
              <span className="text-gray-300">달의 위상</span>
              <span className="font-medium">{weather.moonPhase}</span>
            </div>
            
            <div className="mt-4 p-3 bg-white/10 rounded-lg">
              <p className="text-sm">{weather.recommendation}</p>
            </div>
            
            <div className="text-xs text-gray-400 text-right">
              업데이트: {weather.observationTime}
            </div>
          </div>
        ) : (
          <div className="text-center py-4">
            <p className="text-gray-300">날씨 데이터를 불러오는 중...</p>
          </div>
        )}
      </div>

      {/* 천체 이벤트 */}
      {events.length > 0 ? (
        <div className="bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 rounded-lg p-6 text-white">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-xl font-bold flex items-center">
              🌌 예정된 천체 이벤트
            </h3>
            {user?.role === 'ADMIN' && (
              <button
                onClick={handleCollectAstronomy}
                disabled={collectingAstronomy}
                className="bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-600 text-white px-3 py-1 rounded text-sm font-medium transition-colors flex items-center gap-1"
              >
                {collectingAstronomy ? (
                  <>
                    <div className="animate-spin w-3 h-3 border border-white border-t-transparent rounded-full"></div>
                    업데이트 중...
                  </>
                ) : (
                  <>
                    🔄 업데이트
                  </>
                )}
              </button>
            )}
          </div>
          
          <div className="space-y-3">
            {events.map((event) => (
              <div key={event.id} className="bg-white/10 rounded-lg p-4">
                <div className="flex items-start justify-between">
                  <div className="flex items-center space-x-2">
                    <span className="text-2xl">{getEventTypeIcon(event.eventType)}</span>
                    <div>
                      <h4 className="font-semibold">{event.title}</h4>
                      <p className="text-sm text-gray-300 mt-1">{event.description}</p>
                    </div>
                  </div>
                  <div className="text-right text-sm">
                    <div className="text-gray-300">
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
            ))}
          </div>
        </div>
      ) : (
        <div className="bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 rounded-lg p-6 text-white">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-xl font-bold flex items-center">
              🌌 예정된 천체 이벤트
            </h3>
            {user?.role === 'ADMIN' && (
              <button
                onClick={handleCollectAstronomy}
                disabled={collectingAstronomy}
                className="bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-600 text-white px-3 py-1 rounded text-sm font-medium transition-colors flex items-center gap-1"
              >
                {collectingAstronomy ? (
                  <>
                    <div className="animate-spin w-3 h-3 border border-white border-t-transparent rounded-full"></div>
                    업데이트 중...
                  </>
                ) : (
                  <>
                    🔄 업데이트
                  </>
                )}
              </button>
            )}
          </div>
          <div className="text-center py-4">
            <div className="text-4xl mb-2">🌌</div>
            <p className="text-gray-300">천체 데이터 로딩 중...</p>
            <p className="text-xs text-gray-400 mt-1">NASA API 연동</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default WeatherWidget;