import React from 'react';
import { Link } from 'react-router-dom';
import './MainPage.css';

const MainPage = () => {
  return (
    <div className="main-page">
      <section className="hero">
        <h1>개인화된 레시피 추천 서비스</h1>
        <p>당신의 건강 상태, 선호도, 제약사항에 맞는 맞춤형 레시피를 추천해 드립니다.</p>
        <Link to="/recipe" className="btn btn-primary">레시피 추천 받기</Link>
      </section>

      <section className="features">
        <h2>서비스 특징</h2>
        <div className="feature-grid">
          <div className="feature-card">
            <h3>개인 맞춤형</h3>
            <p>사용자의 건강 상태, 나이, 성별 등을 고려한 맞춤형 레시피를 제공합니다.</p>
          </div>
          <div className="feature-card">
            <h3>알러지 고려</h3>
            <p>사용자의 알러지 정보를 기반으로 안전한 레시피만을 추천합니다.</p>
          </div>
          <div className="feature-card">
            <h3>건강 상태 반영</h3>
            <p>특정 질병이나 건강 상태에 맞는 영양소를 고려한 레시피를 제공합니다.</p>
          </div>
        </div>
      </section>

      <section className="how-it-works">
        <h2>사용 방법</h2>
        <div className="steps">
          <div className="step">
            <div className="step-number">1</div>
            <h3>회원가입</h3>
            <p>기본 정보와 건강 상태, 알러지 정보를 입력하세요.</p>
          </div>
          <div className="step">
            <div className="step-number">2</div>
            <h3>레시피 요청</h3>
            <p>원하는 종류의 레시피를 요청하세요.</p>
          </div>
          <div className="step">
            <div className="step-number">3</div>
            <h3>맞춤형 추천</h3>
            <p>AI가 당신에게 맞는 레시피를 추천해 드립니다.</p>
          </div>
        </div>
      </section>

      <section className="cta">
        <h2>지금 바로 시작하세요!</h2>
        <p>회원가입 없이도 서비스를 체험해 볼 수 있습니다.</p>
        <div className="cta-buttons">
          <Link to="/recipe" className="btn btn-primary">레시피 추천 체험하기</Link>
          <Link to="/register" className="btn btn-secondary">회원가입</Link>
        </div>
      </section>
    </div>
  );
};

export default MainPage; 