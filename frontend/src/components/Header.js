import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './Header.css';

const Header = () => {
  const navigate = useNavigate();
  const isLoggedIn = localStorage.getItem('token');
  
  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    navigate('/');
  };
  
  return (
    <header>
      <div className="container">
        <div className="header-content">
          <h1 className="logo">
            <Link to="/">개인화 레시피</Link>
          </h1>
          <nav>
            <ul className="nav-links">
              <li>
                <Link to="/">홈</Link>
              </li>
              {isLoggedIn ? (
                <>
                  <li>
                    <Link to="/recipe">레시피 추천</Link>
                  </li>
                  <li>
                    <Link to="/profile">프로필</Link>
                  </li>
                  <li>
                    <button onClick={handleLogout} className="btn-link">로그아웃</button>
                  </li>
                </>
              ) : (
                <>
                  <li>
                    <Link to="/login">로그인</Link>
                  </li>
                  <li>
                    <Link to="/register">회원가입</Link>
                  </li>
                </>
              )}
            </ul>
          </nav>
        </div>
      </div>
    </header>
  );
};

export default Header; 