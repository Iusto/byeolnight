import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient } from '@tanstack/react-query'
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client'
import { createSyncStoragePersister } from '@tanstack/query-sync-storage-persister'
import { AuthProvider } from './contexts/AuthContext'
import { ToastProvider } from './contexts/ToastContext'
import { ConfirmProvider } from './contexts/ConfirmContext'
import App from './App'
import './index.css'
import './styles/stellar-animations.css'
import './styles/tui-editor.css'

// React Query 설정
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5분 - 데이터가 5분간 신선(fresh)하다고 간주
      gcTime: 10 * 60 * 1000, // 10분 - 캐시 유지 시간 (구 cacheTime)
      retry: 1, // 실패 시 1번만 재시도
      refetchOnWindowFocus: false, // 윈도우 포커스 시 자동 재요청 비활성화
    },
  },
})

// localStorage 기반 캐시 영속화 - 새로고침 시 캐시 유지
const persister = createSyncStoragePersister({
  storage: window.localStorage,
})

// 정적 파일 경로 확인 함수
const isStaticFilePath = (pathname: string): boolean => {
  return [
    '/sitemap.xml',
    '/robots.txt',
    '/favicon.ico'
  ].includes(pathname) || pathname.startsWith('/sitemap-');
};

// 현재 경로가 정적 파일 경로인 경우 React 앱을 렌더링하지 않음
if (isStaticFilePath(window.location.pathname)) {
  // 아무것도 렌더링하지 않고 종료
} else {
  ReactDOM.createRoot(document.getElementById('root')!).render(
    <PersistQueryClientProvider client={queryClient} persistOptions={{
      persister,
      dehydrateOptions: {
        shouldDehydrateQuery: () => true, // refetchInterval 포함 모든 쿼리 영속화
      },
    }}>
      <BrowserRouter>
        <AuthProvider>
          <ToastProvider>
            <ConfirmProvider>
              <App />
            </ConfirmProvider>
          </ToastProvider>
        </AuthProvider>
      </BrowserRouter>
    </PersistQueryClientProvider>
  );
}
