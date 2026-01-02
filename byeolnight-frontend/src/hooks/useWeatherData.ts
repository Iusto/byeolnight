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
    staleTime: 10 * 60 * 1000, // 10분
    gcTime: 15 * 60 * 1000, // 15분
    retry: 2,
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
    staleTime: 1 * 60 * 1000, // 1분
    gcTime: 10 * 60 * 1000, // 10분
    refetchInterval: 1 * 60 * 1000, // 1분마다 자동 갱신
    retry: 1,
  });
};
