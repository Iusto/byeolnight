import { useState, useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import TuiEditor from '../components/TuiEditor';
import { sanitizeHtml } from '../utils/htmlSanitizer';
import { parseMarkdown } from '../utils/markdownParser';
import { uploadImage } from '../lib/s3Upload';

// 개발 환경에서 API 경로 로깅
console.log('API 기본 URL:', import.meta.env.VITE_API_BASE_URL || '/api');

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
  const [validationAlert, setValidationAlert] = useState<{message: string, type: 'success' | 'error' | 'warning', imageUrl?: string} | null>(null);
  const editorRef = useRef<any>(null);
  
  // URL 파라미터에서 originTopic 추출
  const originTopicId = searchParams.get('originTopic');
  
  // 클립보드 이미지 업로드 함수 (간소화 버전)
  const uploadClipboardImage = async (file: File) => {
    setIsImageChecking(true);
    try {
      const imageData = await uploadImage(file, false);
      setUploadedImages(prev => [...prev, imageData]);
      return imageData.url;
    } catch (error: any) {
      const errorMsg = error.message || '이미지 업로드에 실패했습니다.';
      alert(errorMsg);
      throw error;
    } finally {
      setIsImageChecking(false);
    }
  };
  
  // URL로 이미지 제거 (안전한 방식으로 수정)
  const removeImageByUrl = (imageUrl: string) => {
    // 업로드된 이미지 목록에서 제거
    setUploadedImages(prev => prev.filter(img => img.url !== imageUrl));
    
    // 에디터에서 이미지 제거 - 마크다운 모드일 경우
    if (isMarkdownMode) {
      setContent(prev => {
        const escapedUrl = imageUrl.replace(/[.*+?^${}()|[\\]\\\\]/g, '\\\\$&');
        const imgRegex = new RegExp(`!\\[[^\\]]*\\]\\(${escapedUrl}\\)|<img[^>]*src=\"${escapedUrl}\"[^>]*>(<br>)?`, 'gi');
        return prev.replace(imgRegex, '');
      });
      return;
    }
    
    // 에디터 모드일 경우 - 안전한 방식으로 제거
    try {
      // 현재 콘텐츠를 문자열로 처리
      setContent(prev => {
        const escapedUrl = imageUrl.replace(/[.*+?^${}()|[\\]\\\\]/g, '\\\\$&');
        const imgRegex = new RegExp(`<img[^>]*src=\"${escapedUrl}\"[^>]*>(<br>)?`, 'gi');
        return prev.replace(imgRegex, '');
      });
    } catch (error) {
      console.error('이미지 제거 중 오류:', error);
    }
  };
  
  // 클립보드 붙여넣기 이벤트 핸들러 (간소화 버전)
  const handlePaste = async (event: ClipboardEvent) => {
    try {
      const items = event.clipboardData?.items;
      if (!items) return;
      
      // 모바일 환경 감지
      const isMobileDevice = isMobile();
      
      // 모바일에서 클립보드 접근 제한 있을 수 있음
      if (isMobileDevice && items.length === 0) return;
      
      for (let i = 0; i < items.length; i++) {
        const item = items[i];
        
        // 이미지 파일인지 확인
        if (item.type.indexOf('image') !== -1) {
          event.preventDefault();
          const file = item.getAsFile();
          if (!file) continue;
          
          // 모바일에서는 클립보드 붙여넣기 제한
          if (isMobileDevice) {
            alert('모바일에서는 이미지 붙여넣기가 제한될 수 있습니다. 이미지 버튼을 사용해주세요.');
            return;
          }
          
          try {
            const imageUrl = await uploadClipboardImage(file);
            if (!imageUrl) throw new Error('이미지 URL을 받지 못했습니다.');
            
            // 에디터에 이미지 삽입
            insertImageToEditor(imageUrl, '클립보드 이미지');
          } catch (error) {
            console.error('클립보드 이미지 업로드 실패:', error);
          }
          break;
        }
      }
    } catch (error) {
      console.error('클립보드 처리 중 오류:', error);
      setIsImageChecking(false);
    }
  };
  
  // 에디터에 이미지 삽입 함수 (TUI Editor용으로 수정)
  const insertImageToEditor = (imageUrl: string, altText: string) => {
    // 마크다운 모드일 경우 문자열로 추가
    if (isMarkdownMode) {
      setContent(prev => prev + `<img src="${imageUrl}" alt="${altText}" style="max-width: 100%; height: auto;" /><br/>`);
      return;
    }
    
    // TUI Editor에 이미지 삽입
    try {
      if (editorRef.current && editorRef.current.insertContent) {
        // TUI Editor API 사용
        editorRef.current.insertContent(`<img src="${imageUrl}" alt="${altText}" style="max-width: 100%; height: auto;" />`);
      } else {
        // 에디터 참조가 없는 경우 상태 업데이트
        setContent(prev => prev + `<img src="${imageUrl}" alt="${altText}" style="max-width: 100%; height: auto;" /><br/>`);
      }
    } catch (error) {
      console.error('이미지 삽입 중 오류:', error);
      // 오류 발생 시 상태 업데이트로 폴백
      setContent(prev => prev + `<img src="${imageUrl}" alt="${altText}" style="max-width: 100%; height: auto;" /><br/>`);
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
    input.removeAttribute('capture');
    
    document.body.appendChild(input);
    input.style.display = 'none';
    
    // iOS Safari에서 클릭 이벤트가 제대로 작동하지 않는 문제 해결
    setTimeout(() => input.click(), 100);
    
    input.onchange = async () => {
      const file = input.files?.[0];
      if (!file) {
        document.body.removeChild(input);
        return;
      }
      
      // 파일 형식 검사
      const validImageTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
      if (!validImageTypes.includes(file.type)) {
        alert('지원되는 이미지 형식이 아닙니다. (jpg, png, gif, webp만 허용)');
        document.body.removeChild(input);
        return;
      }
      
      setIsImageChecking(true);
      try {
        const imageData = await uploadImage(file);
        
        if (!imageData || !imageData.url) {
          throw new Error('이미지 URL을 받지 못했습니다.');
        }
        
        // 업로드된 이미지 목록에 추가
        setUploadedImages(prev => [...prev, imageData]);

        // 이미지를 에디터에 삽입
        insertImageToEditor(imageData.url, imageData.originalName || '이미지');
        
      } catch (error: any) {
        const errorMsg = error.message || '이미지 업로드에 실패했습니다.';
        alert(errorMsg);
      } finally {
        setIsImageChecking(false);
        if (document.body.contains(input)) {
          document.body.removeChild(input);
        }
      }
    };
  };
  
  const removeImage = (index: number) => {
    const imageToRemove = uploadedImages[index];
    if (imageToRemove) {
      // 이미 구현된 removeImageByUrl 함수 활용
      removeImageByUrl(imageToRemove.url);
    }
    // 이미지 배열에서 해당 이미지 제거
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
              <div className="rounded-xl overflow-hidden border border-slate-600/50 quill-wrapper">
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
                  <div className="quill-container" style={{ height: '500px', display: 'flex', flexDirection: 'column' }}>
                    {/* TUI 에디터로 교체 */}
                    <TuiEditor
                      ref={editorRef}
                      value={content}
                      onChange={(newContent) => {
                        setContent(newContent);
                      }}
                      placeholder="내용을 입력하세요..."
                      height="500px"
                      handleImageUpload={handleImageUpload}
                    />
                  </div>
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
                    🎨 Toast UI Editor: 한국에서 개발한 강력한 에디터, 한글 지원 완벽!<br/>
                    🖼️ 이미지 붙여넣기: 이미지를 복사한 후 Ctrl+V로 바로 붙여넣을 수 있습니다!<br/>
                    🛡️ 이미지 검열: 업로드된 모든 이미지는 자동으로 검열되어 안전한 콘텐츠만 허용됩니다<br/>
                    🎬 마크다운/WYSIWYG 모드: 두 가지 모드를 지원하여 편리한 편집 가능
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
          
          {/* 검열 결과 알림 */}
          {validationAlert && (
            <div className={`p-4 ${validationAlert.type === 'error' ? 'bg-red-500/10 border-red-500/20 text-red-400' : validationAlert.type === 'warning' ? 'bg-yellow-500/10 border-yellow-500/20 text-yellow-400' : 'bg-green-500/10 border-green-500/20 text-green-400'} border rounded-xl text-sm flex items-center gap-3 animate-fadeIn`}>
              <div className={`${validationAlert.type === 'error' ? 'text-red-400' : validationAlert.type === 'warning' ? 'text-yellow-400' : 'text-green-400'} text-xl`}>
                {validationAlert.type === 'error' ? '⚠️' : validationAlert.type === 'warning' ? '⚠️' : '✅'}
              </div>
              <div>
                <div className="font-medium">{validationAlert.message}</div>
                {validationAlert.type === 'error' && (
                  <div className="text-xs mt-1">
                    이미지가 자동으로 삭제되었습니다. 다른 이미지를 사용해주세요.
                  </div>
                )}
              </div>
              <button 
                onClick={() => setValidationAlert(null)} 
                className="ml-auto text-sm hover:text-white transition-colors"
              >
                ×
              </button>
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