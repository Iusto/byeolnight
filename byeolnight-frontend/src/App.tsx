// App.tsx
import { Routes, Route } from 'react-router-dom'
import AppLayout from './components/Layout/AppLayout'
import Home from './pages/Home'
import LoginPage from './pages/Login'
import SignupPage from './pages/Signup'
import MePage from './pages/Me'
import PostList from './pages/PostList'
import PostCreate from './pages/PostCreate'
import PostDetail from './pages/PostDetail'
import PostEdit from './pages/PostEdit'

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/me" element={<MePage />} />
        <Route path="/posts" element={<PostList />} />
        <Route path="/posts/new" element={<PostCreate />} />
        <Route path="/posts/:id" element={<PostDetail />} />
        <Route path="/posts/edit/:id" element={<PostEdit />} />
      </Route>
    </Routes>
  )
}

export default App
