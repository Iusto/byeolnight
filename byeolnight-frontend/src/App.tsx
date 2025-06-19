import { BrowserRouter, Routes, Route } from 'react-router-dom'
import AppLayout from './components/Layout/AppLayout'
import Home from './pages/Home'
import LoginPage from './pages/Login'
import SignupPage from './pages/Signup'
import MePage from './pages/Me'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/me" element={<MePage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
