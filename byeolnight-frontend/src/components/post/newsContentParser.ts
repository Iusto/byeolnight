export type NewsSectionKey = 'overview' | 'facts' | 'importance' | 'watchPoints' | 'source';

export interface AdditionalNewsSection {
  heading: string;
  content: string;
}

export interface ParsedNewsContent {
  preamble: string;
  sections: Partial<Record<NewsSectionKey, string>>;
  additionalSections: AdditionalNewsSection[];
}

const SECTION_ALIASES: Record<string, NewsSectionKey> = {
  '한눈에 보기': 'overview',
  '뉴스 요약': 'overview',
  '핵심 사실': 'facts',
  '왜 중요한가': 'importance',
  'ai 분석': 'importance',
  '앞으로 볼 점': 'watchPoints',
  '관전 포인트': 'watchPoints',
  '출처': 'source',
  '출처 및 원문': 'source',
  '원문 보기': 'source',
};

const HEADING_PATTERN = /^#{2,3}\s+(.+?)\s*$/gm;

function normalizeHeading(heading: string) {
  return heading
    .replace(/[\p{Extended_Pictographic}\p{Emoji_Presentation}\uFE0F]/gu, '')
    .replace(/[*_`]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .toLocaleLowerCase('ko-KR');
}

function getSectionKey(heading: string): NewsSectionKey | null {
  return SECTION_ALIASES[normalizeHeading(heading)] ?? null;
}

function appendSection(
  sections: Partial<Record<NewsSectionKey, string>>,
  key: NewsSectionKey,
  content: string,
) {
  if (!content) return;
  sections[key] = sections[key] ? `${sections[key]}\n\n${content}` : content;
}

function splitSourceContent(content: string) {
  const separatorMatch = /^\s*---\s*$/m.exec(content);
  if (separatorMatch?.index !== undefined) {
    const sourceStart = separatorMatch.index + separatorMatch[0].length;
    const source = content.slice(sourceStart).trim();
    if (/\*\*출처:\*\*|원문 기사|발행일/.test(source)) {
      return {
        body: content.slice(0, separatorMatch.index).trim(),
        source,
      };
    }
  }

  const sourceMatch = /^\s*\*\*출처:\*\*/m.exec(content);
  if (sourceMatch?.index !== undefined) {
    return {
      body: content.slice(0, sourceMatch.index).trim(),
      source: content.slice(sourceMatch.index).trim(),
    };
  }

  return { body: content.trim(), source: '' };
}

export function parseNewsContent(content: string): ParsedNewsContent | null {
  const matches = Array.from(content.matchAll(HEADING_PATTERN));
  const sections: Partial<Record<NewsSectionKey, string>> = {};
  const additionalSections: AdditionalNewsSection[] = [];
  const firstRecognizedMatch = matches.find((match) => getSectionKey(match[1]) !== null);
  const firstHeadingIndex = matches[0]?.index;

  matches.forEach((match, index) => {
    if (match.index === undefined) return;

    const contentStart = match.index + match[0].length;
    const contentEnd = matches[index + 1]?.index ?? content.length;
    const sectionContent = content.slice(contentStart, contentEnd).trim();
    const key = getSectionKey(match[1]);

    if (!key) {
      additionalSections.push({ heading: match[1].trim(), content: sectionContent });
      return;
    }

    if (key === 'watchPoints') {
      const { body, source } = splitSourceContent(sectionContent);
      appendSection(sections, key, body);
      appendSection(sections, 'source', source);
      return;
    }

    appendSection(sections, key, sectionContent);
  });

  const sectionCount = Object.keys(sections).length;
  if (!sections.overview || sectionCount < 2 || firstRecognizedMatch?.index === undefined) {
    return null;
  }

  return {
    preamble: content.slice(0, firstHeadingIndex ?? firstRecognizedMatch.index).trim(),
    sections,
    additionalSections,
  };
}
