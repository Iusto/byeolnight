import React from 'react';
import ClickableNickname from './ClickableNickname';

type PostProps = {
  id: number;
  title: string;
  category: string;
  writer: string;
  writerId: number;
  hot: boolean;
};

export const PostItem = ({ id, title, category, writer, writerId, hot }: PostProps) => {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b py-3 sm:py-2 gap-2 sm:gap-0 touch-target">
      <div className="flex items-center space-x-2 flex-1 min-w-0">
        {hot && <span className="text-red-500 font-bold text-sm sm:text-base flex-shrink-0">🔥 HOT!</span>}
        <a 
          href={`/posts/${id}`} 
          className="font-medium text-blue-900 hover:underline break-words text-sm sm:text-base min-h-[44px] sm:min-h-0 flex items-center"
        >
          {title}
        </a>
      </div>
      <div className="flex-shrink-0 self-start sm:self-center">
        <ClickableNickname 
          userId={writerId} 
          nickname={writer} 
          className="text-xs sm:text-sm text-gray-500"
        />
      </div>
    </div>
  );
};
