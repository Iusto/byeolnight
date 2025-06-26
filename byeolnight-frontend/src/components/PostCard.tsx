import React from 'react';

type PostProps = {
  id: number;
  title: string;
  category: string;
  writer: string;
  hot: boolean;
};

export const PostItem = ({ id, title, category, writer, hot }: PostProps) => {
  return (
    <div className="flex items-center justify-between border-b py-2">
      <div className="flex items-center space-x-2">
        {hot && <span className="text-red-500 font-bold">🔥 HOT!</span>}
        <a href={`/posts/${id}`} className="font-medium text-blue-900 hover:underline">
          {title}
        </a>
      </div>
      <span className="text-sm text-gray-500">{writer}</span>
    </div>
  );
};
