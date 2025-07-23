import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { initializeClientIp } from './lib/axios'
import App from './App'
import './index.css'
import './styles/stellar-animations.css'
import './styles/tui-editor.css' // TUI Editor 커스텀 스타일

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
  console.log('정적 파일 경로 감지:', window.location.pathname);
  // 아무것도 렌더링하지 않고 종료
} else {
  // 앱 시작 시 클라이언트 IP 초기화
  initializeClientIp();

  ReactDOM.createRoot(document.getElementById('root')!).render(
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  );
}
