import React, { useEffect, useRef } from 'react';
import './DarkModeToggle.css';

const DarkModeToggle: React.FC = () => {
  const toggleRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    // 브라우저 시간에 따라 기본 상태 설정
    const hours = new Date().getHours();
    if (toggleRef.current) {
      toggleRef.current.checked = hours > 7 && hours < 20;
    }
  }, []);

  const handleToggle = () => {
    // 다크모드 토글 로직 (나중에 연결)
    console.log('다크모드 토글:', toggleRef.current?.checked);
  };

  return (
    <div className="dark-mode-toggle">
      <input
        ref={toggleRef}
        type="checkbox"
        className="toggle"
        onChange={handleToggle}
      />
    </div>
  );
};

export default DarkModeToggle;
