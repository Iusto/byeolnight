import React, { useState, useEffect } from 'react';
import axios from 'axios';

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

const WeatherWidget: React.FC = () => {
  const [weather, setWeather] = useState<WeatherData | null>(null);
  const [events, setEvents] = useState<AstronomyEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [locationError, setLocationError] = useState<string | null>(null);

  useEffect(() => {
    getCurrentLocation();
    fetchAstronomyEvents();
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

  const fetchAstronomyEvents = async () => {
    try {
      console.log('천체 이벤트 요청 시작');
      const response = await axios.get('/api/weather/events');
      console.log('천체 이벤트 응답:', response.data);
      setEvents(response.data.slice(0, 3)); // 최대 3개만 표시
    } catch (error) {
      console.error('천체 이벤트 조회 실패:', error);
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
      case 'METEOR_SHOWER': return '☄️';
      case 'ECLIPSE': return '🌙';
      case 'PLANET_CONJUNCTION': return '🪐';
      default: return '⭐';
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
          <h3 className="text-xl font-bold mb-4 flex items-center">
            🌌 예정된 천체 이벤트
          </h3>
          
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
          <h3 className="text-xl font-bold mb-4 flex items-center">
            🌌 예정된 천체 이벤트
          </h3>
          <div className="text-center py-4">
            <p className="text-gray-300">천체 이벤트를 불러오는 중...</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default WeatherWidget;