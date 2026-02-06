import { Link } from 'react-router-dom';
import { UserIconDisplay } from '../user';

interface Post {
  id: number;
  title: string;
  writer: string;
  writerIcon?: string;
  likeCount: number;
  viewCount: number;
  commentCount: number;
  dDay?: string;
}

interface PostCardProps {
  post: Post;
  showStats?: boolean;
}

export default function PostCard({ post, showStats = true }: PostCardProps) {
  return (
    <div className="group bg-gradient-to-br from-white/10 to-white/20 hover:from-white/15 hover:to-white/25 rounded-xl p-3 sm:p-4 transition-all duration-300 border border-white/20 hover:border-purple-400/50 backdrop-blur-sm hover:shadow-lg hover:shadow-purple-500/20 hover:-translate-y-1">
      <Link to={`/posts/${post.id}`} className="block">
        <h3 className="font-bold mb-2 group-hover:text-purple-300 transition-colors line-clamp-2 text-xs sm:text-sm text-white mobile-text" style={{textShadow: '0 2px 4px rgba(0,0,0,0.8)', filter: 'brightness(1.1)'}}>
          {post.dDay && (
            <span className="bg-gradient-to-r from-orange-500 to-red-500 text-white px-1.5 py-0.5 sm:px-2 sm:py-1 rounded-full text-xs mr-1 sm:mr-2 shadow-lg animate-pulse">
              {post.dDay}
            </span>
          )}
          {post.title}
        </h3>
        {showStats && (
          <div className="flex items-center justify-between text-xs text-white mobile-text-secondary group-hover:text-gray-300 transition-colors mobile-caption">
            <span className="flex items-center gap-1 truncate">
              <UserIconDisplay iconName={post.writerIcon} size="xsmall" />
              <span className="truncate text-white mobile-text" style={{textShadow: '0 1px 2px rgba(0,0,0,0.8)', filter: 'brightness(1.1)'}}>{post.writer}</span>
            </span>
            <div className="flex items-center gap-2 sm:gap-3 flex-shrink-0">
              <span className="flex items-center gap-0.5 hover:text-blue-400 transition-colors">
                <span>👁</span> {post.viewCount}
              </span>
              <span className="flex items-center gap-0.5 hover:text-green-400 transition-colors">
                <span>💬</span> {post.commentCount || 0}
              </span>
              <span className="flex items-center gap-0.5 hover:text-red-400 transition-colors">
                <span>❤️</span> {post.likeCount}
              </span>
            </div>
          </div>
        )}
      </Link>
    </div>
  );
}
