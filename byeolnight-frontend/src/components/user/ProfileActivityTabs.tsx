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
              <h2 className="text-lg sm:text-2xl font-bold text-white mb-4 sm:mb-6">?뱥 湲곕낯 ?뺣낫</h2>

              {/* 怨꾩젙 ?뺣낫 */}
              <div className="bg-slate-700/30 rounded-lg p-3 sm:p-4">
                <h3 className="text-base sm:text-lg font-bold text-white mb-3 sm:mb-4">?뵍 怨꾩젙 ?뺣낫</h3>
                <div className="space-y-3">
                  <div>
                    <label className="block text-xs sm:text-sm font-medium text-gray-400 mb-1">?대찓??/label>
                    <div className="text-white bg-slate-800/50 px-3 py-2 rounded-lg text-sm">
                      {profile.email}
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs sm:text-sm font-medium text-gray-400 mb-1">?됰꽕??/label>
                    <div className="text-white bg-slate-800/50 px-3 py-2 rounded-lg text-sm">
                      {profile.nickname}
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs sm:text-sm font-medium text-gray-400 mb-1">沅뚰븳</label>
                    <div className="text-white bg-slate-800/50 px-3 py-2 rounded-lg text-sm">
                      {profile.role === 'ADMIN' ? '愿由ъ옄' : '?쇰컲 ?ъ슜??}
                      {user?.socialProvider && (
                        <span className="ml-2 text-xs px-2 py-1 rounded-full bg-blue-600/20 text-blue-300">
                          {user.socialProvider === 'google' ? '援ш?' :
                           user.socialProvider === 'naver' ? '?ㅼ씠踰? :
                           user.socialProvider === 'kakao' ? '移댁뭅?? : user.socialProvider}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* ?쒕룞 ?듦퀎 */}
              <div className="bg-slate-700/30 rounded-lg p-3 sm:p-4">
                <h3 className="text-base sm:text-lg font-bold text-white mb-3 sm:mb-4">???쒕룞 ?듦퀎</h3>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 sm:gap-3">
                  <div className="bg-slate-800/50 rounded-lg p-2 sm:p-3 text-center">
                    <div className="text-lg sm:text-2xl mb-1">??/div>
                    <div className="text-xs sm:text-sm text-gray-400 mb-1">?ъ씤??/div>
                    <div className="text-sm sm:text-lg font-bold text-yellow-400">
                      {(profile.points || 0).toLocaleString()}
                    </div>
                  </div>

                  <div className="bg-slate-800/50 rounded-lg p-2 sm:p-3 text-center">
                    <div className="text-lg sm:text-2xl mb-1">?렓</div>
                    <div className="text-xs sm:text-sm text-gray-400 mb-1">?꾩씠肄?/div>
                    <div className="text-sm sm:text-lg font-bold text-blue-400">
                      {icons.length}
                    </div>
                  </div>

                  <div className="bg-slate-800/50 rounded-lg p-2 sm:p-3 text-center">
                    <div className="text-lg sm:text-2xl mb-1">?뱷</div>
                    <div className="text-xs sm:text-sm text-gray-400 mb-1">寃뚯떆湲</div>
                    <div className="text-sm sm:text-lg font-bold text-green-400">
                      {activity?.totalPostCount || 0}
                    </div>
                  </div>

                  <div className="bg-slate-800/50 rounded-lg p-2 sm:p-3 text-center">
                    <div className="text-lg sm:text-2xl mb-1">?뮠</div>
                    <div className="text-xs sm:text-sm text-gray-400 mb-1">?볤?</div>
                    <div className="text-sm sm:text-lg font-bold text-purple-400">
                      {activity?.totalCommentCount || 0}
                    </div>
                  </div>

                  <div className="bg-slate-800/50 rounded-lg p-2 sm:p-3 text-center">
                    <div className="text-lg sm:text-2xl mb-1">?뱟</div>
                    <div className="text-xs sm:text-sm text-gray-400 mb-1">異쒖꽍</div>
                    <div className="text-sm sm:text-lg font-bold text-orange-400">
                      {profile?.attendanceCount || 0}
                    </div>
                  </div>
                </div>
              </div>

              {/* ?됰꽕???뺣낫 */}
              <div className="bg-slate-700/30 rounded-lg p-3 sm:p-4">
                <h4 className="text-sm sm:text-base font-medium text-white mb-2 sm:mb-3">?됰꽕??蹂寃??뺣낫</h4>
                <div className="space-y-2 text-xs sm:text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-400">蹂寃??щ?:</span>
                    <span className={profile.nicknameChanged ? 'text-green-400' : 'text-gray-400'}>
                      {profile.nicknameChanged ? '蹂寃쏀븿' : '蹂寃??덊븿'}
                    </span>
                  </div>
                  {profile.nicknameChanged && profile.nicknameUpdatedAt && (
                    <div className="flex justify-between">
                      <span className="text-gray-400">留덉?留?蹂寃?</span>
                      <span className="text-white">{formatDate(profile.nicknameUpdatedAt)}</span>
                    </div>
                  )}
                  <div className="text-xs text-gray-500 mt-2">
                    * ?됰꽕?꾩? 6媛쒖썡留덈떎 蹂寃?媛?ν빀?덈떎
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'posts' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white">?뱷 ?닿쾶?쒓?</h2>
                <span className="text-sm text-gray-400">珥?{activity?.totalPostCount || 0}媛?/span>
              </div>
              {postsLoading ? (
                <div className="text-center py-8">
                  <div className="text-gray-400">濡쒕뵫 以?..</div>
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
                              {post.isBlinded && <span className="text-red-400 ml-2">(釉붾씪?몃뱶)</span>}
                            </Link>
                            <p className="text-gray-400 text-sm mb-2">
                              {post.content.replace(/<[^>]*>/g, '').substring(0, 100)}...
                            </p>
                            <div className="flex items-center gap-4 text-xs text-gray-500">
                              <span>?뾺 {categoryLabels[post.category] || post.category}</span>
                              <span>?ㅿ툘 {post.likeCount}</span>
                              <span>?뮠 {post.commentCount}</span>
                              <span>?몓 {post.viewCount}</span>
                              <span>?뱟 {formatDate(post.createdAt)}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                  {/* 寃뚯떆湲 ?섏씠吏?*/}
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
                        ?댁쟾
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
                        ?ㅼ쓬
                      </button>
                    </div>
                  )}
                </>
              ) : (
                <div className="text-center text-gray-400 py-8">
                  <div className="text-4xl mb-2">?뱷</div>
                  <p>?묒꽦??寃뚯떆湲???놁뒿?덈떎.</p>
                  <Link
                    to="/posts/create"
                    className="inline-block mt-3 px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg text-sm transition-colors text-white"
                  >
                    寃뚯떆湲 ?묒꽦?섍린
                  </Link>
                </div>
              )}
            </div>
          )}

          {activeTab === 'comments' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white">?뮠 ?대뙎湲</h2>
                <span className="text-sm text-gray-400">珥?{activity?.totalCommentCount || 0}媛?/span>
              </div>
              {commentsLoading ? (
                <div className="text-center py-8">
                  <div className="text-gray-400">濡쒕뵫 以?..</div>
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
                            ?뱞 {comment.postTitle}
                          </Link>
                          {comment.parentId && (
                            <span className="text-xs text-gray-500 ml-2">(?듦?)</span>
                          )}
                        </div>
                        <p className="text-white mb-2">
                          {comment.content}
                          {comment.isBlinded && <span className="text-red-400 ml-2">(釉붾씪?몃뱶)</span>}
                        </p>
                        <div className="text-xs text-gray-500">
                          ?뱟 {formatDate(comment.createdAt)}
                        </div>
                      </div>
                    ))}
                  </div>
                  {/* ?볤? ?섏씠吏?*/}
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
                        ?댁쟾
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
                        ?ㅼ쓬
                      </button>
                    </div>
                  )}
                </>
              ) : (
                <div className="text-center text-gray-400 py-8">
                  <div className="text-4xl mb-2">?뮠</div>
                  <p>?묒꽦???볤????놁뒿?덈떎.</p>
                </div>
              )}
            </div>
          )}

          {activeTab === 'icons' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white">?렓 蹂댁쑀 ?ㅽ뀛???꾩씠肄?/h2>
                <div className="text-sm text-gray-400">
                  珥?{icons.length}媛?
                </div>
              </div>
              {icons.length === 0 ? (
                <div className="text-center text-gray-400 py-8">
                  <div className="text-4xl mb-2">?썟截?/div>
                  <p>?꾩쭅 援щℓ???꾩씠肄섏씠 ?놁뒿?덈떎.</p>
                  <Link
                    to="/shop"
                    className="inline-block mt-3 px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg text-sm transition-colors text-white"
                  >
                    ?곸젏?먯꽌 援щℓ?섍린
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
                        <div className="text-xs text-gray-500 mb-1">{(icon.price || 0).toLocaleString()} ?ㅽ뀛??/div>
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
                        {iconLoading === icon.id ? '...' : (icon.equipped ? '?댁젣' : '?μ갑')}
                      </button>

                      {icon.equipped && (
                        <div className="absolute -top-1 -right-1 w-5 h-5 bg-green-500 rounded-full flex items-center justify-center">
                          <span className="text-xs text-white">??/span>
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
