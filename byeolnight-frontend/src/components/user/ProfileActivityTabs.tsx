import { Link } from 'react-router-dom';
import type { MyActivityData, ProfileTab, UserIcon, UserProfile } from '../../types/profile';
import { categoryLabels } from '../../types/profile';

interface ProfileActivityTabsProps {
  activeTab: ProfileTab;
  profile: UserProfile | null;
  socialProvider?: string;
  icons: UserIcon[];
  activity: MyActivityData | null;
  formatDate: (date?: string) => string;
  postsLoading: boolean;
  postsPage: number;
  fetchPostsPage: (page: number) => void;
  commentsLoading: boolean;
  commentsPage: number;
  fetchCommentsPage: (page: number) => void;
  iconLoading: number | null;
  handleIconEquip: (iconId: number) => void;
}

/** ???? ?? ????????? ? ??? ???? ??? ?? ??? ????. */
export default function ProfileActivityTabs({
  activeTab,
  profile,
  socialProvider,
  icons,
  activity,
  formatDate,
  postsLoading,
  postsPage,
  fetchPostsPage,
  commentsLoading,
  commentsPage,
  fetchCommentsPage,
  iconLoading,
  handleIconEquip,
}: ProfileActivityTabsProps) {
  const user = { socialProvider };
  return (
    <>
          {activeTab === 'info' && profile && (
            <div className="space-y-4 sm:space-y-6">
              <h2 className="text-lg sm:text-2xl font-bold text-white mb-4 sm:mb-6">📋 기본 정보</h2>

              {/* 계정 정보 */}
              <div className="bg-slate-700/30 rounded-lg p-3 sm:p-4">
                <h3 className="text-base sm:text-lg font-bold text-white mb-3 sm:mb-4">🔐 계정 정보</h3>
                <div className="space-y-3">
                  <div>
                    <label className="block text-xs sm:text-sm font-medium text-gray-400 mb-1">이메일</label>
                    <div className="text-white bg-slate-800/50 px-3 py-2 rounded-lg text-sm">
                      {profile.email}
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs sm:text-sm font-medium text-gray-400 mb-1">닉네임</label>
                    <div className="text-white bg-slate-800/50 px-3 py-2 rounded-lg text-sm">
                      {profile.nickname}
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs sm:text-sm font-medium text-gray-400 mb-1">권한</label>
                    <div className="text-white bg-slate-800/50 px-3 py-2 rounded-lg text-sm">
                      {profile.role === 'ADMIN' ? '관리자' : '일반 사용자'}
                      {user?.socialProvider && (
                        <span className="ml-2 text-xs px-2 py-1 rounded-full bg-blue-600/20 text-blue-300">
                          {user.socialProvider === 'google' ? '구글' :
                           user.socialProvider === 'naver' ? '네이버' :
                           user.socialProvider === 'kakao' ? '카카오' : user.socialProvider}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* 활동 통계 */}
              <div className="bg-slate-700/30 rounded-lg p-3 sm:p-4">
                <h3 className="text-base sm:text-lg font-bold text-white mb-3 sm:mb-4">✨ 활동 통계</h3>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 sm:gap-3">
                  <div className="bg-slate-800/50 rounded-lg p-2 sm:p-3 text-center">
                    <div className="text-lg sm:text-2xl mb-1">✨</div>
                    <div className="text-xs sm:text-sm text-gray-400 mb-1">포인트</div>
                    <div className="text-sm sm:text-lg font-bold text-yellow-400">
                      {(profile.points || 0).toLocaleString()}
                    </div>
                  </div>

                  <div className="bg-slate-800/50 rounded-lg p-2 sm:p-3 text-center">
                    <div className="text-lg sm:text-2xl mb-1">🎨</div>
                    <div className="text-xs sm:text-sm text-gray-400 mb-1">아이콘</div>
                    <div className="text-sm sm:text-lg font-bold text-blue-400">
                      {icons.length}
                    </div>
                  </div>

                  <div className="bg-slate-800/50 rounded-lg p-2 sm:p-3 text-center">
                    <div className="text-lg sm:text-2xl mb-1">📝</div>
                    <div className="text-xs sm:text-sm text-gray-400 mb-1">게시글</div>
                    <div className="text-sm sm:text-lg font-bold text-green-400">
                      {activity?.totalPostCount || 0}
                    </div>
                  </div>

                  <div className="bg-slate-800/50 rounded-lg p-2 sm:p-3 text-center">
                    <div className="text-lg sm:text-2xl mb-1">💬</div>
                    <div className="text-xs sm:text-sm text-gray-400 mb-1">댓글</div>
                    <div className="text-sm sm:text-lg font-bold text-purple-400">
                      {activity?.totalCommentCount || 0}
                    </div>
                  </div>

                  <div className="bg-slate-800/50 rounded-lg p-2 sm:p-3 text-center">
                    <div className="text-lg sm:text-2xl mb-1">📅</div>
                    <div className="text-xs sm:text-sm text-gray-400 mb-1">출석</div>
                    <div className="text-sm sm:text-lg font-bold text-orange-400">
                      {profile?.attendanceCount || 0}
                    </div>
                  </div>
                </div>
              </div>

              {/* 닉네임 정보 */}
              <div className="bg-slate-700/30 rounded-lg p-3 sm:p-4">
                <h4 className="text-sm sm:text-base font-medium text-white mb-2 sm:mb-3">닉네임 변경 정보</h4>
                <div className="space-y-2 text-xs sm:text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-400">변경 여부:</span>
                    <span className={profile.nicknameChanged ? 'text-green-400' : 'text-gray-400'}>
                      {profile.nicknameChanged ? '변경함' : '변경 안함'}
                    </span>
                  </div>
                  {profile.nicknameChanged && profile.nicknameUpdatedAt && (
                    <div className="flex justify-between">
                      <span className="text-gray-400">마지막 변경:</span>
                      <span className="text-white">{formatDate(profile.nicknameUpdatedAt)}</span>
                    </div>
                  )}
                  <div className="text-xs text-gray-500 mt-2">
                    * 닉네임은 6개월마다 변경 가능합니다
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'posts' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white">📝 내게시글</h2>
                <span className="text-sm text-gray-400">총 {activity?.totalPostCount || 0}개</span>
              </div>
              {postsLoading ? (
                <div className="text-center py-8">
                  <div className="text-gray-400">로딩 중...</div>
                </div>
              ) : activity?.myPosts && activity.myPosts.length > 0 ? (
                <>
                  <div className="space-y-3">
                    {activity.myPosts.map((post) => (
                      <div key={post.id} className="bg-[#2a2e45] bg-opacity-60 rounded-lg p-4 hover:bg-[#2a2e45] hover:bg-opacity-80 transition-colors">
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <Link
                              to={`/posts/${post.id}`}
                              className="text-white hover:text-purple-300 font-medium block mb-2"
                            >
                              {post.title}
                              {post.isBlinded && <span className="text-red-400 ml-2">(블라인드)</span>}
                            </Link>
                            <p className="text-gray-400 text-sm mb-2">
                              {post.content.replace(/<[^>]*>/g, '').substring(0, 100)}...
                            </p>
                            <div className="flex items-center gap-4 text-xs text-gray-500">
                              <span>🗂 {categoryLabels[post.category] || post.category}</span>
                              <span>❤️ {post.likeCount}</span>
                              <span>💬 {post.commentCount}</span>
                              <span>👁 {post.viewCount}</span>
                              <span>📅 {formatDate(post.createdAt)}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                  {/* 게시글 페이징 */}
                  {(activity.postsTotalPages > 1) && (
                    <div className="flex items-center justify-center gap-2 mt-6">
                      <button
                        onClick={() => fetchPostsPage(postsPage - 1)}
                        disabled={!activity.postsHasPrevious}
                        className={`px-3 py-2 rounded-lg text-sm transition-colors ${
                          activity.postsHasPrevious
                            ? 'bg-slate-700/50 text-white hover:bg-slate-600/50'
                            : 'bg-slate-800/30 text-gray-500 cursor-not-allowed'
                        }`}
                      >
                        이전
                      </button>
                      <span className="text-sm text-gray-400 px-3">
                        {activity.postsCurrentPage + 1} / {activity.postsTotalPages}
                      </span>
                      <button
                        onClick={() => fetchPostsPage(postsPage + 1)}
                        disabled={!activity.postsHasNext}
                        className={`px-3 py-2 rounded-lg text-sm transition-colors ${
                          activity.postsHasNext
                            ? 'bg-slate-700/50 text-white hover:bg-slate-600/50'
                            : 'bg-slate-800/30 text-gray-500 cursor-not-allowed'
                        }`}
                      >
                        다음
                      </button>
                    </div>
                  )}
                </>
              ) : (
                <div className="text-center text-gray-400 py-8">
                  <div className="text-4xl mb-2">📝</div>
                  <p>작성한 게시글이 없습니다.</p>
                  <Link
                    to="/posts/create"
                    className="inline-block mt-3 px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg text-sm transition-colors text-white"
                  >
                    게시글 작성하기
                  </Link>
                </div>
              )}
            </div>
          )}

          {activeTab === 'comments' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white">💬 내댓글</h2>
                <span className="text-sm text-gray-400">총 {activity?.totalCommentCount || 0}개</span>
              </div>
              {commentsLoading ? (
                <div className="text-center py-8">
                  <div className="text-gray-400">로딩 중...</div>
                </div>
              ) : activity?.myComments && activity.myComments.length > 0 ? (
                <>
                  <div className="space-y-3">
                    {activity.myComments.map((comment) => (
                      <div key={comment.id} className="bg-[#2a2e45] bg-opacity-60 rounded-lg p-4 hover:bg-[#2a2e45] hover:bg-opacity-80 transition-colors">
                        <div className="mb-2">
                          <Link
                            to={`/posts/${comment.postId}`}
                            className="text-purple-300 hover:text-purple-200 text-sm font-medium"
                          >
                            📄 {comment.postTitle}
                          </Link>
                          {comment.parentId && (
                            <span className="text-xs text-gray-500 ml-2">(답글)</span>
                          )}
                        </div>
                        <p className="text-white mb-2">
                          {comment.content}
                          {comment.isBlinded && <span className="text-red-400 ml-2">(블라인드)</span>}
                        </p>
                        <div className="text-xs text-gray-500">
                          📅 {formatDate(comment.createdAt)}
                        </div>
                      </div>
                    ))}
                  </div>
                  {/* 댓글 페이징 */}
                  {(activity.commentsTotalPages > 1) && (
                    <div className="flex items-center justify-center gap-2 mt-6">
                      <button
                        onClick={() => fetchCommentsPage(commentsPage - 1)}
                        disabled={!activity.commentsHasPrevious}
                        className={`px-3 py-2 rounded-lg text-sm transition-colors ${
                          activity.commentsHasPrevious
                            ? 'bg-slate-700/50 text-white hover:bg-slate-600/50'
                            : 'bg-slate-800/30 text-gray-500 cursor-not-allowed'
                        }`}
                      >
                        이전
                      </button>
                      <span className="text-sm text-gray-400 px-3">
                        {activity.commentsCurrentPage + 1} / {activity.commentsTotalPages}
                      </span>
                      <button
                        onClick={() => fetchCommentsPage(commentsPage + 1)}
                        disabled={!activity.commentsHasNext}
                        className={`px-3 py-2 rounded-lg text-sm transition-colors ${
                          activity.commentsHasNext
                            ? 'bg-slate-700/50 text-white hover:bg-slate-600/50'
                            : 'bg-slate-800/30 text-gray-500 cursor-not-allowed'
                        }`}
                      >
                        다음
                      </button>
                    </div>
                  )}
                </>
              ) : (
                <div className="text-center text-gray-400 py-8">
                  <div className="text-4xl mb-2">💬</div>
                  <p>작성한 댓글이 없습니다.</p>
                </div>
              )}
            </div>
          )}

          {activeTab === 'icons' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white">🎨 보유 스텔라 아이콘</h2>
                <div className="text-sm text-gray-400">
                  총 {icons.length}개
                </div>
              </div>
              {icons.length === 0 ? (
                <div className="text-center text-gray-400 py-8">
                  <div className="text-4xl mb-2">🛒️</div>
                  <p>아직 구매한 아이콘이 없습니다.</p>
                  <Link
                    to="/shop"
                    className="inline-block mt-3 px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg text-sm transition-colors text-white"
                  >
                    상점에서 구매하기
                  </Link>
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
                  {icons.map((icon) => (
                    <div
                      key={icon.id}
                      className={[
                        'relative p-4 rounded-lg text-center transition-all min-h-[160px] sm:min-h-[180px] flex flex-col justify-between',
                        icon.equipped
                          ? 'bg-purple-600 bg-opacity-30 border-2 border-purple-400'
                          : 'bg-[#2a2e45] bg-opacity-60 hover:bg-[#2a2e45] hover:bg-opacity-80'
                      ].join(' ')}
                    >
                      <div className="flex-1">
                        <div className="text-3xl mb-3">{icon.iconUrl}</div>
                        <div className="text-sm text-gray-300 mb-2 break-words leading-tight">{icon.name}</div>
                        <div className="text-xs text-gray-500 mb-1">{(icon.price || 0).toLocaleString()} 스텔라</div>
                        <div className="text-xs text-gray-500 mb-3">{formatDate(icon.purchasedAt)}</div>
                      </div>

                      <button
                        onClick={() => handleIconEquip(icon.id)}
                        disabled={iconLoading === icon.id}
                        className={[
                          'w-full py-3 px-2 text-xs sm:text-sm rounded transition-colors font-medium min-h-[44px] flex items-center justify-center',
                          icon.equipped
                            ? 'bg-red-600 bg-opacity-20 text-red-400 hover:bg-red-600 hover:bg-opacity-30'
                            : 'bg-purple-600 bg-opacity-20 text-purple-400 hover:bg-purple-600 hover:bg-opacity-30',
                          iconLoading === icon.id ? 'opacity-50 cursor-not-allowed' : ''
                        ].join(' ')}
                      >
                        {iconLoading === icon.id ? '...' : (icon.equipped ? '해제' : '장착')}
                      </button>

                      {icon.equipped && (
                        <div className="absolute -top-1 -right-1 w-5 h-5 bg-green-500 rounded-full flex items-center justify-center">
                          <span className="text-xs text-white">✓</span>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

    </>
  );
}
