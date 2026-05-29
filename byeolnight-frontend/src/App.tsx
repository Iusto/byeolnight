// App.tsx
import './i18n'; // i18n 초기화
import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { Layout, ProtectedRoute } from './components/layout';
import { LoadingSpinner } from './components/common';

// 코드 스플리팅: 각 페이지를 lazy 로드하여 초기 번들 크기 축소
const Home = lazy(() => import('./pages/Home'));
const LoginPage = lazy(() => import('./pages/Login'));
const SignupPage = lazy(() => import('./pages/Signup'));
const ProfilePage = lazy(() => import('./pages/ProfilePage'));
const Profile = lazy(() => import('./pages/Profile'));
const PostList = lazy(() => import('./pages/PostList'));
const PostCreate = lazy(() => import('./pages/PostCreate'));
const PostDetail = lazy(() => import('./pages/PostDetail'));
const PostEdit = lazy(() => import('./pages/PostEdit'));
const PasswordReset = lazy(() => import('./pages/PasswordReset'));
const AdminUserPage = lazy(() => import('./pages/AdminUserPage'));
const Unauthorized = lazy(() => import('./pages/Unauthorized'));
const Certificates = lazy(() => import('./pages/Certificates'));
const StellaShop = lazy(() => import('./pages/StellaShop'));
const PostReport = lazy(() => import('./pages/PostReport'));
const PointHistory = lazy(() => import('./pages/PointHistory'));
const SuggestionList = lazy(() => import('./pages/SuggestionList'));
const SuggestionCreate = lazy(() => import('./pages/SuggestionCreate'));
const SuggestionDetail = lazy(() => import('./pages/SuggestionDetail'));
const SuggestionEdit = lazy(() => import('./pages/SuggestionEdit'));
const MessagesPage = lazy(() => import('./pages/MessagesPage'));
const OAuthCallback = lazy(() => import('./pages/OAuthCallback'));
const OAuthNicknameSetup = lazy(() => import('./pages/OAuthNicknameSetup'));
const OAuthRecover = lazy(() => import('./pages/OAuthRecover'));
const NotFound = lazy(() => import('./pages/NotFound'));

// 구 경로 /posts/write → /posts/new 리다이렉트 (쿼리스트링 유지)
const PostWriteRedirect = () => {
  const { search } = useLocation();
  return <Navigate to={`/posts/new${search}`} replace />;
};

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
    <Suspense fallback={<LoadingSpinner />}>
      <Routes>
        {/* 독립적인 페이지들 (Layout 없음) */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/password-reset" element={<PasswordReset />} />
        <Route path="/oauth/callback" element={<OAuthCallback />} />
        <Route path="/oauth/setup-nickname" element={<OAuthNicknameSetup />} />
        <Route path="/oauth/recover" element={<OAuthRecover />} />

        {/* Layout이 적용되는 페이지들 */}
        <Route element={<Layout />}>
          <Route path="/" element={<Home />} />

          {/* 공개 페이지 */}
          <Route path="/posts" element={<PostList />} />
          <Route path="/posts/:id" element={<PostDetail />} />
          <Route path="/certificates" element={<Certificates />} />
          <Route path="/shop" element={<StellaShop />} />
          <Route path="/suggestions" element={<SuggestionList />} />
          <Route path="/suggestions/:id" element={<SuggestionDetail />} />
          <Route path="/unauthorized" element={<Unauthorized />} />

          {/* 로그인 필요 페이지 */}
          <Route path="/me" element={
            <ProtectedRoute><ProfilePage /></ProtectedRoute>
          } />
          <Route path="/profile" element={
            <ProtectedRoute><Profile /></ProtectedRoute>
          } />
          <Route path="/messages" element={
            <ProtectedRoute><MessagesPage /></ProtectedRoute>
          } />
          <Route path="/points" element={
            <ProtectedRoute><PointHistory /></ProtectedRoute>
          } />
          <Route path="/posts/new" element={
            <ProtectedRoute><PostCreate /></ProtectedRoute>
          } />
          <Route path="/posts/write" element={<PostWriteRedirect />} />
          <Route path="/posts/:id/edit" element={
            <ProtectedRoute><PostEdit /></ProtectedRoute>
          } />
          <Route path="/posts/:id/report" element={
            <ProtectedRoute><PostReport /></ProtectedRoute>
          } />
          <Route path="/suggestions/new" element={
            <ProtectedRoute><SuggestionCreate /></ProtectedRoute>
          } />
          <Route path="/suggestions/:id/edit" element={
            <ProtectedRoute><SuggestionEdit /></ProtectedRoute>
          } />

          {/* 관리자 전용 페이지 */}
          <Route path="/admin/users" element={
            <ProtectedRoute requireAdmin>
              <AdminUserPage />
            </ProtectedRoute>
          } />

          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </Suspense>
  )
}

export default App
