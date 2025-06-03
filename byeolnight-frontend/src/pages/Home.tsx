import React from 'react';
import { useNavigate } from 'react-router-dom';

const Home = () => {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    navigate('/login');
  };

  return (
    <div>
      <h1>환영합니다!</h1>
      <button onClick={handleLogout}>로그아웃</button>
    </div>
  );
};

export default Home;
