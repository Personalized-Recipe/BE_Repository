import React from 'react';
import './HowToUseSection.css';

const HowToUseSection: React.FC = () => {
  return (
    <section id="how-to-use" className="section">
      <div className="section-container">
        <h2 className="section-title">사용법</h2>
        <div className="section-content">
          <div className="step-card">
            <div className="step-number">1</div>
            <h3>회원가입</h3>
            <p>간단한 회원가입으로 서비스를 이용하세요.</p>
          </div>
          <div className="step-card">
            <div className="step-number">2</div>
            <h3>공간 만들기</h3>
            <p>우리만의 공간을 만들고 초대하세요.</p>
          </div>
          <div className="step-card">
            <div className="step-number">3</div>
            <h3>소통하기</h3>
            <p>다양한 기능으로 즐겁게 소통하세요.</p>
          </div>
        </div>
      </div>
    </section>
  );
};

export default HowToUseSection;
