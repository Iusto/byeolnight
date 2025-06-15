import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Home from './pages/Home';
import Login from './pages/Login';
import Signup from './pages/Signup';
import BoardList from './pages/PostList';
import PostDetail from './pages/PostDetail';
import Header from './components/layout/Header';
import PostWrite from './pages/PostWrite';
import PostEdit from './pages/PostEdit';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Header />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/board" element={<BoardList />} />
          <Route path="/board/:id" element={<PostDetail />} />
          <Route path="/board/write" element={<PostWrite />} />
          <Route path="/board/edit/:id" element={<PostEdit />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
