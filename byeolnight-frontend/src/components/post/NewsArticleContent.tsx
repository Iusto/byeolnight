import { useMemo } from 'react';
import MarkdownRenderer from './MarkdownRenderer';
import { parseNewsContent, type NewsSectionKey } from './newsContentParser';

interface NewsArticleContentProps {
  content: string;
}

const SECTION_PRESENTATION: Record<NewsSectionKey, { title: string; icon: string }> = {
  overview: { title: '한눈에 보기', icon: '✦' },
  facts: { title: '핵심 사실', icon: '✓' },
  importance: { title: '왜 중요한가', icon: '◎' },
  watchPoints: { title: '앞으로 볼 점', icon: '→' },
  source: { title: '출처', icon: '↗' },
};

function SectionHeading({ sectionKey }: { sectionKey: NewsSectionKey }) {
  const presentation = SECTION_PRESENTATION[sectionKey];

  return (
    <h2 className="mb-4 flex items-center gap-3 text-lg font-bold text-white sm:text-xl">
      <span
        aria-hidden="true"
        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-sky-300/30 bg-sky-400/10 text-sm text-sky-200"
      >
        {presentation.icon}
      </span>
      {presentation.title}
    </h2>
  );
}

export default function NewsArticleContent({ content }: NewsArticleContentProps) {
  const parsedContent = useMemo(() => parseNewsContent(content), [content]);

  if (!parsedContent) {
    return <MarkdownRenderer content={content} />;
  }

  const { preamble, sections, additionalSections } = parsedContent;

  return (
    <article aria-label="우주 뉴스 기사" className="space-y-5 sm:space-y-6">
      {preamble && (
        <div className="overflow-hidden rounded-2xl border border-slate-700/70 bg-slate-950/30 p-1 sm:p-2">
          <MarkdownRenderer content={preamble} />
        </div>
      )}

      <section className="rounded-2xl border border-sky-300/20 bg-gradient-to-br from-sky-400/10 via-indigo-400/5 to-transparent p-5 shadow-lg shadow-sky-950/20 sm:p-7">
        <SectionHeading sectionKey="overview" />
        <MarkdownRenderer content={sections.overview ?? ''} className="news-section-content" />
      </section>

      {(sections.facts || sections.importance) && (
        <div className="grid gap-5 md:grid-cols-2">
          {sections.facts && (
            <section className="rounded-2xl border border-slate-600/50 bg-slate-900/45 p-5 sm:p-6">
              <SectionHeading sectionKey="facts" />
              <MarkdownRenderer content={sections.facts} className="news-section-content" />
            </section>
          )}

          {sections.importance && (
            <section className="rounded-2xl border border-violet-400/20 bg-violet-500/5 p-5 sm:p-6">
              <SectionHeading sectionKey="importance" />
              <MarkdownRenderer content={sections.importance} className="news-section-content" />
            </section>
          )}
        </div>
      )}

      {sections.watchPoints && (
        <section className="rounded-2xl border border-amber-300/20 bg-amber-400/5 p-5 sm:p-6">
          <SectionHeading sectionKey="watchPoints" />
          <MarkdownRenderer content={sections.watchPoints} className="news-section-content" />
        </section>
      )}

      {additionalSections.map((section, index) => (
        <section
          key={`${section.heading}-${index}`}
          className="rounded-2xl border border-slate-600/50 bg-slate-900/35 p-5 sm:p-6"
        >
          <h2 className="mb-4 text-lg font-bold text-white sm:text-xl">{section.heading}</h2>
          {section.content && (
            <MarkdownRenderer content={section.content} className="news-section-content" />
          )}
        </section>
      ))}

      {sections.source && (
        <footer className="rounded-xl border border-slate-700/60 bg-slate-950/35 p-5 sm:p-6">
          <SectionHeading sectionKey="source" />
          <MarkdownRenderer content={sections.source} className="news-section-content text-sm" />
        </footer>
      )}
    </article>
  );
}
