import { useMemo } from 'react';
import MarkdownRenderer from './MarkdownRenderer';
import YouTubeEmbed from './YouTubeEmbed';
import { parseCinemaContent, type CinemaSectionKey } from './cinemaContentParser';

interface CinemaArticleContentProps {
  content: string;
}

const SECTION_PRESENTATION: Record<CinemaSectionKey, { title: string; icon: string }> = {
  selectionReason: { title: '오늘 이 영상을 선정한 이유', icon: '✨' },
  introduction: { title: '영상에서 만나볼 내용', icon: '🎬' },
  keyPoints: { title: '알고 보면 좋은 핵심 포인트', icon: '🔭' },
  recommendedFor: { title: '이런 분께 추천해요', icon: '👀' },
  source: { title: '채널 및 출처', icon: '📺' },
};

function SectionHeading({ sectionKey }: { sectionKey: CinemaSectionKey }) {
  const presentation = SECTION_PRESENTATION[sectionKey];
  return (
    <h2 className="mb-4 flex items-center gap-3 text-lg font-bold text-white sm:text-xl">
      <span
        aria-hidden="true"
        className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-fuchsia-300/30 bg-fuchsia-400/10 text-base"
      >
        {presentation.icon}
      </span>
      {presentation.title}
    </h2>
  );
}

export default function CinemaArticleContent({ content }: CinemaArticleContentProps) {
  const parsedContent = useMemo(() => parseCinemaContent(content), [content]);

  if (!parsedContent) {
    return <MarkdownRenderer content={content} />;
  }

  const { videoUrl, preamble, sections, additionalSections } = parsedContent;

  return (
    <article aria-label="별빛시네마 큐레이션" className="space-y-5 sm:space-y-6">
      <section className="overflow-hidden rounded-2xl border border-fuchsia-300/25 bg-black/35 p-2 shadow-xl shadow-fuchsia-950/20 sm:p-3">
        <YouTubeEmbed url={videoUrl} title="오늘의 별빛시네마 영상" />
      </section>

      {preamble && (
        <div className="rounded-2xl border border-slate-700/70 bg-slate-950/30 p-4 sm:p-5">
          <MarkdownRenderer content={preamble} />
        </div>
      )}

      {sections.selectionReason && (
        <section className="rounded-2xl border border-fuchsia-300/20 bg-gradient-to-br from-fuchsia-400/10 via-purple-400/5 to-transparent p-5 sm:p-7">
          <SectionHeading sectionKey="selectionReason" />
          <MarkdownRenderer content={sections.selectionReason} />
        </section>
      )}

      {(sections.introduction || sections.keyPoints) && (
        <div className="grid gap-5 md:grid-cols-2">
          {sections.introduction && (
            <section className="rounded-2xl border border-slate-600/50 bg-slate-900/45 p-5 sm:p-6">
              <SectionHeading sectionKey="introduction" />
              <MarkdownRenderer content={sections.introduction} />
            </section>
          )}
          {sections.keyPoints && (
            <section className="rounded-2xl border border-violet-400/20 bg-violet-500/5 p-5 sm:p-6">
              <SectionHeading sectionKey="keyPoints" />
              <MarkdownRenderer content={sections.keyPoints} />
            </section>
          )}
        </div>
      )}

      {sections.recommendedFor && (
        <section className="rounded-2xl border border-amber-300/20 bg-amber-400/5 p-5 sm:p-6">
          <SectionHeading sectionKey="recommendedFor" />
          <MarkdownRenderer content={sections.recommendedFor} />
        </section>
      )}

      {additionalSections.map((section, index) => (
        <section key={`${section.heading}-${index}`} className="rounded-2xl border border-slate-600/50 bg-slate-900/35 p-5 sm:p-6">
          <h2 className="mb-4 text-lg font-bold text-white sm:text-xl">{section.heading}</h2>
          <MarkdownRenderer content={section.content} />
        </section>
      ))}

      {sections.source && (
        <footer className="rounded-xl border border-slate-700/60 bg-slate-950/35 p-5 sm:p-6">
          <SectionHeading sectionKey="source" />
          <MarkdownRenderer content={sections.source} className="text-sm" />
        </footer>
      )}
    </article>
  );
}
