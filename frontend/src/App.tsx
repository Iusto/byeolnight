import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import Home from './pages/Home'
import Login from './pages/Login'
import Signup from './pages/Signup'
import BoardList from './pages/BoardList'
import MyPage from './pages/MyPage'
import Header from './components/layout/Header'
import PostCreate from './pages/PostCreate'
import PostDetailPage from './pages/PostDetail'

function App() {
  return (
    <Router>
      <Header />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/board" element={<BoardList />} />
        <Route path="/board/:id" element={<PostDetail />} />
        <Route path="/mypage" element={<MyPage />} />
        <Route path="/board/write" element={<PostCreate />} />
      </Routes>
    </Router>
  )
}

export default App
