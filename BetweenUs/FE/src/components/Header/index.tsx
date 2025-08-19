import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import DarkModeToggle from '../DarkModeToggle';
import CloudButton from '../CloudButton';
import './Header.css';
import logo from '../../assets/icons/우리 사이 로고.png';

const Header: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const scrollToSection = (sectionId: string) => {
    const element = document.getElementById(sectionId);
    if (element) {
      const headerHeight = 150; // 헤더 높이
      const extraOffset = 20; // 추가 여백
      const elementPosition = element.offsetTop - headerHeight - extraOffset;
      window.scrollTo({
        top: elementPosition,
        behavior: 'smooth'
      });
    }
  };

  const handleLogoClick = () => {
    if (location.pathname === '/') {
      // 이미 홈페이지에 있으면 상단으로 스크롤
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
      // 다른 페이지에 있으면 홈페이지로 이동
      navigate('/');
    }
  };

  return (
    <header className="header">
      <div className="header-container">
        <div className="logo-section">
          <div className="logo" onClick={handleLogoClick}>
            <img src={logo} alt="우리 사이 로고" />
          </div>
          <DarkModeToggle />
        </div>
        <nav className="navigation">
          <CloudButton 
            text="소식듣기"
            onClick={() => scrollToSection('email-subscription')}
          />
          <CloudButton 
            text="사용법"
            onClick={() => scrollToSection('how-to-use')}
          />
          <CloudButton 
            text="사용자후기"
            onClick={() => scrollToSection('user-reviews')}
          />
        </nav>
        <div className="auth-buttons">
          <button className="login-btn">우리사이에 함께하기</button>
        </div>
      </div>
    </header>
  );
};

export default Header;
