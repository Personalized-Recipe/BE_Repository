import React from 'react';

const Footer = () => {
  return (
    <footer>
      <div className="container">
        <p>&copy; {new Date().getFullYear()} 개인화 레시피 서비스. All rights reserved.</p>
      </div>
    </footer>
  );
};

export default Footer; 