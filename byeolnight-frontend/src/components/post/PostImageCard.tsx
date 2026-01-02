import { Link } from 'react-router-dom';
import { Post } from '../../types/post';
import { extractFirstImage } from '../../utils/postHelpers';

interface PostImageCardProps {
  post: Post;
  isAdmin: boolean;
  selectedPosts: number[];
  onSelect: (postId: number, checked: boolean) => void;
}

export default function PostImageCard({ post, isAdmin, selectedPosts, onSelect }: PostImageCardProps) {
  const isBlinded = post.blinded && !isAdmin;
  const displayTitle = isBlinded ? 
    (post.blindType === 'ADMIN_BLIND' ? '관리자 블라인드 처리됨' : '신고로 블라인드 처리됨') : 
    post.title;
  const displayWriter = isBlinded ? '***' : post.writer;
  const displayStats = isBlinded ? '*' : '';
  const imageUrl = isBlinded ? null : (post.thumbnailUrl || extractFirstImage(post.content));

  const handleImageError = (e: React.SyntheticEvent<HTMLImageElement>) => {
    e.currentTarget.style.display = 'none';
    const parent = e.currentTarget.parentElement;
    if (parent) {
      parent.innerHTML = '<div class="w-full h-full bg-gradient-to-br from-slate-700/50 to-purple-900/30 flex items-center justify-center"><span class="text-3xl sm:text-4xl">🌌</span></div>';
    }
  };

  return (
    <div className={`transition-all duration-300 transform hover:scale-[1.02] active:scale-[0.98] touch-manipulation rounded-xl overflow-hidden ${
      isBlinded ? 'opacity-80 border-red-500/50' : 
      'bg-slate-800/50 border border-slate-600/30 hover:border-purple-500/40 hover:shadow-lg'
    }`}>
      <Link to={`/posts/${post.id}`} className="block">
        <div className="relative aspect-square bg-slate-700/50">
          {isAdmin && (
            <div className="absolute top-2 left-2 z-20">
              <input
                type="checkbox"
                checked={selectedPosts.includes(post.id)}
                onChange={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  onSelect(post.id, e.target.checked);
                }}
                className="w-4 h-4 bg-white/90 rounded border-2 border-gray-400"
                onClick={(e) => e.stopPropagation()}
              />
            </div>
          )}
          
          {post.blinded && (
            <div className="absolute top-2 right-2 z-20">
              <span className={`text-xs px-2 py-1 rounded-full font-bold ${
                post.blindType === 'ADMIN_BLIND' 
                  ? 'bg-red-600/90 text-red-100 border border-red-400/50' 
                  : 'bg-yellow-600/90 text-yellow-100 border border-yellow-400/50'
              }`}>
                {post.blindType === 'ADMIN_BLIND' ? '관리자' : '신고'}
              </span>
            </div>
          )}
          
          {imageUrl ? (
            <img
              src={imageUrl}
              alt="별 사진"
              className="w-full h-full object-cover"
              loading="lazy"
              onError={handleImageError}
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-slate-700/50 to-purple-900/30">
              <span className="text-3xl">🌌</span>
            </div>
          )}
          
          {post.blinded && (
            <div className="absolute inset-0 bg-black/80 flex flex-col items-center justify-center z-10">
              <span className="text-4xl mb-2">🔒</span>
              <span className={`text-xs px-2 py-1 rounded font-bold ${
                post.blindType === 'ADMIN_BLIND' 
                  ? 'bg-red-600/90 text-red-100' 
                  : 'bg-yellow-600/90 text-yellow-100'
              }`}>
                {post.blindType === 'ADMIN_BLIND' ? '관리자 블라인드' : '신고 블라인드'}
              </span>
            </div>
          )}
        </div>
        
        <div className={`p-3 ${
          post.blinded 
            ? 'bg-slate-800/60 border-t border-red-500/30' 
            : 'bg-slate-800/80'
        }`}>
          <h4 className={`text-sm font-medium line-clamp-2 mb-2 ${
            post.blinded ? 'text-gray-300' : 'text-white'
          }`}>
            {displayTitle}
          </h4>
          
          <div className="flex justify-between items-center text-xs text-gray-400">
            <div className="flex items-center gap-1">
              <span className={`rounded px-1 py-0.5 ${
                post.blinded 
                  ? 'bg-red-700/50 border border-red-600/30' 
                  : 'bg-slate-700/50'
              }`}>
                {isBlinded ? '🔒' : '👤'}
              </span>
              <span className="truncate max-w-[60px]">{displayWriter}</span>
            </div>
            <div className="flex gap-2">
              <span className={isBlinded ? 'text-gray-500' : ''}>
                💬 {isBlinded ? displayStats : (post.commentCount || 0)}
              </span>
              <span className={isBlinded ? 'text-gray-500' : ''}>
                ❤️ {isBlinded ? displayStats : post.likeCount}
              </span>
            </div>
          </div>
        </div>
      </Link>
    </div>
  );
}
