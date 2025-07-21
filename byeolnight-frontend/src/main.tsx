import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { initializeClientIp } from './lib/axios'
import App from './App'
import './index.css'
import './styles/stellar-animations.css'
import './styles/tui-editor.css' // TUI Editor 커스텀 스타일

// 앱 시작 시 클라이언트 IP 초기화
initializeClientIp();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <AuthProvider>
      <App />
    </AuthProvider>
  </BrowserRouter>
)
