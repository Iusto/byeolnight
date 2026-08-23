import { useCallback, useEffect, useState } from 'react';

export interface Coordinates {
  lat: number;
  lon: number;
}

type LocationMessageKey =
  | 'weather.location_error'
  | 'weather.location_timeout'
  | 'weather.location_not_supported';

interface LocationState {
  coordinates: Coordinates | null;
  isLoading: boolean;
  messageKey: LocationMessageKey | null;
  canRetry: boolean;
}

const SEOUL_COORDINATES: Coordinates = { lat: 37.5665, lon: 126.978 };
const LOCATION_CACHE_KEY = 'byeolnight.current-location.v1';
const LOCATION_CACHE_TTL_MS = 24 * 60 * 60 * 1000;

interface CachedLocation extends Coordinates {
  savedAt: number;
}

let pendingLocationRequest: Promise<Coordinates> | null = null;

function isValidCoordinates(coordinates: Coordinates): boolean {
  return Number.isFinite(coordinates.lat)
    && Number.isFinite(coordinates.lon)
    && coordinates.lat >= -90
    && coordinates.lat <= 90
    && coordinates.lon >= -180
    && coordinates.lon <= 180;
}

function readCachedLocation(): Coordinates | null {
  try {
    const raw = window.sessionStorage.getItem(LOCATION_CACHE_KEY);
    if (!raw) return null;

    const cached = JSON.parse(raw) as CachedLocation;
    const coordinates = { lat: cached.lat, lon: cached.lon };
    const isFresh = Number.isFinite(cached.savedAt)
      && Date.now() - cached.savedAt < LOCATION_CACHE_TTL_MS;

    if (!isFresh || !isValidCoordinates(coordinates)) {
      window.sessionStorage.removeItem(LOCATION_CACHE_KEY);
      return null;
    }

    return coordinates;
  } catch {
    // 사생활 보호 모드 등에서 세션 저장소 자체가 차단될 수 있다.
    return null;
  }
}

function cacheLocation(coordinates: Coordinates): void {
  try {
    // 민감한 위치 정보는 탭을 닫으면 사라지는 세션 저장소에만 보관한다.
    const cached: CachedLocation = {
      lat: Number(coordinates.lat.toFixed(3)),
      lon: Number(coordinates.lon.toFixed(3)),
      savedAt: Date.now(),
    };
    window.sessionStorage.setItem(LOCATION_CACHE_KEY, JSON.stringify(cached));
  } catch {
    // 저장소 사용이 제한된 브라우저에서도 현재 화면의 위치 조회는 계속 사용한다.
  }
}

function requestCurrentLocation(): Promise<Coordinates> {
  if (pendingLocationRequest) return pendingLocationRequest;

  pendingLocationRequest = new Promise<Coordinates>((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(
      (position) => resolve({
        lat: position.coords.latitude,
        lon: position.coords.longitude,
      }),
      reject,
      {
        // 도시 단위 관측 정보에는 고정밀 GPS가 불필요하며 저정밀 조회가 더 빠르고 안정적이다.
        enableHighAccuracy: false,
        timeout: 15_000,
        maximumAge: 30 * 60 * 1000,
      },
    );
  }).finally(() => {
    pendingLocationRequest = null;
  });

  return pendingLocationRequest;
}

function getLocationMessageKey(error: unknown): LocationMessageKey {
  if (typeof error === 'object'
    && error !== null
    && 'code' in error
    && error.code === 3) {
    return 'weather.location_timeout';
  }

  return 'weather.location_error';
}

/**
 * 오늘의 우주 카드가 사용할 좌표를 결정한다.
 * 실제 위치가 확정되기 전에는 서울 데이터를 조회하지 않고, 실패한 경우에만 서울을 사용한다.
 */
export function useCurrentLocation() {
  const [state, setState] = useState<LocationState>({
    coordinates: null,
    isLoading: true,
    messageKey: null,
    canRetry: true,
  });

  const locate = useCallback(async () => {
    if (!navigator.geolocation) {
      setState({
        coordinates: SEOUL_COORDINATES,
        isLoading: false,
        messageKey: 'weather.location_not_supported',
        canRetry: false,
      });
      return;
    }

    setState((previous) => ({
      ...previous,
      coordinates: null,
      isLoading: true,
      messageKey: null,
    }));

    try {
      const coordinates = await requestCurrentLocation();
      if (!isValidCoordinates(coordinates)) {
        throw new Error('잘못된 위치 좌표');
      }

      cacheLocation(coordinates);
      setState({
        coordinates,
        isLoading: false,
        messageKey: null,
        canRetry: true,
      });
    } catch (error) {
      setState({
        coordinates: SEOUL_COORDINATES,
        isLoading: false,
        messageKey: getLocationMessageKey(error),
        canRetry: true,
      });
    }
  }, []);

  useEffect(() => {
    const cachedLocation = readCachedLocation();
    if (cachedLocation) {
      setState({
        coordinates: cachedLocation,
        isLoading: false,
        messageKey: null,
        canRetry: true,
      });
      return;
    }

    void locate();
  }, [locate]);

  return {
    ...state,
    retry: locate,
  };
}
