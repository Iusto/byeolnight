// App.tsx
import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout';
import Home from './pages/Home'
import LoginPage from './pages/Login'
import SignupPage from './pages/Signup'
import MePage from './pages/Me'
import PostList from './pages/PostList'
import PostCreate from './pages/PostCreate'
import PostDetail from './pages/PostDetail'
import PostEdit from './pages/PostEdit';
import PasswordReset from './pages/PasswordReset';
import ProfileEdit from './pages/ProfileEdit';
import AdminUserPage from './pages/AdminUserPage';
import AdminReportedPostsPage from './pages/AdminReportedPostsPage';
import ChatRoom from './pages/ChatRoom';
import Certificates from './pages/Certificates';
import PasswordChange from './pages/PasswordChange';
import StellaShop from './pages/StellaShop';
import PostReport from './pages/PostReport';

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/password-reset" element={<PasswordReset />} />
        <Route path="/me" element={<MePage />} />
        <Route path="/profile/edit" element={<ProfileEdit />} />
        <Route path="/posts" element={<PostList />} />
        <Route path="/posts/new" element={<PostCreate />} />
        <Route path="/posts/write" element={<PostCreate />} />
        <Route path="/posts/edit/:id" element={<PostEdit />} />
        <Route path="/posts/:id" element={<PostDetail />} />
        <Route path="/posts/:id/report" element={<PostReport />} />
        <Route path="/admin/users" element={<AdminUserPage />} />
        <Route path="/admin/reports" element={<AdminReportedPostsPage />} />
        <Route path="/chat" element={<ChatRoom />} />
        <Route path="/certificates" element={<Certificates />} />
        <Route path="/password-change" element={<PasswordChange />} />
        <Route path="/shop" element={<StellaShop />} />
      </Route>
    </Routes>
  )
}

export default App