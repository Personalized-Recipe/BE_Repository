import React from 'react';
import './UserReviewsSection.css';

const UserReviewsSection: React.FC = () => {
  return (
    <section id="user-reviews" className="section">
      <div className="section-container">
        <h2 className="section-title">사용자후기</h2>
        <div className="section-content">
          <div className="review-card">
            <p className="review-text">"우리 가족과 더 가까워졌어요. 정말 좋은 서비스입니다!"</p>
            <p className="review-author">- 김가족님</p>
          </div>
          <div className="review-card">
            <p className="review-text">"친구들과 소통하기가 훨씬 편해졌어요."</p>
            <p className="review-author">- 이친구님</p>
          </div>
          <div className="review-card">
            <p className="review-text">"직장 동료들과 업무 소통도 원활해졌습니다."</p>
            <p className="review-author">- 박직장님</p>
          </div>
        </div>
      </div>
    </section>
  );
};

export default UserReviewsSection;
