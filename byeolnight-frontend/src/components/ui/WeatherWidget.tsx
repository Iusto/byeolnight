import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useWeatherObservation } from '../../hooks/useWeatherData';
import IssTracker from './IssTracker';

const WeatherWidget: React.FC = () => {
  const { t } = useTranslation();

  // 위치 정보 상태
  const [coordinates, setCoordinates] = useState<{ lat: number; lon: number }>({
    lat: 37.5665,
    lon: 126.9780,
  });
  const [locationLoading, setLocationLoading] = useState(true);

  // React Query hooks
  const {
    data: weather,
    isLoading: weatherLoading,
    error: weatherError
  } = useWeatherObservation(coordinates.lat, coordinates.lon);

  // 위치 정보 가져오기 (한 번만 실행)
  useEffect(() => {
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

    const loadPosition = async () => {
      setLocationLoading(true);
      const position = await getCurrentPosition();
      if (position) {
        setCoordinates({
          lat: position.coords.latitude,
          lon: position.coords.longitude,
        });
      }
      setLocationLoading(false);
    };

    loadPosition();
  }, []);


  const getMoonPhaseIcon = (moonPhase: string) => {
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

  // 로딩 상태 (위치 정보나 날씨 데이터 로딩 중)
  const loading = locationLoading || weatherLoading;

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
            <p className="text-red-200 text-sm">{t('weather.weather_error')}</p>
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

      {/* ISS 실시간 추적 */}
      <IssTracker latitude={coordinates.lat} longitude={coordinates.lon} />
    </div>
  );
};

export default WeatherWidget;