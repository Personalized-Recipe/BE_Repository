import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './HeroSection.css';

const HeroSection: React.FC = () => {
  const navigate = useNavigate();
  const [scrollY, setScrollY] = useState(0);

  useEffect(() => {
    const handleScroll = () => {
      setScrollY(window.scrollY);
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <section className="hero-section">
      <div className="hero-content">
        <div className={`hero-element hero-title ${scrollY > 200 ? 'visible' : ''}`}>
          <h1>우리사이</h1>
        </div>
        
        <div className={`hero-element hero-subtitle ${scrollY > 250 ? 'visible' : ''}`}>
          <p>함께하는 공간, 우리만의 이야기</p>
        </div>
        
        <div className={`hero-element hero-description ${scrollY > 300 ? 'visible' : ''}`}>
          <p>거리에 상관없이 마음을 나누는 특별한 공간</p>
        </div>
        
        <div className={`hero-element hero-features ${scrollY > 350 ? 'visible' : ''}`}>
          <div className="feature-item">
            <span className="feature-icon">💬</span>
            <span>실시간 대화</span>
          </div>
          <div className="feature-item">
            <span className="feature-icon">🔒</span>
            <span>안전한 공간</span>
          </div>
          <div className="feature-item">
            <span className="feature-icon">❤️</span>
            <span>마음의 연결</span>
          </div>
        </div>
        
        <div className={`hero-element hero-button-container ${scrollY > 600 ? 'visible' : ''}`}>
          <button 
            className="hero-button"
            onClick={() => navigate('/middle-distance')}
          >
            시작하기
          </button>
        </div>
      </div>
      
      <div className="scroll-indicator">
        <div className="scroll-arrow">↓</div>
        <span>스크롤하여 더 알아보기</span>
      </div>
    </section>
  );
};

export default HeroSection;
