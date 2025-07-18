import { useState, useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
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

export default function PostCreate() {
  const navigate = useNavigate();
  const { user, refreshUserInfo } = useAuth();
  const [searchParams] = useSearchParams();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('DISCUSSION');
  const [error, setError] = useState('');
  const [uploadedImages, setUploadedImages] = useState<FileDto[]>([]);
  const [isImageChecking, setIsImageChecking] = useState(false);
  const [isMarkdownMode, setIsMarkdownMode] = useState(false);
  const editorRef = useRef<any>(null);
  
  // URL 파라미터에서 originTopic 추출
  const originTopicId = searchParams.get('originTopic');
  
  // 클립보드 이미지 업로드 함수 (검열 포함)
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
      
      // 명시적으로 Content-Type 설정 안함 (브라우저가 자동으로 설정)
      const response = await axios.post('/files/upload-image', formData);
      
      const imageData = response.data.data;
      setUploadedImages(prev => [...prev, imageData]);
      
      return imageData.url;
    } catch (error: any) {
      console.error('클립보드 이미지 업로드 실패:', error);
      const errorMsg = error.response?.data?.message || '이미지 업로드에 실패했습니다.';
      alert(errorMsg);
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
            // ReactQuill에 이미지 삽입 (영구 URL 사용)
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
  
  // 컴포넌트 마운트 시 이벤트 리스너 등록
  useEffect(() => {
    document.addEventListener('paste', handlePaste);
    return () => {
      document.removeEventListener('paste', handlePaste);
    };
  }, []);
  
  // 모바일 환경 감지 함수
  const isMobile = () => {
    return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
  };

  const handleImageUpload = () => {
    const input = document.createElement('input');
    input.setAttribute('type', 'file');
    input.setAttribute('accept', 'image/*');
    
    // 모바일에서 갤러리 접근을 위해 capture 속성을 설정하지 않음
    // capture 속성이 있으면 카메라가 열리므로 완전히 제거
    input.removeAttribute('capture'); // 명시적으로 제거
    
    // 실제 DOM에 추가하여 모바일에서도 작동하도록 함
    document.body.appendChild(input);
    input.style.display = 'none';
    
    // iOS Safari에서 클릭 이벤트가 제대로 작동하지 않는 문제 해결
    setTimeout(() => {
      input.click();
    }, 100);
    
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
          
          // 명시적으로 Content-Type 설정 안함 (브라우저가 자동으로 설정)
          const response = await axios.post('/files/upload-image', formData);
          
          const imageData = response.data.data;
          setUploadedImages(prev => [...prev, imageData]);
          
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
    const imageToRemove = uploadedImages[index];
    if (imageToRemove) {
      // 게시글 내용에서도 해당 이미지 제거
      setContent(prev => {
        const imgRegex = new RegExp(`<img[^>]*src="${imageToRemove.url.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}"[^>]*>`, 'gi');
        return prev.replace(imgRegex, '');
      });
    }
    setUploadedImages(prev => prev.filter((_, i) => i !== index));
  };

  // URL 파라미터에서 고정 카테고리 설정
  const fixedCategory = searchParams.get('fixedCategory');
  const isFixedCategory = fixedCategory && ['DISCUSSION', 'IMAGE', 'REVIEW', 'FREE', 'NOTICE', 'NEWS', 'STARLIGHT_CINEMA'].includes(fixedCategory);
  
  useEffect(() => {
    if (isFixedCategory) {
      setCategory(fixedCategory);
    }
  }, [fixedCategory, isFixedCategory]);



  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!user) {
      setError('로그인이 필요합니다.');
      return;
    }
    
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
    
    try {
      const response = await axios.post('/member/posts', {
        title,
        content: finalContent,
        category,
        images: uploadedImages,
        originTopicId: originTopicId ? parseInt(originTopicId) : null
      });
      
      console.log('게시글 작성 완료:', response.data);
      
      // 공지글인 경우 알림 생성 확인
      if (category === 'NOTICE') {
        console.log('공지글 작성 완료 - 알림 생성 대기 중...');
        // 3초 후 알림 확인
        setTimeout(async () => {
          try {
            const notificationResponse = await axios.get('/member/notifications/unread/count');
            console.log('공지글 작성 후 알림 개수:', notificationResponse.data);
          } catch (err) {
            console.error('알림 확인 실패:', err);
          }
        }, 3000);
      }
      
      // 사용자 정보 새로고침 (포인트 업데이트)
      await refreshUserInfo();
      
      // 해당 카테고리 게시판으로 이동
      navigate(`/posts?category=${category}&sort=recent`);
    } catch (err: any) {
      const msg = err?.response?.data?.message || '게시글 작성 실패';
      setError(msg);
    }
  };

  // 로그인 검증
  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] flex items-center justify-center text-white">
        <div className="text-center">
          <p className="text-lg mb-4">게시글 작성은 로그인이 필요합니다.</p>
          <button 
            onClick={() => navigate('/login')}
            className="bg-purple-600 hover:bg-purple-700 px-4 py-2 rounded"
          >
            로그인 하러 가기
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
              📝
            </div>
            <h1 className="text-4xl md:text-5xl font-bold bg-gradient-to-r from-white via-purple-200 to-white bg-clip-text text-transparent mb-4">
              게시글 작성
            </h1>
            {originTopicId && (
              <div className="inline-flex items-center gap-3 px-6 py-3 bg-blue-600/20 border border-blue-400/30 rounded-full text-blue-200 backdrop-blur-sm">
                <span className="text-blue-400">💬</span>
                <span>오늘의 토론 주제에 대한 의견글을 작성 중입니다</span>
              </div>
            )}
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
                  placeholder="제목을 입력하세요..."
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  maxLength={100}
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
                        placeholder="마크다운으로 작성해보세요...&#10;&#10;예시:&#10;# 제목&#10;## 부제목&#10;**굵은 글씨**&#10;*기울임*&#10;- 리스트&#10;---&#10;[링크](URL)"
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
                      toolbar: {
                        container: [
                          [{ 'header': [1, 2, 3, false] }],
                          ['bold', 'italic', 'underline', 'strike'],
                          [{ 'color': [] }, { 'background': [] }],
                          [{ 'list': 'ordered'}, { 'list': 'bullet' }],
                          [{ 'align': [] }],
                          ['link', 'image', 'video'],
                          ['clean']
                        ],
                        handlers: {
                          // 이미지 버튼 클릭 시 사용자 정의 함수 실행
                          image: handleImageUpload
                        }
                      },
                      clipboard: {
                        matchVisual: false
                      }
                    }}
                    formats={[
                      'header', 'bold', 'italic', 'underline', 'strike',
                      'color', 'background', 'list', 'bullet', 'align',
                      'link', 'image', 'video', 'iframe'
                    ]}
                    placeholder="내용을 입력하세요..."
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
                    🎨 실시간 미리보기로 결과를 확인하며 작성하세요!<br/>
                    🔄 언제든 "마크다운 OFF" 버튼으로 리치 에디터로 전환 가능합니다
                  </>
                ) : (
                  <>
                    🎨 ReactQuill Editor: 강력한 리치 텍스트 에디터, 한글 지원 완벽!<br/>
                    🖼️ 이미지 붙여넣기: 이미지를 복사한 후 Ctrl+V로 바로 붙여넣을 수 있습니다!<br/>
                    🛡️ 이미지 검열: 업로드된 모든 이미지는 자동으로 검열되어 안전한 콘텐츠만 허용됩니다<br/>
                    🎬 YouTube 임베드: 비디오 버튼으로 YouTube 임베드 URL 삽입 가능 (width="100%" height="500")
                  </>
                )}
              </div>
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

          {/* 업로드된 이미지 미리보기 */}
          {uploadedImages.length > 0 && (
            <div className="space-y-2">
              <h3 className="text-sm font-medium text-gray-300 flex items-center gap-2">
                업로드된 이미지:
                <span className="text-xs bg-green-600/20 text-green-400 px-2 py-1 rounded-full border border-green-500/30">
                  ✓ 검열 완료
                </span>
              </h3>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                {uploadedImages.map((image, index) => (
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
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">카테고리</label>
              {isFixedCategory ? (
                <div className="w-full px-4 py-3 rounded-xl bg-slate-700/50 text-gray-300 border border-slate-600/50">
                  {{
                    DISCUSSION: '토론',
                    IMAGE: '사진', 
                    REVIEW: '후기',
                    FREE: '자유',
                    NOTICE: '공지',
                    NEWS: '뉴스',
                    STARLIGHT_CINEMA: '별빛 시네마'
                  }[category]} (고정)
                </div>
              ) : (
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-700/50 text-white border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                >
                  <option value="DISCUSSION">토론</option>
                  <option value="IMAGE">사진</option>
                  <option value="REVIEW">후기</option>
                  <option value="FREE">자유</option>
                  <option value="STARLIGHT_CINEMA">별빛 시네마</option>
                  {user?.role === 'ADMIN' && (
                    <option value="NEWS">뉴스</option>
                  )}
                </select>
              )}
            </div>

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
                '🚀 게시글 등록'
              )}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
