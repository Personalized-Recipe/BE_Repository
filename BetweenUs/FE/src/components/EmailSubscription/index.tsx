import React, { useState } from 'react';
import './EmailSubscription.css';

const EmailSubscription: React.FC = () => {
  const [email, setEmail] = useState('');

  const handleEmailSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // 이메일 제출 로직 (나중에 API 연동)
    console.log('이메일 제출:', email);
    alert('이메일이 성공적으로 제출되었습니다!');
    setEmail('');
  };

  return (
    <section id="email-subscription" className="email-subscription-section">
      <div className="email-subscription-container">
        <h3>서비스 소식을 받아보세요</h3>
        <p>새로운 기능과 업데이트 소식을 이메일로 받아보세요</p>
        <form onSubmit={handleEmailSubmit} className="email-form">
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="이메일 주소를 입력하세요"
            required
            className="email-input"
          />
          <button type="submit" className="email-submit-btn">
            구독하기
          </button>
        </form>
      </div>
    </section>
  );
};

export default EmailSubscription;
