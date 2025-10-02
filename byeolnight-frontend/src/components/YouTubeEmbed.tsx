import { useEffect, useRef } from 'react';

interface YouTubeEmbedProps {
  url: string;
  title?: string;
}

export default function YouTubeEmbed({ url, title = 'YouTube video' }: YouTubeEmbedProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  
  // YouTube URL에서 video ID 추출
  const getVideoId = (url: string): string | null => {
    const patterns = [
      /(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([^&\n?#]+)/,
      /youtube\.com\/v\/([^&\n?#]+)/
    ];
    
    for (const pattern of patterns) {
      const match = url.match(pattern);
      if (match) return match[1];
    }
    return null;
  };

  const videoId = getVideoId(url);
  
  if (!videoId) {
    return (
      <div className="bg-red-900/20 border border-red-500/30 rounded-lg p-4 my-4">
        <p className="text-red-300 text-sm">유효하지 않은 YouTube URL입니다.</p>
      </div>
    );
  }

  const embedUrl = `https://www.youtube.com/embed/${videoId}?rel=0&modestbranding=1`;

  return (
    <div 
      ref={containerRef}
      className="youtube-embed-container my-6"
      style={{
        position: 'relative',
        width: '100%',
        maxWidth: '100%',
        paddingBottom: '56.25%', // 16:9 비율
        height: 0,
        overflow: 'hidden',
        borderRadius: '12px',
        backgroundColor: '#000'
      }}
    >
      <iframe
        src={embedUrl}
        title={title}
        frameBorder="0"
        allowFullScreen
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          width: '100%',
          height: '100%',
          border: 'none'
        }}
      />
    </div>
  );
}