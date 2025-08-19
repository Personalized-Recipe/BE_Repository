import React from 'react';
import cloudIcon from '../../assets/icons/구름.png';
import './CloudButton.css';

interface CloudButtonProps {
  text: string;
  onClick: () => void;
  className?: string;
}

const CloudButton: React.FC<CloudButtonProps> = ({ text, onClick, className = '' }) => {
  return (
    <button 
      className={`cloud-button ${className}`}
      onClick={onClick}
    >
      <img src={cloudIcon} alt="구름" className="cloud-bg" />
      <span className="cloud-text">{text}</span>
    </button>
  );
};

export default CloudButton;
