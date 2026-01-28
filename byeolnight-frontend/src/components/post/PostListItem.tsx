import { Link } from 'react-router-dom';
import { Post } from '../../types/post';
import { UserIconDisplay } from '../user';

interface PostListItemProps {
  post: Post;
  isHot?: boolean;
  isAdmin: boolean;
  selectedPosts: number[];
  onSelect: (postId: number, checked: boolean) => void;
  showContent?: boolean;
}

export default function PostListItem({ post, isHot = false, isAdmin, selectedPosts, onSelect, showContent = false }: PostListItemProps) {
  const isBlinded = post.blinded && !isAdmin;
  const displayTitle = isBlinded ? 
    (post.blindType === 'ADMIN_BLIND' ? '관리자 블라인드 처리됨' : '신고로 블라인드 처리됨') : 
    post.title;
  const displayContent = isBlinded ? '내용을 볼 수 없습니다.' : post.content;
  const displayWriter = isBlinded ? '***' : post.writer;
  const displayStats = isBlinded ? '*' : '';

  return (
    <div className={`transition-all duration-300 transform hover:scale-[1.02] active:scale-[0.98] touch-manipulation rounded-xl p-4 ${
      isBlinded ? 'opacity-80 border-red-500/50' : 
      isHot ? 'bg-gradient-to-br from-orange-600/20 to-red-600/20 border border-orange-500/30 hover:shadow-orange-500/25' :
      'bg-slate-800/50 border border-slate-600/30 hover:border-purple-500/40 hover:shadow-lg'
    }`}>
      <div className="flex items-start gap-3">
        {isAdmin && (
          <input
            type="checkbox"
            checked={selectedPosts.includes(post.id)}
            onChange={(e) => onSelect(post.id, e.target.checked)}
            className="w-4 h-4 mt-1 flex-shrink-0"
          />
        )}
        <div className="flex-1 min-w-0">
          <Link to={`/posts/${post.id}`} className="block">
            <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start mb-2 gap-2">
              <h4 className="text-base sm:text-lg font-semibold text-white flex-1 flex items-center gap-2 line-clamp-2">
                {isHot && <span className="text-orange-400 flex-shrink-0">🔥</span>}
                {post.dDay && <span className="bg-orange-500 text-white px-2 py-1 rounded text-xs flex-shrink-0">[{post.dDay}]</span>}
                <span className="break-words">{displayTitle}</span>
                {post.blinded && (
                  <span className={`text-xs px-2 py-1 rounded flex-shrink-0 ${
                    post.blindType === 'ADMIN_BLIND' 
                      ? 'bg-red-600/20 text-red-400 border border-red-500/30' 
                      : 'bg-yellow-600/20 text-yellow-400 border border-yellow-500/30'
                  }`}>
                    {post.blindType === 'ADMIN_BLIND' ? '관리자 블라인드' : '신고 블라인드'}
                  </span>
                )}
              </h4>
              <div className="flex items-center gap-3 text-sm text-gray-400 flex-shrink-0">
                {isHot && <span className="bg-orange-500/20 text-orange-300 px-2 py-1 rounded text-xs font-bold">HOT</span>}
                <span className="flex items-center gap-1">💬 {isBlinded ? displayStats : (post.commentCount || 0)}</span>
                <span className="flex items-center gap-1">❤️ {isBlinded ? displayStats : post.likeCount}</span>
              </div>
            </div>
            {showContent && (
              <p className="text-sm text-gray-300 mb-3 line-clamp-2 leading-relaxed">
                {displayContent}
              </p>
            )}
            <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2 text-sm text-gray-400">
              <span className="flex items-center gap-2">
                <span className="bg-slate-700/50 rounded px-2 py-1 border border-slate-600/30 flex items-center justify-center">
                  {isBlinded ? '🔒' : <UserIconDisplay iconName={post.writerIcon} size="small" />}
                </span>
                <span className="truncate">{displayWriter}</span>
              </span>
              <span className="flex items-center gap-1 text-xs sm:text-sm">
                📅 {isBlinded ? '****-**-**' : new Date(post.createdAt).toLocaleDateString()}
              </span>
            </div>
          </Link>
        </div>
      </div>
    </div>
  );
}
