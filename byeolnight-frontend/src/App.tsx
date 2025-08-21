// App.tsx
import './i18n'; // i18n 초기화
import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import Home from './pages/Home'
import LoginPage from './pages/Login'
import SignupPage from './pages/Signup'
import ProfilePage from './pages/ProfilePage'
import Profile from './pages/Profile'
import PostList from './pages/PostList'
import PostCreate from './pages/PostCreate'
import PostDetail from './pages/PostDetail'
import PostEdit from './pages/PostEdit';
import PasswordReset from './pages/PasswordReset';
import AdminUserPage from './pages/AdminUserPage';
import Unauthorized from './pages/Unauthorized';
import Certificates from './pages/Certificates';
import StellaShop from './pages/StellaShop';
import PostReport from './pages/PostReport';
import PointHistory from './pages/PointHistory';
import SuggestionList from './pages/SuggestionList';
import SuggestionCreate from './pages/SuggestionCreate';
import SuggestionDetail from './pages/SuggestionDetail';
import SuggestionEdit from './pages/SuggestionEdit';
import MessagesPage from './pages/MessagesPage';
import OAuthCallback from './pages/OAuthCallback';
import OAuthNicknameSetup from './pages/OAuthNicknameSetup';
import OAuthRecover from './pages/OAuthRecover';

// 정적 파일 경로 확인 함수
const isStaticFilePath = (pathname: string): boolean => {
  return [
    '/sitemap.xml',
    '/robots.txt',
    '/favicon.ico'
  ].includes(pathname) || pathname.startsWith('/sitemap-');
};

function App() {
  // 현재 경로가 정적 파일 경로인 경우 아무것도 렌더링하지 않음
  if (isStaticFilePath(window.location.pathname)) {
    return null;
  }
  
  return (
    <Routes>
      {/* 독립적인 페이지들 (Layout 없음) */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/reset-password" element={<PasswordReset />} />
      <Route path="/oauth/callback" element={<OAuthCallback />} />
      <Route path="/oauth/setup-nickname" element={<OAuthNicknameSetup />} />
      <Route path="/oauth/recover" element={<OAuthRecover />} />
      
      {/* Layout이 적용되는 페이지들 */}
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/me" element={<ProfilePage />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/posts" element={<PostList />} />
        <Route path="/posts/new" element={<PostCreate />} />
        <Route path="/posts/write" element={<PostCreate />} />
        <Route path="/posts/:id/edit" element={<PostEdit />} />
        <Route path="/posts/:id/report" element={<PostReport />} />
        <Route path="/posts/:id" element={<PostDetail />} />
        <Route path="/admin/users" element={
          <ProtectedRoute requireAdmin>
            <AdminUserPage />
          </ProtectedRoute>
        } />


        <Route path="/certificates" element={<Certificates />} />
        <Route path="/shop" element={<StellaShop />} />
        <Route path="/points" element={<PointHistory />} />
        <Route path="/suggestions" element={<SuggestionList />} />
        <Route path="/suggestions/new" element={<SuggestionCreate />} />
        <Route path="/suggestions/:id/edit" element={<SuggestionEdit />} />
        <Route path="/suggestions/:id" element={<SuggestionDetail />} />
        <Route path="/messages" element={<MessagesPage />} />
        <Route path="/unauthorized" element={<Unauthorized />} />
      </Route>
    </Routes>
  )
}

export default App