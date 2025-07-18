import { useEffect, useState, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';
import { sanitizeHtml } from '../utils/htmlSanitizer';
import { parseMarkdown } from '../utils/markdownParser';

interface FileDto {
  originalName: string;
  s3Key: string;
  url: string;
}

export default function PostEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('NEWS');
  const [images, setImages] = useState<FileDto[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [isMarkdownMode, setIsMarkdownMode] = useState(false);
  const editorRef = useRef<any>(null);
  
  const [isImageChecking, setIsImageChecking] = useState(false);
  
  // 모바일 환경 감지 함수
  const isMobile = () => {
    return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
  };

  // 클립보드 이미지 업로드 함수
  const uploadClipboardImage = async (file: File) => {
    // 파일 크기 체크 (10MB 제한)
    if (file.size > 10 * 1024 * 1024) {
      alert('파일 크기는 10MB를 초과할 수 없습니다.');
      return Promise.reject(new Error('파일 크기 초과'));
    }
    
    setIsImageChecking(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      // 직접 서버로 업로드하는 방식 사용 (모바일 환경에서 더 안정적)
      const response = await axios.post('/files/upload-image', formData);
      
      const imageData = response.data.data;
      setImages(prev => [...prev, imageData]);
      
      return imageData.url;
    } catch (error) {
      console.error('클립보드 이미지 업로드 실패:', error);
      throw error;
    } finally {
      setIsImageChecking(false);
    }
  };
  
  // 클립보드 붙여넣기 이벤트 핸들러
  const handlePaste = async (event: ClipboardEvent) => {
    const items = event.clipboardData?.items;
    if (!items) return;
    
    // 모바일 환경 감지
    const isMobileDevice = isMobile();
    
    // 모바일에서 클립보드 접근 제한 있을 수 있음
    if (isMobileDevice && items.length === 0) {
      console.log('모바일에서 클립보드 접근 제한 감지');
      return;
    }
    
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      
      // 이미지 파일인지 확인
      if (item.type.indexOf('image') !== -1) {
        event.preventDefault();
        const file = item.getAsFile();
        if (file) {
          try {
            const imageUrl = await uploadClipboardImage(file);
            // ReactQuill에 이미지 삽입
            setContent(prev => prev + `<img src="${imageUrl}" alt="클립보드 이미지" style="max-width: 100%; height: auto;" /><br/>`);
          } catch (error) {
            console.error('클립보드 이미지 업로드 실패:', error);
            alert('이미지 업로드에 실패했습니다.');
          }
        }
        break;
      }
    }
  };
  
  const handleImageUpload = () => {
    const input = document.createElement('input');
    input.setAttribute('type', 'file');
    input.setAttribute('accept', 'image/*');
    
    // 모바일에서 갤러리 접근을 위해 capture 속성 제거 및 camera 접근 방지
    input.removeAttribute('capture');
    
    // 모바일 환경에서 카메라 대신 갤러리만 사용하도록 설정
    if (isMobile()) {
      // 명시적으로 카메라 접근을 방지하는 설정
      input.setAttribute('capture', 'none');
    }
    
    // 실제 DOM에 추가하여 모바일에서도 작동하도록 함
    document.body.appendChild(input);
    input.style.display = 'none';
    input.click();
    
    input.onchange = async () => {
      const file = input.files?.[0];
      if (file) {
        // 파일 크기 체크 (10MB 제한)
        if (file.size > 10 * 1024 * 1024) {
          alert('파일 크기는 10MB를 초과할 수 없습니다.');
          document.body.removeChild(input);
          return;
        }
        
        setIsImageChecking(true);
        try {
          const formData = new FormData();
          formData.append('file', file);
          
          // 직접 서버로 업로드하는 방식 사용 (모바일 환경에서 더 안정적)
          const response = await axios.post('/files/upload-image', formData);
          
          const imageData = response.data.data;
          setImages(prev => [...prev, imageData]);
          
          // ReactQuill에 이미지 삽입
          setContent(prev => prev + `<img src="${imageData.url}" alt="${imageData.originalName}" style="max-width: 100%; height: auto;" /><br/>`);
        } catch (error: any) {
          console.error('이미지 업로드 실패:', error);
          const errorMsg = error.response?.data?.message || '이미지 업로드에 실패했습니다.';
          alert(errorMsg);
        } finally {
          setIsImageChecking(false);
          // DOM에서 제거
          document.body.removeChild(input);
        }
      } else {
        // 파일 선택 취소 시 DOM에서 제거
        document.body.removeChild(input);
      }
    };
  };
  
  const removeImage = (index: number) => {
    const imageToRemove = images[index];
    if (imageToRemove) {
      // 게시글 내용에서도 해당 이미지 제거
      setContent(prev => {
        const imgRegex = new RegExp(`<img[^>]*src="${imageToRemove.url.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}"[^>]*>`, 'gi');
        return prev.replace(imgRegex, '');
      });
    }
    setImages(prev => prev.filter((_, i) => i !== index));
  };

  useEffect(() => {
    const fetchPost = async () => {
      if (!user) {
        setError('로그인이 필요합니다.');
        setLoading(false);
        return;
      }
      
      try {
        console.log('게시글 수정 페이지 - API 호출:', `/public/posts/${id}`);
        const res = await axios.get(`/public/posts/${id}`);
        console.log('게시글 수정 API 응답:', res.data);
        
        // 응답 구조 안전하게 처리
        const post = res.data?.data || res.data;
        console.log('처리된 게시글 데이터:', post);
        
        if (!post || !post.title) {
          setError('게시글 데이터를 찾을 수 없습니다.');
          setLoading(false);
          return;
        }
        
        // 작성자 또는 관리자만 수정 가능
        console.log('권한 확인 - 게시글 작성자:', post.writer, '현재 사용자:', user.nickname);
        if (post.writer !== user.nickname && user.role !== 'ADMIN') {
          setError('수정 권한이 없습니다.');
          setLoading(false);
          return;
        }
        
        setTitle(post.title);
        setContent(post.content);
        setCategory(post.category);
        setImages(post.images || []);
        
        // ReactQuill에 콘텐츠 설정은 state로 처리됨
      } catch (err) {
        console.error('게시글 로드 실패:', err);
        console.error('에러 상세:', err.response?.data);
        setError('게시글을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchPost();
  }, [id, user]);
  
  // 컴포넌트 마운트 시 이벤트 리스너 등록
  useEffect(() => {
    document.addEventListener('paste', handlePaste);
    return () => {
      document.removeEventListener('paste', handlePaste);
    };
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    // 길이 검증
    if (title.length > 100) {
      setError('제목은 100자를 초과할 수 없습니다.');
      return;
    }
    
    if (content.length > 50000) {
      setError('내용은 50,000자를 초과할 수 없습니다.');
      return;
    }
    
    // 마크다운 모드인 경우 HTML로 변환 후 보안 검증
    const finalContent = sanitizeHtml(isMarkdownMode ? parseMarkdown(content) : content);
    
    // 콘텐츠에서 실제 사용된 이미지 URL 추출
    const usedImageUrls = new Set<string>();
    const imgRegex = /<img[^>]+src="([^"]+)"/gi;
    let match;
    while ((match = imgRegex.exec(finalContent)) !== null) {
      usedImageUrls.add(match[1]);
    }
    
    // 실제 사용된 이미지만 필터링
    const usedImages = images.filter(img => usedImageUrls.has(img.url));
    
    console.log('수정 전송 데이터:', {
      title,
      content: finalContent,
      category,
      images: usedImages,
      originalImages: images,
      usedImageUrls: Array.from(usedImageUrls)
    });
    
    try {
      await axios.put(`/member/posts/${id}`, {
        title,
        content: finalContent,
        category,
        images: usedImages,
      });
      navigate(`/posts/${id}`);
    } catch (error) {
      console.error('게시글 수정 실패:', error);
      setError('수정 실패');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6 flex items-center justify-center">
        <div className="text-center">
          <div className="text-lg">로딩 중...</div>
        </div>
      </div>
    );
  }
  
  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6 flex items-center justify-center">
        <div className="text-center">
          <div className="text-red-400 text-lg mb-4">{error}</div>
          <button 
            onClick={() => navigate('/posts')}
            className="px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded transition"
          >
            게시글 목록으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white">
      {/* 헤더 섹션 */}
      <div className="relative overflow-hidden bg-gradient-to-r from-purple-900/50 to-pink-900/50 border-b border-purple-500/20">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/10 to-pink-600/10"></div>
        <div className="relative max-w-4xl mx-auto px-6 py-16">
          <div className="text-center">
            <div className="w-16 h-16 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-3xl mx-auto mb-6 shadow-lg">
              ✏️
            </div>
            <h1 className="text-4xl md:text-5xl font-bold bg-gradient-to-r from-white via-purple-200 to-white bg-clip-text text-transparent mb-4">
              게시글 수정
            </h1>
            <p className="text-xl text-gray-300">내용을 수정하고 다시 공유해보세요</p>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-6 py-8">
        <div className="bg-gradient-to-br from-slate-800/50 to-purple-900/30 backdrop-blur-md rounded-2xl p-8 border border-purple-500/20 shadow-2xl">

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">제목</label>
              <div className="relative">
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  maxLength={100}
                  placeholder="제목을 입력하세요..."
                  required
                  className="w-full px-4 py-3 rounded-xl bg-slate-700/50 text-white border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent placeholder-gray-400 transition-all duration-200"
                />
                <div className={`text-xs mt-1 ${title.length > 90 ? 'text-red-400' : 'text-gray-400'}`}>
                  {title.length}/100
                </div>
              </div>
            </div>
            <div>
              <div className="flex justify-between items-center mb-3">
                <label className="text-sm font-medium text-gray-300">내용</label>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setIsMarkdownMode(!isMarkdownMode)}
                    className={`flex items-center gap-2 px-4 py-2 ${isMarkdownMode ? 'bg-green-600/80 hover:bg-green-600' : 'bg-gray-600/80 hover:bg-gray-600'} text-white rounded-xl text-sm font-medium transition-all duration-200 shadow-lg transform hover:scale-105`}
                  >
                    📝 {isMarkdownMode ? '마크다운 ON' : '마크다운 OFF'}
                  </button>
                  <button
                    type="button"
                    onClick={handleImageUpload}
                    disabled={isImageChecking}
                    className="flex items-center gap-2 px-4 py-2 bg-blue-600/80 hover:bg-blue-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white rounded-xl text-sm font-medium transition-all duration-200 shadow-lg hover:shadow-blue-500/25 transform hover:scale-105 disabled:transform-none"
                  >
                    {isImageChecking ? (
                      <>
                        <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
                        검열 중...
                      </>
                    ) : (
                      <>
                        🖼️ 이미지
                      </>
                    )}
                  </button>
                </div>
              </div>
              <div className="rounded-xl overflow-hidden border border-slate-600/50">
                {isMarkdownMode ? (
                  <div className="space-y-4">
                    <div className="relative">
                      <textarea
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        maxLength={50000}
                        placeholder="마크다운으로 수정해보세요...&#10;&#10;예시:&#10;# 제목&#10;## 부제목&#10;**굵은 글씨**&#10;*기울임*&#10;- 리스트&#10;---&#10;[링크](URL)"
                        className="w-full h-96 px-4 py-3 rounded-xl bg-slate-700/50 text-white border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent placeholder-gray-400 resize-none font-mono text-sm"
                      />
                      <div className={`text-xs mt-1 ${content.length > 45000 ? 'text-red-400' : 'text-gray-400'}`}>
                        {content.length}/50,000
                      </div>
                    </div>
                    <div className="p-4 bg-slate-800/30 rounded-xl border border-slate-700/50">
                      <h3 className="text-sm font-medium text-gray-300 mb-3">📝 마크다운 미리보기:</h3>
                      <div 
                        className="prose prose-invert max-w-none min-h-[100px] p-3 bg-slate-900/30 rounded-lg border border-slate-600/30"
                        dangerouslySetInnerHTML={{ __html: parseMarkdown(content) }}
                      />
                    </div>
                  </div>
                ) : (
                  <ReactQuill
                    ref={editorRef}
                    value={content}
                    onChange={setContent}
                    theme="snow"
                    style={{ height: '400px', marginBottom: '50px' }}
                    modules={{
                      toolbar: [
                        [{ 'header': [1, 2, 3, false] }],
                        ['bold', 'italic', 'underline', 'strike'],
                        [{ 'color': [] }, { 'background': [] }],
                        [{ 'list': 'ordered'}, { 'list': 'bullet' }],
                        [{ 'align': [] }],
                        ['link', 'image', 'video'],
                        ['clean']
                      ]
                    }}
                    formats={[
                      'header', 'bold', 'italic', 'underline', 'strike',
                      'color', 'background', 'list', 'bullet', 'align',
                      'link', 'image', 'video', 'iframe'
                    ]}
                  />
                )}
              </div>
              
              {/* YouTube 영상 미리보기 */}
              {content.includes('iframe') && content.includes('youtube.com') && (
                <div className="mt-4 p-4 bg-slate-800/30 rounded-xl border border-slate-700/50">
                  <h3 className="text-sm font-medium text-gray-300 mb-3">🎬 YouTube 영상 미리보기:</h3>
                  <div 
                    className="prose prose-invert max-w-none"
                    dangerouslySetInnerHTML={{ 
                      __html: content.replace(
                        /<iframe[^>]*src="https:\/\/www\.youtube\.com\/embed\/([^"?]+)[^"]*"[^>]*>.*?<\/iframe>/gi,
                        (match, videoId) => {
                          const cleanVideoId = videoId.split('?')[0].split('&')[0];
                          return `
                            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 15px; border-radius: 8px; text-align: center; margin: 10px 0; border: 2px solid #8b5cf6;">
                              <div style="color: #fbbf24; font-size: 16px; margin-bottom: 10px; font-weight: bold;">🎬 YouTube 영상</div>
                              <div style="position: relative; display: inline-block; border-radius: 8px; overflow: hidden;">
                                <img src="https://img.youtube.com/vi/${cleanVideoId}/maxresdefault.jpg" 
                                     style="width: 100%; max-width: 400px; height: auto; cursor: pointer;"
                                     onclick="window.open('https://www.youtube.com/watch?v=${cleanVideoId}', '_blank');" 
                                     alt="YouTube 영상 썸네일" />
                                <div style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); background: rgba(255,0,0,0.9); border-radius: 50%; width: 60px; height: 60px; display: flex; align-items: center; justify-content: center; cursor: pointer;" onclick="window.open('https://www.youtube.com/watch?v=${cleanVideoId}', '_blank')">
                                  <div style="color: white; font-size: 20px; margin-left: 3px;">▶</div>
                                </div>
                              </div>
                              <div style="margin-top: 10px;">
                                <a href="https://www.youtube.com/watch?v=${cleanVideoId}" 
                                   target="_blank" 
                                   style="display: inline-block; background: #ef4444; color: white; padding: 8px 16px; border-radius: 6px; text-decoration: none; font-weight: bold; font-size: 12px;">
                                  🎥 YouTube에서 시청
                                </a>
                              </div>
                            </div>
                          `;
                        }
                      )
                    }}
                  />
                </div>
              )}
              <div className="text-xs text-gray-400 mt-2 p-3 bg-slate-800/30 rounded-lg border border-slate-700/50">
                {isMarkdownMode ? (
                  <>
                    📝 마크다운 모드: # 제목, **굵게**, *기울임*, - 리스트, --- 구분선, [링크](URL)<br/>
                    🎨 실시간 미리보기로 결과를 확인하며 수정하세요!<br/>
                    🔄 언제든 "마크다운 OFF" 버튼으로 리치 에디터로 전환 가능합니다
                  </>
                ) : (
                  <>
                    🎨 ReactQuill Editor: 강력한 리치 텍스트 에디터, 한글 지원 완벽!<br/>
                    🖼️ 이미지 붙여넣기: 이미지를 복사한 후 Ctrl+V로 바로 붙여넣을 수 있습니다!<br/>
                    🎬 YouTube 임베드: 비디오 버튼으로 YouTube 임베드 URL 삽입 가능 (width="100%" height="500")
                  </>
                )}
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">카테고리</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-700/50 text-white border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              >
                <option value="FREE">자유</option>
                <option value="DISCUSSION">토론</option>
                <option value="IMAGE">사진</option>
                <option value="REVIEW">후기</option>
                <option value="STARLIGHT_CINEMA">별빛 시네마</option>
              </select>
            </div>

            {/* 이미지 검열 중 알림 */}
            {isImageChecking && (
              <div className="p-4 bg-blue-500/10 border border-blue-500/20 rounded-xl text-blue-400 text-sm flex items-center gap-3">
                <div className="animate-spin w-5 h-5 border-2 border-blue-400 border-t-transparent rounded-full"></div>
                <div>
                  <div className="font-medium">🛡️ 이미지 검열 중...</div>
                  <div className="text-xs text-blue-300 mt-1">안전한 콘텐츠를 위해 이미지를 검사하고 있습니다. 잠시만 기다려주세요.</div>
                </div>
              </div>
            )}

            {/* 이미지 미리보기 */}
            {images.length > 0 && (
              <div className="space-y-2">
                <h3 className="text-sm font-medium text-gray-300 flex items-center gap-2">
                  업로드된 이미지:
                  <span className="text-xs bg-green-600/20 text-green-400 px-2 py-1 rounded-full border border-green-500/30">
                    ✓ 검열 완료
                  </span>
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                  {images.map((image, index) => (
                    <div key={index} className="relative group">
                      <img
                        src={image.url}
                        alt={image.originalName}
                        className="w-full h-24 object-cover rounded-lg shadow-md"
                        onError={(e) => {
                          console.error('이미진 로드 실패:', image.url);
                          e.currentTarget.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEyIDJMMTMuMDkgOC4yNkwyMCA5TDEzLjA5IDE1Ljc0TDEyIDIyTDEwLjkxIDE1Ljc0TDQgOUwxMC45MSA4LjI2TDEyIDJaIiBmaWxsPSIjOTk5Ii8+Cjwvc3ZnPgo=';
                        }}
                      />
                      <button
                        type="button"
                        onClick={() => removeImage(index)}
                        className="absolute top-1 right-1 bg-red-600 hover:bg-red-700 text-white rounded-full w-6 h-6 flex items-center justify-center text-xs opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        ×
                      </button>
                      <div className="absolute bottom-0 left-0 right-0 bg-black/50 text-white text-xs p-1 rounded-b-lg truncate">
                        {image.originalName}
                      </div>
                      <div className="absolute top-1 left-1 bg-green-600/80 text-white text-xs px-1 py-0.5 rounded flex items-center gap-1">
                        ✓ 검열완료
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {error && (
              <div className="p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm">
                {error}
              </div>
            )}
            
            <button
              type="submit"
              disabled={isImageChecking}
              className="w-full bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 disabled:from-gray-600 disabled:to-gray-700 disabled:cursor-not-allowed text-white font-semibold py-4 rounded-xl transition-all duration-200 transform hover:scale-105 disabled:transform-none shadow-lg hover:shadow-purple-500/25"
            >
              {isImageChecking ? (
                <div className="flex items-center justify-center gap-2">
                  <div className="animate-spin w-5 h-5 border-2 border-white border-t-transparent rounded-full"></div>
                  이미지 검열 중... 잠시만 기다려주세요
                </div>
              ) : (
                '✏️ 수정 완료'
              )}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
