import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import UserActionPopup from './UserActionPopup';

interface ClickableNicknameProps {
  userId: number;
  nickname: string;
  className?: string;
  children?: React.ReactNode;
}

export default function ClickableNickname({ userId, nickname, className = '', children }: ClickableNicknameProps) {
  const [showPopup, setShowPopup] = useState(false);
  const [popupPosition, setPopupPosition] = useState({ x: 0, y: 0 });

  const handleClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    
    console.log('ClickableNickname 클릭:', { userId, nickname });
    
    const x = e.clientX + 10;
    const y = e.clientY + 10;
    
    setPopupPosition({ x, y });
    setShowPopup(true);
  };

  return (
    <>
      <span
        onClick={handleClick}
        className={`cursor-pointer hover:text-purple-300 transition-colors ${className}`}
      >
        {children || nickname}
      </span>
      
      {showPopup && createPortal(
        <UserActionPopup
          targetUserId={userId}
          targetNickname={nickname}
          onClose={() => setShowPopup(false)}
          position={popupPosition}
        />,
        document.body
      )}
    </>
  );
}