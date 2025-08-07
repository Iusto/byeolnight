import { useEffect, useState, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import TuiEditor, { isHandlingImageUpload } from '../components/TuiEditor';
import { sanitizeHtml } from '../utils/htmlSanitizer';

import { uploadImage } from '../lib/s3Upload';

interface FileDto {
  originalName: string;
  s3Key: string;
  url: string;
}

export default function PostEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, refreshToken } = useAuth();
  const { t } = useTranslation();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('NEWS');
  const [images, setImages] = useState<FileDto[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const editorRef = useRef<any>(null);
  
  const [isImageValidating, setIsImageValidating] = useState(false);
  
  // 모바일 환경 감지 함수
  const isMobile = () => {
    return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
  };

  // 클립보드 이미지 업로드 함수
  const uploadClipboardImage = async (file: File) => {
    // 파일 크기 체크 (10MB 제한으로 변경)
    if (file.size > 10 * 1024 * 1024) {
      // alert 제거하고 오류만 발생시킴
      return Promise.reject(new Error('파일 크기는 10MB를 초과할 수 없습니다. 이미지를 압축하거나 크기를 줄여주세요.'));
    }
    
    console.log('이미지 업로드 시작:', file.name, file.type, file.size);
    setIsImageValidating(true);
    
    try {
      console.log('이미지 업로드 요청 시작');
      
      // 통합된 s3Upload 유틸리티 사용
      const imageData = await uploadImage(file);
      
      console.log('이미지 업로드 완료:', imageData?.url ? '성공' : '실패');
      setImages(prev => [...prev, imageData]);
      
      return imageData; // imageData 전체를 반환
    } catch (error) {
      console.error('클립보드 이미지 업로드 실패:', error);
      console.error('오류 상세:', error.response?.status, error.response?.data);
      console.error('오류 메시지:', error.message);
      throw error;
    } finally {
      setIsImageValidating(false);
    }
  };
  
  // 클립보드 붙여넣기 이벤트 핸들러
  const handlePaste = async (event: ClipboardEvent) => {
    console.log('클립보드 붙여넣기 이벤트 발생');
    
    // TUI Editor에서 이미지 업로드를 처리 중이면 중복 처리 방지
    if (isHandlingImageUpload.current) {
      console.log('TUI Editor에서 이미지 업로드 처리 중 - 중복 처리 방지');
      return;
    }
    
    const items = event.clipboardData?.items;
    if (!items || items.length === 0) {
      console.log('클립보드 데이터 없음');
      return;
    }
    
    // 모바일 환경 감지
    const isMobileDevice = isMobile();
    console.log('클립보드 붙여넣기 - 모바일 환경:', isMobileDevice);
    
    console.log('클립보드 데이터 감지:', items.length, '개 항목');
    
    // 이미지 파일 찾기
    let imageFile: File | null = null;
    
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      console.log('클립보드 항목 형식:', item.type, item.kind);
      
      // 이미지 파일인지 확인 (더 엄격한 검사)
      if (item.kind === 'file' && item.type.startsWith('image/')) {
        imageFile = item.getAsFile();
        if (imageFile) {
          console.log('클립보드에서 이미지 감지:', imageFile.name, imageFile.type, imageFile.size);
          break;
        }
      }
    }
    
    if (!imageFile) {
      console.log('클립보드에 이미지 없음');
      return;
    }
    
    // 이미지가 있으면 기본 동작 방지
    event.preventDefault();
    
    // 파일 크기 체크 (10MB 제한)
    if (imageFile.size > 10 * 1024 * 1024) {
      setError('파일 크기는 10MB를 초과할 수 없습니다. 이미지를 압축하거나 크기를 줄여주세요.');
      return;
    }
    
    // 파일 형식 검사
    const validImageTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
    if (!validImageTypes.includes(imageFile.type)) {
      setError('지원되는 이미지 형식이 아닙니다. (jpg, png, gif, webp만 허용)');
      return;
    }
    
    try {
      // 모바일에서는 클립보드 붙여넣기 제한
      if (isMobileDevice) {
        setError('모바일에서는 이미지 붙여넣기가 제한될 수 있습니다. 이미지 버튼을 사용해주세요.');
        return;
      }
      
      const imageData = await uploadClipboardImage(imageFile);
      if (!imageData || !imageData.url) {
        throw new Error('이미지 URL을 받지 못했습니다.');
      }
      
      // 이미지 삽입 - TUI Editor 인스턴스를 통해 직접 삽입
      const instance = editorRef.current?.getInstance();
      if (instance) {
        console.log('TUI Editor 인스턴스를 통한 이미지 삽입');
        // 현재 커서 위치에 이미지 마크다운 삽입
        instance.insertText(`![클립보드 이미지](${imageData.url})`);
      } else {
        console.log('상태 업데이트를 통한 이미지 삽입');
        setContent(prev => prev + `![클립보드 이미지](${imageData.url})\n`);
      }
    } catch (error: any) {
      console.error('클립보드 이미지 업로드 실패:', error);
      setError(error.message || '이미지 업로드에 실패했습니다.');
    }
  };
  
  // 파일 선택 입력 요소를 참조하기 위한 ref 추가
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  const handleImageUpload = () => {
    console.log('이미지 업로드 버튼 클릭');
    
    // 모바일 환경 감지
    const isMobileDevice = isMobile();
    console.log('모바일 환경 감지:', isMobileDevice);
    
    // 기존 ref를 통해 파일 선택 대화상자 열기
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };
  
  // 파일 선택 시 호출되는 함수
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
      console.log('파일 선택 완료');
      const file = e.target.files?.[0];
      if (file) {
        console.log('선택된 파일:', file.name, file.type, file.size);
        // 파일 크기 체크 (10MB 제한으로 변경)
        if (file.size > 10 * 1024 * 1024) {
          // alert 제거하고 setError로 대체
          setError('파일 크기는 10MB를 초과할 수 없습니다. 이미지를 압축하거나 크기를 줄여주세요.');
          if (fileInputRef.current) {
            fileInputRef.current.value = '';
          }
          return;
        }
        
        // 파일 형식 검사
        const validImageTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
        if (!validImageTypes.includes(file.type)) {
          // alert 제거하고 setError로 대체
          setError('지원되는 이미지 형식이 아닙니다. (jpg, png, gif, webp만 허용)');
          if (fileInputRef.current) {
            fileInputRef.current.value = '';
          }
          return;
        }
        
        setIsImageValidating(true);
        try {
          console.log('이미지 업로드 요청 시작');
          
          // 통합된 s3Upload 유틸리티 사용
          const imageData = await uploadImage(file);
          
          console.log('이미지 업로드 완료:', imageData?.url ? '성공' : '실패');
          setImages(prev => [...prev, imageData]);
          
          // 이미지 URL 삽입 전 유효성 검사
          if (!imageData || !imageData.url) {
            throw new Error('이미지 URL을 받지 못했습니다.');
          }
          
          // 모바일에서는 에디터 참조 대신 상태 업데이트 사용
          const isMobileDevice = isMobile();
          if (isMobileDevice || !editorRef.current || !editorRef.current.getInstance) {
            console.log('상태 업데이트를 통한 이미지 삽입 (모바일 모드)');
            setContent(prev => prev + `![${imageData.originalName || '이미지'}](${imageData.url})\n`);
          } else {
            // PC에서는 에디터 참조 사용
            console.log('에디터 참조를 통한 이미지 삽입');
            const instance = editorRef.current.getInstance();
            if (instance) {
              instance.insertText(`![${imageData.originalName || '이미지'}](${imageData.url})`);
            }
          }
        } catch (error: any) {
          console.error('이미지 업로드 실패:', error);
          console.error('오류 상세:', error.response?.status, error.response?.data);
          console.error('오류 메시지:', error.message);
          
          const errorMsg = error.response?.data?.message || error.message || '이미지 업로드에 실패했습니다.';
          // alert 제거하고 setError로 대체
          setError(errorMsg);
        } finally {
          setIsImageValidating(false);
          // 파일 입력 초기화 (동일한 파일 재선택 가능하도록)
          if (fileInputRef.current) {
            fileInputRef.current.value = '';
          }
        }
      } else {
        // 파일 선택 취소 시 초기화
        console.log('파일 선택 취소');
        if (fileInputRef.current) {
          fileInputRef.current.value = '';
        }
      }
  };
  
  const removeImage = (index: number) => {
    const imageToRemove = images[index];
    if (imageToRemove) {
      if (editorRef.current?.getInstance) {
        // TUI Editor의 인스턴스를 통해 현재 콘텐츠 가져오기
        const instance = editorRef.current.getInstance();
        if (instance) {
          const currentContent = instance.getMarkdown();
          // 마크다운 형식의 이미지 제거
          const escapedUrl = imageToRemove.url.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
          const imgRegex = new RegExp(`!\[[^\]]*\]\(${escapedUrl}\)`, 'gi');
          const newContent = currentContent.replace(imgRegex, '');
          // 업데이트된 콘텐츠 적용
          instance.setMarkdown(newContent);
          console.log('TUI Editor 인스턴스를 통해 이미지 제거 완료');
        }
      } else {
        try {
          setContent(prev => {
            const escapedUrl = imageToRemove.url.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            const imgRegex = new RegExp(`<img[^>]*src="${escapedUrl}"[^>]*>(<br>)?|!\[[^\]]*\]\(${escapedUrl}\)`, 'gi');
            const newContent = prev.replace(imgRegex, '');
            return newContent || prev;
          });
        } catch (error) {
          console.error('이미지 제거 중 오류 발생:', error);
        }
      }
    }
    // 이미지 배열에서 해당 이미지 제거
    setImages(prev => prev.filter((_, i) => i !== index));
  };

  useEffect(() => {
    const fetchPost = async () => {
      // 사용자 정보가 없는 경우 토큰 갱신 시도
      if (!user) {
        const token = localStorage.getItem('accessToken');
        const rememberMe = localStorage.getItem('rememberMe');
        
        // 토큰이 있고 로그인 유지 옵션이 활성화된 경우 토큰 갱신 시도
        if (token && rememberMe === 'true') {
          try {
            console.log('토큰 갱신 시도 중...');
            const refreshSuccess = await refreshToken();
            if (!refreshSuccess) {
              setError('로그인이 필요합니다.');
              setLoading(false);
              return;
            }
            // 토큰 갱신 성공 시 사용자 정보가 업데이트되었으므로 계속 진행
          } catch (err) {
            console.error('토큰 갱신 실패:', err);
            setError('로그인이 필요합니다.');
            setLoading(false);
            return;
          }
        } else {
          setError('로그인이 필요합니다.');
          setLoading(false);
          return;
        }
      }
      
      // 여기서 user가 여전히 null일 수 있으므로 다시 확인
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
        // 기존 이미지들을 FileDto 형식으로 변환하여 설정
        const existingImages = (post.images || []).map(img => ({
          originalName: img.originalName || '기존 이미지',
          s3Key: img.s3Key || '',
          url: img.url
        }));
        console.log('기존 이미지 로드:', existingImages);
        setImages(existingImages);
        
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
  }, [id, user, refreshToken]);
  
  // 컴포넌트 마운트 시 이벤트 리스너 등록
  useEffect(() => {
    document.addEventListener('paste', handlePaste);
    
    // TUI Editor에서 발생하는 이미지 검열 이벤트 수신
    const handleImageValidating = (e: CustomEvent) => {
      const { validating } = e.detail;
      setIsImageValidating(validating);
    };
    
    document.addEventListener('imageValidating', handleImageValidating as EventListener);
    
    return () => {
      document.removeEventListener('paste', handlePaste);
      document.removeEventListener('imageValidating', handleImageValidating as EventListener);
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
    
    // HTML 보안 검증
    const finalContent = sanitizeHtml(content);
    
    // 콘텐츠에서 실제 사용된 이미지 URL 추출
    const usedImageUrls = new Set<string>();
    
    // HTML 태그 형식 이미지 추출
    const htmlImgRegex = /<img[^>]+src="([^"]+)"/gi;
    let htmlMatch;
    while ((htmlMatch = htmlImgRegex.exec(finalContent)) !== null) {
      usedImageUrls.add(htmlMatch[1]);
    }
    
    // 마크다운 형식 이미지 추출
    const mdImgRegex = /!\[[^\]]*\]\(([^)]+)\)/gi;
    let mdMatch;
    while ((mdMatch = mdImgRegex.exec(finalContent)) !== null) {
      usedImageUrls.add(mdMatch[1]);
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
      {/* 파일 선택 입력 요소 - 화면에 보이지 않지만 React에서 관리 */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        onChange={handleFileChange}
        style={{ display: 'none' }}
      />
      {/* 헤더 섹션 */}
      <div className="relative overflow-hidden bg-gradient-to-r from-purple-900/50 to-pink-900/50 border-b border-purple-500/20">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/10 to-pink-600/10"></div>
        <div className="relative max-w-4xl mx-auto px-6 py-16">
          <div className="text-center">
            <div className="w-16 h-16 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-3xl mx-auto mb-6 shadow-lg">
              ✏️
            </div>
            <h1 className="text-4xl md:text-5xl font-bold bg-gradient-to-r from-white via-purple-200 to-white bg-clip-text text-transparent mb-4">
              {t('home.post_edit')}
            </h1>
            <p className="text-xl text-gray-300">{t('home.edit_and_share')}</p>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-6 py-8">
        <div className="bg-gradient-to-br from-slate-800/50 to-purple-900/30 backdrop-blur-md rounded-2xl p-8 border border-purple-500/20 shadow-2xl">

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">{t('post.title')}</label>
              <div className="relative">
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  maxLength={100}
                  placeholder={t('home.title_placeholder')}
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
                <label className="text-sm font-medium text-gray-300">{t('post.content')}</label>
                <div className="flex gap-2">

                  <button
                    type="button"
                    onClick={handleImageUpload}
                    disabled={isImageValidating}
                    className="flex items-center gap-2 px-4 py-2 bg-blue-600/80 hover:bg-blue-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white rounded-xl text-sm font-medium transition-all duration-200 shadow-lg hover:shadow-blue-500/25 transform hover:scale-105 disabled:transform-none"
                  >
                    {isImageValidating ? (
                      <>
                        <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
                        {t('home.validating')}
                      </>
                    ) : (
                      <>
                        🖼️ {t('home.add_image')}
                      </>
                    )}
                  </button>
                </div>
              </div>
              <div className="rounded-xl overflow-hidden border border-slate-600/50 quill-wrapper" style={{ height: '500px', display: 'flex', flexDirection: 'column' }}>
                <TuiEditor
                  ref={editorRef}
                  value={content}
                  onChange={(newContent) => {
                    setContent(newContent);
                  }}
                  placeholder={t('home.content_placeholder')}
                  height="500px"
                  handleImageUpload={handleImageUpload}
                />
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
                🎨 {t('home.editor_info_1')}<br/>
                🖼️ {t('home.editor_info_2')}<br/>
                🛡️ {t('home.editor_info_3')}<br/>
                🎬 {t('home.editor_info_4')}
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">{t('home.category')}</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-700/50 text-white border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              >
                <option value="FREE">{t('home.free')}</option>
                <option value="DISCUSSION">{t('home.discussion')}</option>
                <option value="IMAGE">{t('home.star_photo')}</option>
                <option value="REVIEW">{t('home.review')}</option>
                <option value="STARLIGHT_CINEMA">{t('home.star_cinema')}</option>
              </select>
            </div>

            {/* 이미지 검열 중 알림 */}
            {isImageValidating && (
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
                          console.error('이미지 로드 실패:', image.url);
                          // 기본 이미지로 대체
                          e.currentTarget.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEyIDJMMTMuMDkgOC4yNkwyMCA5TDEzLjA5IDE1Ljc0TDEyIDIyTDEwLjkxIDE1Ljc0TDQgOUwxMC45MSA4LjI2TDEyIDJaIiBmaWxsPSIjOTk5Ii8+Cjwvc3ZnPgo=';
                        }}
                        onLoad={() => {
                          console.log('이미지 로드 성공:', image.url);
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
              disabled={isImageValidating}
              className="w-full bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 disabled:from-gray-600 disabled:to-gray-700 disabled:cursor-not-allowed text-white font-semibold py-4 rounded-xl transition-all duration-200 transform hover:scale-105 disabled:transform-none shadow-lg hover:shadow-purple-500/25"
            >
              {isImageValidating ? (
                <div className="flex items-center justify-center gap-2">
                  <div className="animate-spin w-5 h-5 border-2 border-white border-t-transparent rounded-full"></div>
                  {t('home.image_validating')}
                </div>
              ) : (
                `✏️ ${t('home.edit_complete')}`
              )}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
