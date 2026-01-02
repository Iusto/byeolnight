import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

// 날씨 데이터 인터페이스
export interface WeatherData {
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

export interface AstronomyEvent {
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

export interface IssData {
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

// 날씨 관측 조건 조회
export const useWeatherObservation = (latitude: number, longitude: number) => {
  return useQuery<WeatherData>({
    queryKey: ['weather', 'observation', latitude, longitude],
    queryFn: async () => {
      const response = await axios.get('/api/weather/observation', {
        params: { latitude, longitude },
        timeout: 10000,
      });
      return response.data;
    },
    staleTime: 10 * 60 * 1000, // 10분 - 백엔드 Cache-Control과 동일
    gcTime: 15 * 60 * 1000, // 15분 - 백엔드 Redis TTL과 비슷
    retry: 2,
  });
};

// 천체 이벤트 조회 (매일 오전 9시 수집, 날짜 기반 캐싱)
export const useAstronomyEvents = () => {
  // 오늘 날짜를 queryKey에 포함 (날짜가 바뀌면 자동으로 새 쿼리)
  const today = new Date().toISOString().split('T')[0]; // 'YYYY-MM-DD' 형식

  return useQuery<AstronomyEvent[]>({
    queryKey: ['weather', 'events', today],
    queryFn: async () => {
      const response = await axios.get('/api/weather/events', {
        timeout: 10000,
      });
      return response.data || [];
    },
    staleTime: Infinity, // 같은 날짜 내에서는 절대 stale하지 않음
    gcTime: 24 * 60 * 60 * 1000, // 24시간 - 다음날까지 캐시 유지
  });
};

// ISS 관측 기회 조회 (실시간 업데이트)
export const useIssObservation = (latitude: number, longitude: number) => {
  return useQuery<IssData>({
    queryKey: ['weather', 'iss', latitude, longitude],
    queryFn: async () => {
      const response = await axios.get('/api/weather/iss', {
        params: { latitude, longitude },
        timeout: 10000,
      });
      return response.data;
    },
    staleTime: 1 * 60 * 1000, // 1분 - 자주 업데이트
    gcTime: 10 * 60 * 1000, // 10분
    refetchInterval: 1 * 60 * 1000, // 1분마다 자동 갱신
    retry: 1,
  });
};
