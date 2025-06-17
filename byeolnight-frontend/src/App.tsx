import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Home from './pages/Home'
import AppLayout from './components/Layout/AppLayout'
import LoginPage from './pages/Login'
import SignupPage from './pages/Signup'

function App() {
  return (
    <BrowserRouter>
      <AppLayout>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
        </Routes>
      </AppLayout>
    </BrowserRouter>
  )
}

export default App
