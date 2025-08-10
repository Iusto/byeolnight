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
    <div className="flex items-center justify-between border-b py-2">
      <div className="flex items-center space-x-2">
        {hot && <span className="text-red-500 font-bold">🔥 HOT!</span>}
        <a href={`/posts/${id}`} className="font-medium text-blue-900 hover:underline">
          {title}
        </a>
      </div>
      <ClickableNickname 
        userId={writerId} 
        nickname={writer} 
        className="text-sm text-gray-500"
      />
    </div>
  );
};
