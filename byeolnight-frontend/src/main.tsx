import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { initializeClientIp } from './lib/axios'
import App from './App'
import './index.css'
import './styles/stellar-animations.css'
import 'react-quill/dist/quill.snow.css'
import './styles/quill-editor.css' // ReactQuill 통합 스타일

// 앱 시작 시 클라이언트 IP 초기화
initializeClientIp();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <AuthProvider>
      <App />
    </AuthProvider>
  </BrowserRouter>
)
