import React, { useState, useEffect, useMemo, useCallback } from 'react';
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
  const { t } = useTranslation();
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
        setLocationError(t('weather.location_not_supported'));
        fetchWeatherData(37.5665, 126.9780);
        resolve(null);
        return;
      }

      const timeoutId = setTimeout(() => {
        setLocationError(t('weather.location_timeout'));
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
          setLocationError(t('weather.location_error'));
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
  
  // 언어 변경 시 이벤트 목록 업데이트 (번역 즉시 반영)
  useEffect(() => {
    if (events.length > 0) {
      setEvents(prevEvents => {
        const astronomyEvents = prevEvents.filter(event => event.eventType !== 'ISS_LOCATION');
        return updateEventsWithIss(astronomyEvents, issLocation);
      });
    }
  }, [t, issLocation]);

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
      
      // 날씨 데이터 조회 성공 시 ISS 관측 기회 업데이트
      fetchIssLocation(latitude, longitude);
    } catch (error: any) {
      const errorMessage = t('weather.weather_error');
      console.error('날씨 데이터 조회 실패:', sanitizeForLog(error.message || 'Unknown error'));
      setWeatherError(errorMessage);
    }
  }, [t]);

  const fetchIssLocation = useCallback(async (lat?: number, lon?: number) => {
    try {
      setIssError(null);
      const latitude = lat || 37.5665;
      const longitude = lon || 126.9780;
      
      console.log('ISS 관측 기회 업데이트 시작:', new Date().toLocaleTimeString());
      const response = await axios.get('/api/weather/iss', {
        params: { latitude, longitude }
      });
      const data = response.data;
      console.log('ISS 관측 데이터 수신 완료');
      
      if (data.message_key) {
        setIssLocation({
          latitude: latitude.toString(),
          longitude: longitude.toString(),
          timestamp: Date.now(),
          ...data
        });
        console.log('ISS 관측 기회 업데이트 완료');
      }
    } catch (error: any) {
      const errorMessage = t('weather.events_error');
      console.error('ISS 관측 정보 조회 실패:', sanitizeForLog(error.message || 'Unknown error'));
      setIssError(errorMessage);
    }
  }, [t]);

  const formatIssObservationMessage = (issData: any) => {
    if (!issData || issData.error) {
      return t('weather.iss_no_data');
    }
    
    // 새로운 ISS API 응답 형식 처리
    if (issData.message_key === 'iss.current_status') {
      return issData.friendly_message || t('weather.iss_current_status');
    }
    
    if (issData.message_key === 'iss.fallback') {
      return issData.friendly_message || t('weather.iss_fallback_info');
    }
    
    if (issData.message_key === 'iss.no_passes') {
      return issData.friendly_message || t('weather.iss_no_passes');
    }
    
    if (issData.message_key === 'iss.advanced_opportunity') {
      return issData.friendly_message || t('weather.iss_advanced_opportunity');
    }
    
    // 기존 형식 지원 (하위 호환성)
    if (issData.message_key === 'iss.detailed_opportunity') {
      const timeKey = issData.is_today ? 'weather.iss_today' : 
                     issData.is_tomorrow ? 'weather.iss_tomorrow' : 'weather.iss_future';
      
      return t('weather.iss_detailed_message', {
        time_desc: t(timeKey),
        time: issData.time,
        start_direction: t(`weather.compass_${issData.start_direction}`),
        end_direction: t(`weather.compass_${issData.end_direction}`),
        max_elevation: issData.max_elevation,
        duration: issData.duration_minutes,
        quality: t(`weather.visibility_${issData.visibility_quality}`)
      });
    }
    
    if (issData.message_key === 'iss.basic_opportunity') {
      const timeKey = issData.is_today ? 'weather.iss_today' : 
                     issData.is_tomorrow ? 'weather.iss_tomorrow' : 'weather.iss_future';
      
      return t('weather.iss_basic_message', {
        time_desc: t(timeKey),
        time: issData.time,
        direction: t(`weather.compass_${issData.direction}`),
        duration: issData.duration_minutes
      });
    }
    
    return issData.friendly_message || t('weather.iss_parse_error');
  };
  
  const updateEventsWithIss = (astronomyEvents: AstronomyEvent[], currentIssLocation: IssLocation | null) => {
    // ISS 데이터는 별도 섹션으로 표시하므로 천체 이벤트 목록에서 제외
    const filteredEvents = astronomyEvents.filter(event => 
      !event.eventType.includes('ISS') && 
      event.eventType !== 'ISS_OBSERVATION' &&
      event.eventType !== 'ISS_LOCATION'
    );
    return filteredEvents.slice(0, 5);
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
      
      // 최신순 정렬 후 타입별 최대 1개씩 선택 (모든 천체현상 포함)
      const sortedEvents = recentEvents.sort((a: AstronomyEvent, b: AstronomyEvent) => {
        const aDate = new Date(a.eventDate);
        const bDate = new Date(b.eventDate);
        return bDate.getTime() - aDate.getTime(); // 최신순
      });
      
      const eventsByType = sortedEvents.reduce((acc: Record<string, AstronomyEvent>, event: AstronomyEvent) => {
        // ISS 이벤트는 제외하고 다른 천체현상만 처리
        if (event.eventType.includes('ISS') || event.eventType === 'ISS_OBSERVATION' || event.eventType === 'ISS_LOCATION') {
          return acc;
        }
        
        const typeGroup = event.eventType.includes('ASTEROID') ? 'ASTEROID' :
                         event.eventType.includes('SOLAR_FLARE') ? 'SOLAR_FLARE' :
                         event.eventType.includes('GEOMAGNETIC') ? 'GEOMAGNETIC_STORM' :
                         event.eventType;
        
        if (!acc[typeGroup]) {
          acc[typeGroup] = event;
        }
        return acc;
      }, {});
      
      const astronomyEvents = Object.values(eventsByType);
      const finalEvents = updateEventsWithIss(astronomyEvents, null);
      setEvents(finalEvents);
    } catch (error: any) {
      const errorMessage = t('weather.events_error');
      console.error('천체 이벤트 조회 실패:', sanitizeForLog(error.message || 'Unknown error'));
      setEventsError(errorMessage);
    }
  }, [thirtyDaysAgo]);

  const handleCollectAstronomy = async () => {
    if (!confirm(t('weather.confirm_nasa_update'))) return;
    
    setCollectingAstronomy(true);
    try {
      await axios.post('/api/admin/scheduler/astronomy/manual');
      alert(t('weather.nasa_update_success'));
      await fetchAstronomyEvents();
    } catch (error: any) {
      console.error('천체 데이터 수집 실패:', sanitizeForLog(error.message || 'Unknown error'));
      alert(t('weather.nasa_update_failed'));
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
      case 'ISS_OBSERVATION': return '🛰️';
      case 'LUNAR_ECLIPSE': return '🌙';
      case 'SOLAR_ECLIPSE': return '☀️';
      case 'BLOOD_MOON': return '🔴';
      case 'BLUE_MOON': return '🔵';
      case 'SUPERMOON': return '🌕';
      default: return '⭐';
    }
  };
  
  const getEventTypeLabel = (eventType: string) => {
    // 이벤트 타입을 정규화하여 번역 키와 매칭
    let normalizedType = eventType;
    if (eventType.includes('ASTEROID')) normalizedType = 'ASTEROID';
    else if (eventType.includes('SOLAR')) normalizedType = 'SOLAR_FLARE';
    else if (eventType.includes('GEOMAGNETIC')) normalizedType = 'GEOMAGNETIC_STORM';
    else if (eventType.includes('METEOR')) normalizedType = 'METEOR_SHOWER';
    else if (eventType.includes('LUNAR')) normalizedType = 'LUNAR_ECLIPSE';
    else if (eventType.includes('PLANET')) normalizedType = 'PLANET_CONJUNCTION';
    
    const eventTypeKey = `weather.event_types.${normalizedType}` as const;
    return t(eventTypeKey, { defaultValue: t('weather.event_types.DEFAULT') });
  };
  
  // 달의 위상 번역
  const translateMoonPhase = (moonPhase: string) => {
    const phaseKey = `weather.moon_phases.${moonPhase}` as const;
    return t(phaseKey, { defaultValue: moonPhase });
  };
  
  // 관측 품질 번역
  const translateObservationQuality = (quality: string) => {
    const qualityKey = `weather.observation_quality.${quality}` as const;
    return t(qualityKey, { defaultValue: quality });
  };
  
  // 추천 메시지 번역
  const translateRecommendation = (recommendation: string) => {
    const recommendations: Record<string, string> = {
      'Observation conditions are difficult. Indoor activities are recommended.': t('weather.recommendations.poor'),
      'Fair observation conditions. Simple stargazing is possible.': t('weather.recommendations.fair'),
      'Good observation conditions. Constellation observation is recommended.': t('weather.recommendations.good'),
      'Excellent observation conditions. Telescope observation is recommended.': t('weather.recommendations.excellent'),
      'Unable to check weather information.': t('weather.recommendations.unknown', { defaultValue: recommendation })
    };
    return recommendations[recommendation] || recommendation;
  };
  
  // 이벤트 설명 번역 (리팩토링된 버전)
  const translateEventDescription = (description: string) => {
    // 패턴 매칭 함수들
    const matchSolarFlare = (desc: string) => {
      const patterns = [
        /Solar flare class ([A-Z]\d+\.?\d*) occurred on (\d{4}-\d{2}-\d{2})\. Peak time: (\d{2}:\d{2}) \(UTC\)/,
        /Solar Flare Class ([A-Z]\d+\.?\d*)/i,
        /태양 플레어.*등급.*([A-Z]\d+\.?\d*)/,
        /태양플레어.*등급.*([A-Z]\d+\.?\d*)/i
      ];
      
      for (const pattern of patterns) {
        const match = desc.match(pattern);
        if (match) {
          return {
            class: match[1],
            date: match[2] || new Date().toISOString().split('T')[0],
            time: match[3] || '00:00'
          };
        }
      }
      return null;
    };
    
    const matchGeomagneticStorm = (desc: string) => {
      const patterns = [
        /Geomagnetic storm occurred on (\d{4}-\d{2}-\d{2})\. Kp index: ([^.]+)/,
        /Geomagnetic Storm/i,
        /지자기.*폭풍/,
        /지자기폭풍/i
      ];
      
      for (const pattern of patterns) {
        const match = desc.match(pattern);
        if (match) {
          return {
            date: match[1] || new Date().toISOString().split('T')[0],
            kp: match[2] || 'Unknown'
          };
        }
      }
      return null;
    };
    
    const matchIssPosition = (desc: string) => {
      const patterns = [
        /International Space Station current position: ([^,]+), ([^°]+)°/,
        /국제우주정거장.*위치.*: ([^,]+), ([^°]+)°/,
        /ISS.*position.*: ([^,]+), ([^°]+)°/i
      ];
      
      for (const pattern of patterns) {
        const match = desc.match(pattern);
        if (match) {
          return {
            lat: parseFloat(match[1]).toFixed(1),
            lon: parseFloat(match[2]).toFixed(1)
          };
        }
      }
      return null;
    };
    
    // 번역 키 매핑 객체
    const eventTypeKeywords = {
      solarFlare: ['solar flare', '태양', '플레어'],
      geomagneticStorm: ['geomagnetic', '지자기', '폭풍'],
      asteroid: ['asteroid', '소행성'],
      meteorShower: ['meteor', '유성우', 'shower'],
      lunarEclipse: ['lunar eclipse', '월식'],
      solarEclipse: ['solar eclipse', '일식'],
      supermoon: ['supermoon', '슈퍼문'],
      planetConjunction: ['conjunction', '합'],
      comet: ['comet', '혜성']
    };
    
    // 패턴 매칭 시도
    const solarFlareMatch = matchSolarFlare(description);
    if (solarFlareMatch) {
      return t('weather.event_descriptions.solar_flare', solarFlareMatch);
    }
    
    const geomagneticMatch = matchGeomagneticStorm(description);
    if (geomagneticMatch) {
      return t('weather.event_descriptions.geomagnetic_storm', geomagneticMatch);
    }
    
    const issMatch = matchIssPosition(description);
    if (issMatch) {
      return t('weather.event_descriptions.iss_position', issMatch);
    }
    
    // 소행성 패턴 매칭
    const asteroidMatch = description.match(/asteroid.*([\d.]+).*km.*([\d.]+).*m/i);
    if (asteroidMatch) {
      return t('weather.event_descriptions.asteroid', {
        name: 'NEO',
        date: new Date().toISOString().split('T')[0],
        distance: asteroidMatch[1],
        size: asteroidMatch[2]
      });
    }
    
    // 키워드 기반 번역 (패턴 매칭 실패 시)
    const lowerDesc = description.toLowerCase();
    
    if (eventTypeKeywords.solarFlare.some(keyword => lowerDesc.includes(keyword))) {
      const classMatch = description.match(/([A-Z]\d+\.?\d*)/i);
      const dateMatch = description.match(/(\d{4}-\d{2}-\d{2})/);
      const timeMatch = description.match(/(\d{2}:\d{2})/);
      
      return t('weather.event_descriptions.solar_flare', {
        class: classMatch ? classMatch[1] : 'Unknown',
        date: dateMatch ? dateMatch[1] : new Date().toISOString().split('T')[0],
        time: timeMatch ? timeMatch[1] : '00:00'
      });
    }
    
    if (eventTypeKeywords.geomagneticStorm.some(keyword => lowerDesc.includes(keyword))) {
      const kpMatch = description.match(/Kp[^\d]*([\d.]+)/i) || description.match(/지수[^\d]*([\d.]+)/);
      const dateMatch = description.match(/(\d{4}-\d{2}-\d{2})/);
      
      return t('weather.event_descriptions.geomagnetic_storm', {
        date: dateMatch ? dateMatch[1] : new Date().toISOString().split('T')[0],
        kp: kpMatch ? kpMatch[1] : 'Unknown'
      });
    }
    
    // 기본값: 원본 반환
    return description;
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
                <span className="text-blue-200 font-medium">{t('weather.loading_weather')}</span>
              </div>
              <p className="text-gray-300 text-sm">{t('weather.analyzing_conditions')}</p>
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
                <span className="text-purple-200 font-medium">{t('weather.loading_events')}</span>
              </div>
              <p className="text-gray-300 text-sm mb-2">{t('weather.collecting_space_data')}</p>
              <div className="flex items-center justify-center gap-3 text-xs text-gray-400 flex-wrap">
                <span>🪨 {t('weather.loading_events_list.asteroid')}</span>
                <span>☀️ {t('weather.loading_events_list.solar_flare')}</span>
                <span>🛰️ {t('weather.loading_events_list.iss')}</span>
                <span>🌍 {t('weather.loading_events_list.geomagnetic')}</span>
                <span>🌙 {t('weather.loading_events_list.lunar_eclipse')}</span>
                <span>🔴 {t('weather.loading_events_list.blood_moon')}</span>
                <span>☄️ {t('weather.loading_events_list.meteor_shower')}</span>
                <span>🌕 {t('weather.loading_events_list.supermoon')}</span>
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
            🌟 {t('weather.star_observation')}
            {requestingLocation && (
              <div className="animate-spin w-4 h-4 sm:w-5 sm:h-5 border-2 border-white border-t-transparent rounded-full"></div>
            )}
          </h3>
          {weather && (
            <span className={`px-3 py-1 rounded-full text-sm font-semibold ${getQualityColor(weather.observationQuality)} shadow-lg`}>
              {translateObservationQuality(weather.observationQuality)}
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
              {t('weather.retry_location')}
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
              {t('weather.retry')}
            </button>
          </div>
        ) : weather ? (
          <div className="space-y-4">
            <div className="bg-white/10 rounded-lg p-4 backdrop-blur-sm border border-white/20">
              {/* 날씨 정보 그리드 */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-4">
                <div className="flex items-center justify-between p-2 bg-white/5 rounded-lg">
                  <span className="text-gray-300 text-sm flex items-center gap-2">
                    <span>📍</span>
                    <span>{t('weather.location')}</span>
                  </span>
                  <span className="font-semibold text-white text-sm break-words text-right max-w-[120px]">{weather.location}</span>
                </div>
                <div className="flex items-center justify-between p-2 bg-white/5 rounded-lg">
                  <span className="text-gray-300 text-sm flex items-center gap-2">
                    <span>☁️</span>
                    <span>{t('weather.cloud_cover')}</span>
                  </span>
                  <span className="font-semibold text-white text-sm">{weather.cloudCover.toFixed(0)}%</span>
                </div>
                <div className="flex items-center justify-between p-2 bg-white/5 rounded-lg">
                  <span className="text-gray-300 text-sm flex items-center gap-2">
                    <span>👁️</span>
                    <span>{t('weather.visibility')}</span>
                  </span>
                  <span className="font-semibold text-white text-sm">{weather.visibility.toFixed(1)}km</span>
                </div>
                <div className="flex items-center justify-between p-2 bg-white/5 rounded-lg">
                  <span className="text-gray-300 text-sm flex items-center gap-2">
                    <span>🌙</span>
                    <span>{t('weather.moon_phase')}</span>
                  </span>
                  <span className="font-semibold text-white text-sm break-words text-right max-w-[80px]">{translateMoonPhase(weather.moonPhase)}</span>
                </div>
              </div>
              
              {/* 추천 사항 */}
              <div className="p-3 bg-gradient-to-r from-white/5 to-white/10 rounded-lg border border-white/10">
                <div className="flex items-start gap-2 mb-2">
                  <span className="text-lg">📝</span>
                  <span className="text-sm font-medium text-white">{t('weather.observation_recommendation')}</span>
                </div>
                <p className="text-sm text-gray-200 leading-relaxed break-words pl-7">{translateRecommendation(weather.recommendation)}</p>
              </div>
              
              {/* 업데이트 시간 */}
              <div className="flex items-center justify-center gap-2 text-xs text-gray-400 mt-3 p-2 bg-white/5 rounded-lg">
                <span>⏰</span>
                <span>{t('weather.updated')}: {weather.observationTime}</span>
              </div>
            </div>
          </div>
        ) : (
          <div className="text-center py-8">
            <div className="animate-pulse">
              <div className="text-4xl mb-4">🌌</div>
              <div className="flex items-center justify-center gap-3 mb-2">
                <div className="animate-spin w-5 h-5 border-2 border-blue-400 border-t-transparent rounded-full"></div>
                <p className="text-blue-200 font-medium">{t('weather.loading_weather_data')}</p>
              </div>
              <p className="text-gray-300 text-sm">{t('weather.calculating_conditions')}</p>
            </div>
          </div>
        )}
      </div>

      {/* 천체 이벤트 */}
      <div className="bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 rounded-xl p-6 text-white shadow-2xl border border-blue-500/20">
        <div className="flex items-center justify-between mb-4 sm:mb-6">
          <h3 className="text-lg sm:text-2xl font-bold flex items-center gap-2">
            🌌 {t('weather.recent_astronomy_events')}
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
                  {t('weather.updating')}
                </>
              ) : (
                <>
                  {t('weather.nasa_update')}
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
              {t('weather.retry')}
            </button>
          </div>
        ) : events.length > 0 ? (
          <div className="space-y-3">
            {events.map((event, index) => (
              <div key={`${event.eventType}-${index}`} className="bg-white/10 rounded-lg p-4 backdrop-blur-sm border border-white/20 hover:bg-white/15 transition-all duration-200">
                {/* 모바일: 세로 배치, 데스크톱: 가로 배치 */}
                <div className="flex flex-col gap-3">
                  {/* 상단: 아이콘 + 제목 + 라벨 */}
                  <div className="flex items-start gap-3">
                    <span className="text-2xl flex-shrink-0 mt-1">{getEventTypeIcon(event.eventType)}</span>
                    <div className="flex-1 min-w-0">
                      <h4 className="font-semibold text-white text-base mb-2 break-words">{event.title}</h4>
                      <div className="flex flex-wrap gap-2 mb-3">
                        <span className="inline-block text-xs px-3 py-1 bg-blue-500/30 text-blue-200 rounded-full border border-blue-400/30 whitespace-nowrap">
                          {getEventTypeLabel(event.eventType)}
                        </span>
                        <span className="inline-block text-xs px-3 py-1 bg-gray-500/30 text-gray-300 rounded-full border border-gray-400/30 whitespace-nowrap">
                          {new Date(event.eventDate).toLocaleDateString('ko-KR')}
                        </span>
                        <span className="inline-block text-xs px-3 py-1 bg-purple-500/30 text-purple-200 rounded-full border border-purple-400/30 whitespace-nowrap">
                          {new Date(event.peakTime).toLocaleTimeString('ko-KR', { 
                            hour: '2-digit', 
                            minute: '2-digit' 
                          })}
                        </span>
                      </div>
                    </div>
                  </div>
                  
                  {/* 하단: 설명 */}
                  <div className="pl-11">
                    <p className="text-sm text-gray-300 leading-relaxed break-words">{translateEventDescription(event.description)}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8">
            <div className="text-4xl mb-4">🌌</div>
            <div className="flex items-center justify-center gap-3 mb-2">
              <div className="animate-spin w-5 h-5 border-2 border-purple-400 border-t-transparent rounded-full"></div>
              <p className="text-purple-200 font-medium">{t('weather.loading_astronomy_events')}</p>
            </div>
            <p className="text-gray-300 text-sm mb-2">{t('weather.nasa_api_data')}</p>
            <div className="flex items-center justify-center gap-3 text-xs text-gray-400 flex-wrap">
              <span>🪨 NeoWs</span>
              <span>☀️ DONKI</span>
              <span>🛰️ ISS</span>
            </div>
          </div>
        )}

      </div>

      {/* ISS 관측 정보 (별도 섹션) */}
      <div className="bg-gradient-to-br from-gray-900 via-slate-900 to-gray-900 rounded-xl p-6 text-white shadow-2xl border border-gray-500/20">
        <div className="flex items-center justify-between mb-4 sm:mb-6">
          <h3 className="text-lg sm:text-2xl font-bold flex items-center gap-2">
            🛰️ {t('weather.iss_observation_opportunity')}
          </h3>
        </div>

        {issError ? (
          <div className="p-4 bg-red-500/20 border border-red-400/30 rounded-lg backdrop-blur-sm">
            <p className="text-red-200 text-sm mb-2">{issError}</p>
            <button
              onClick={fetchIssLocation}
              className="bg-gradient-to-r from-red-600 to-red-700 hover:from-red-700 hover:to-red-800 text-white px-4 py-2 rounded-lg text-sm font-semibold transition-all duration-200 shadow-lg hover:shadow-xl"
            >
              {t('weather.iss_retry')}
            </button>
          </div>
        ) : issLocation ? (
          <div className="bg-white/10 rounded-lg p-4 backdrop-blur-sm border border-white/20">
            <div className="flex items-start gap-3">
              <span className="text-2xl flex-shrink-0 mt-1">🛰️</span>
              <div className="flex-1 min-w-0">
                <h4 className="font-semibold text-white text-base mb-3">{t('weather.iss_current_status')}</h4>
                <p className="text-sm text-gray-300 leading-relaxed break-words mb-4">
                  {formatIssObservationMessage(issLocation)}
                </p>
                
                {/* ISS 상세 정보 */}
                {issLocation.current_altitude_km && (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div className="flex items-center justify-between p-2 bg-white/5 rounded-lg">
                      <span className="text-gray-300 text-sm flex items-center gap-2">
                        <span>🚀</span>
                        <span>{t('weather.iss_altitude')}</span>
                      </span>
                      <span className="font-semibold text-white text-sm">{issLocation.current_altitude_km}km</span>
                    </div>
                    {issLocation.current_velocity_kmh && (
                      <div className="flex items-center justify-between p-2 bg-white/5 rounded-lg">
                        <span className="text-gray-300 text-sm flex items-center gap-2">
                          <span>⚡</span>
                          <span>{t('weather.iss_velocity')}</span>
                        </span>
                        <span className="font-semibold text-white text-sm">{issLocation.current_velocity_kmh}km/h</span>
                      </div>
                    )}
                    {issLocation.current_distance_km && (
                      <div className="flex items-center justify-between p-2 bg-white/5 rounded-lg">
                        <span className="text-gray-300 text-sm flex items-center gap-2">
                          <span>📏</span>
                          <span>{t('weather.iss_distance')}</span>
                        </span>
                        <span className="font-semibold text-white text-sm">{issLocation.current_distance_km}km</span>
                      </div>
                    )}
                    {issLocation.is_visible_now !== undefined && (
                      <div className="flex items-center justify-between p-2 bg-white/5 rounded-lg">
                        <span className="text-gray-300 text-sm flex items-center gap-2">
                          <span>👁️</span>
                          <span>{t('weather.iss_visibility')}</span>
                        </span>
                        <span className={`font-semibold text-sm ${
                          issLocation.is_visible_now ? 'text-green-400' : 'text-red-400'
                        }`}>
                          {issLocation.is_visible_now ? t('weather.iss_visible') : t('weather.iss_not_visible')}
                        </span>
                      </div>
                    )}
                  </div>
                )}
                
                {/* 관측 팁 */}
                {issLocation.observation_tip && (
                  <div className="mt-4 p-3 bg-gradient-to-r from-white/5 to-white/10 rounded-lg border border-white/10">
                    <div className="flex items-start gap-2">
                      <span className="text-lg">💡</span>
                      <div>
                        <span className="text-sm font-medium text-white">{t('weather.observation_tip')}</span>
                        <p className="text-sm text-gray-200 leading-relaxed break-words mt-1">{issLocation.observation_tip}</p>
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
            <div className="flex items-center justify-center gap-3 mb-2">
              <div className="animate-spin w-5 h-5 border-2 border-gray-400 border-t-transparent rounded-full"></div>
              <p className="text-gray-200 font-medium">{t('weather.loading_iss_data')}</p>
            </div>
            <p className="text-gray-300 text-sm">{t('weather.fetching_iss_position')}</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default WeatherWidget;