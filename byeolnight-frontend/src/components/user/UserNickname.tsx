import React, { useEffect, useState } from 'react';
import axios from '../../lib/axios';
import StellaIcon from './StellaIcon';

interface UserNicknameProps {
  nickname: string;
  userId?: number;
  size?: 'sm' | 'md' | 'lg';
  showIcon?: boolean;
}

interface UserIconData {
  iconUrl: string;
  name: string;
  grade: string;
  animationClass?: string;
}

const UserNickname: React.FC<UserNicknameProps> = ({
  nickname,
  userId,
  size = 'md',
  showIcon = true
}) => {
  const [userIcon, setUserIcon] = useState<UserIconData | null>(null);

  useEffect(() => {
    if (showIcon && userId) {
      fetchUserIcon();
    }
  }, [userId, showIcon]);

  const fetchUserIcon = async () => {
    try {
      const res = await axios.get(`/users/${userId}/equipped-icon`);
      if (res.data.data) {
        setUserIcon(res.data.data);
      }
    } catch (err) {
      // 아이콘이 없거나 오류 발생 시 무시
    }
  };

  const textSizes = {
    sm: 'text-sm',
    md: 'text-base',
    lg: 'text-lg'
  };

  const iconSizes = {
    sm: 'sm' as const,
    md: 'sm' as const,
    lg: 'md' as const
  };

  return (
    <div className="inline-flex items-center gap-1">
      <span className={`font-medium ${textSizes[size]}`}>
        {nickname}
      </span>
      {showIcon && userIcon && (
        <StellaIcon
          iconUrl={userIcon.iconUrl}
          animationClass={userIcon.animationClass}
          grade={userIcon.grade}
          size={iconSizes[size]}
          showTooltip={true}
          tooltipText={userIcon.name}
        />
      )}
    </div>
  );
};

export default UserNickname;