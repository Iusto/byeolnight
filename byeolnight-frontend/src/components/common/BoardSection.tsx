import { Link } from 'react-router-dom';
import Section from './Section';
import { UserIconDisplay } from '../user';
import { formatDate } from '../../utils/formatters';
import type { Post } from '../../types/post';

export interface BoardSectionProps {
  title: string;
  icon: string;
  link: string;
  posts: Post[];
  isAdmin: boolean;
  /** Section 래퍼 배경 (그라데이션 가능) */
  bgColor: string;
  /** Section 래퍼 테두리 */
  borderColor: string;
  /** 게시글 행 배경+호버 (그라데이션 가능) */
  rowClass: string;
  /** 통계(좋아요/댓글/조회) 텍스트 색 */
  statColor: string;
  /** 작성자/날짜 메타 텍스트 색 */
  metaColor: string;
  /** 🤖 자동 수집 안내 배너 텍스트 (있을 때만 표시) */
  autoDescription?: string;
  /** 자동 안내 배너 컨테이너 클래스 (배경/테두리/텍스트색) */
  autoDescClass?: string;
  /** 제목 앞 뱃지 (공지/AI 등) */
  badge?: { text: string; className: string };
  /** 좋아요 수 표시 여부 (기본 true) */
  showLikes?: boolean;
  /** 작성자 아이콘 대신 🤖 표시 (AI 자동 생성 게시판) */
  aiBot?: boolean;
}

const titleShadow = { textShadow: '0 2px 4px rgba(0,0,0,0.8)', filter: 'brightness(1.1)' };

/**
 * 홈 화면의 카테고리별 게시판 카드. 색상/뱃지/안내배너만 props로 달라지는
 * 동일한 "게시글 행 리스트" 마크업을 단일 컴포넌트로 통합한다.
 */
export default function BoardSection({
  title,
  icon,
  link,
  posts,
  isAdmin,
  bgColor,
  borderColor,
  rowClass,
  statColor,
  metaColor,
  autoDescription,
  autoDescClass = '',
  badge,
  showLikes = true,
  aiBot = false,
}: BoardSectionProps) {
  const visiblePosts = posts.filter((post) => isAdmin || !post.blinded);

  return (
    <Section title={title} icon={icon} link={link} bgColor={bgColor} borderColor={borderColor}>
      {autoDescription && (
        <div className={`mb-3 p-2 rounded-lg border ${autoDescClass}`}>
          <p className="text-xs flex items-center gap-2">
            <span>🤖</span>
            <span>{autoDescription}</span>
          </p>
        </div>
      )}

      <div className="space-y-3">
        {visiblePosts.map((post) => (
          <div key={post.id} className={`rounded-lg p-4 transition-all duration-300 hover:shadow-glow ${rowClass}`}>
            <Link to={`/posts/${post.id}`}>
              <div className="flex items-center justify-between">
                <span className="flex items-center gap-2 flex-1 mr-3 min-w-0">
                  {badge && (
                    <span className={`flex-shrink-0 text-xs font-bold ${badge.className}`}>{badge.text}</span>
                  )}
                  <span
                    className="font-bold text-white text-sm sm:text-base line-clamp-1 min-w-0"
                    style={titleShadow}
                  >
                    {post.title}
                  </span>
                </span>
                <div className={`flex items-center gap-2 text-sm flex-shrink-0 ${statColor}`}>
                  {showLikes && <span>❤️ {post.likeCount}</span>}
                  <span>💬 {post.commentCount || 0}</span>
                  <span>👁 {post.viewCount}</span>
                </div>
              </div>
              <div className={`text-sm mt-1 flex items-center gap-1 ${metaColor}`}>
                {aiBot ? (
                  <span>🤖</span>
                ) : (
                  <UserIconDisplay iconName={post.writerIcon} size="xsmall" />
                )}
                {post.writer} • 📅 {formatDate(post.createdAt || post.updatedAt)}
              </div>
            </Link>
          </div>
        ))}
      </div>
    </Section>
  );
}
