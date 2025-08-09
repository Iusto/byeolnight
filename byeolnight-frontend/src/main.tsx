console.log('🚨🚨🚨 main.tsx 파일 로드됨!')

import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import App from './App'
import './index.css'
import './styles/stellar-animations.css'
import './styles/tui-editor.css' // TUI Editor 커스텀 스타일

console.log('🚨🚨🚨 main.tsx import 완료!')

// 정적 파일 경로 확인 함수
const isStaticFilePath = (pathname: string): boolean => {
  return [
    '/sitemap.xml',
    '/robots.txt',
    '/favicon.ico'
  ].includes(pathname) || pathname.startsWith('/sitemap-');
};

console.log('🚨 현재 경로:', window.location.pathname)
console.log('🚨 정적 파일 체크:', isStaticFilePath(window.location.pathname))

// 현재 경로가 정적 파일 경로인 경우 React 앱을 렌더링하지 않음
if (isStaticFilePath(window.location.pathname)) {
  console.log('🚨 정적 파일 경로 감지:', window.location.pathname);
  // 아무것도 렌더링하지 않고 종료
} else {
  console.log('🚨 React 앱 렌더링 시작!')
  ReactDOM.createRoot(document.getElementById('root')!).render(
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  );
  console.log('🚨 React 앱 렌더링 완료!')
}
