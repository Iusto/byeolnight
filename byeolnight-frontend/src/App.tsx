// App.tsx
import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout';
import Home from './pages/Home'
import LoginPage from './pages/Login'
import SignupPage from './pages/Signup'
import MePage from './pages/Me'
import Profile from './pages/Profile'
import PostList from './pages/PostList'
import PostCreate from './pages/PostCreate'
import PostDetail from './pages/PostDetail'
import PostEdit from './pages/PostEdit';
import PasswordReset from './pages/PasswordReset';
import ProfileEdit from './pages/ProfileEdit';
import AdminUserPage from './pages/AdminUserPage';
import AdminReportedPostsPage from './pages/AdminReportedPostsPage';

import Certificates from './pages/Certificates';
import PasswordChange from './pages/PasswordChange';
import StellaShop from './pages/StellaShop';
import PostReport from './pages/PostReport';
import PointHistory from './pages/PointHistory';
import AdminReportsPage from './pages/AdminReportsPage';
import SuggestionList from './pages/SuggestionList';
import SuggestionCreate from './pages/SuggestionCreate';
import SuggestionDetail from './pages/SuggestionDetail';
import MessagesPage from './pages/MessagesPage';
import NotificationTest from './pages/NotificationTest';

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/password-reset" element={<PasswordReset />} />
        <Route path="/me" element={<MePage />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/profile/edit" element={<ProfileEdit />} />
        <Route path="/posts" element={<PostList />} />
        <Route path="/posts/new" element={<PostCreate />} />
        <Route path="/posts/write" element={<PostCreate />} />
        <Route path="/posts/edit/:id" element={<PostEdit />} />
        <Route path="/posts/:id" element={<PostDetail />} />
        <Route path="/posts/:id/report" element={<PostReport />} />
        <Route path="/admin/users" element={<AdminUserPage />} />
        <Route path="/admin/reports" element={<AdminReportedPostsPage />} />
        <Route path="/admin/report-management" element={<AdminReportsPage />} />

        <Route path="/certificates" element={<Certificates />} />
        <Route path="/password-change" element={<PasswordChange />} />
        <Route path="/shop" element={<StellaShop />} />
        <Route path="/points" element={<PointHistory />} />
        <Route path="/suggestions" element={<SuggestionList />} />
        <Route path="/suggestions/new" element={<SuggestionCreate />} />
        <Route path="/suggestions/:id" element={<SuggestionDetail />} />
        <Route path="/messages" element={<MessagesPage />} />
        <Route path="/notification-test" element={<NotificationTest />} />
      </Route>
    </Routes>
  )
}

export default App