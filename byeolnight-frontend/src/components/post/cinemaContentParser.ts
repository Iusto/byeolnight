export type CinemaSectionKey = 'selectionReason' | 'introduction' | 'keyPoints' | 'recommendedFor' | 'source';

export interface AdditionalCinemaSection {
  heading: string;
  content: string;
}

export interface ParsedCinemaContent {
  videoUrl: string;
  preamble: string;
  sections: Partial<Record<CinemaSectionKey, string>>;
  additionalSections: AdditionalCinemaSection[];
}

const SECTION_ALIASES: Record<string, CinemaSectionKey> = {
  '오늘 이 영상을 선정한 이유': 'selectionReason',
  '선정 이유': 'selectionReason',
  '왜 이 영상인가요': 'selectionReason',
  '영상에서 만나볼 내용': 'introduction',
  '영상 소개': 'introduction',
  '한눈에 보기': 'introduction',
  '알고 보면 좋은 핵심 포인트': 'keyPoints',
  '핵심 포인트': 'keyPoints',
  '관전 포인트': 'keyPoints',
  '이런 분께 추천해요': 'recommendedFor',
  '추천 대상': 'recommendedFor',
  '출처': 'source',
  '영상 정보': 'source',
  '채널 및 출처': 'source',
};

const HEADING_PATTERN = /^#{2,3}\s+(.+?)\s*$/gm;
const YOUTUBE_URL_PATTERN = /https?:\/\/(?:www\.)?(?:youtube\.com\/watch\?[^\s)\]]*?v=|youtu\.be\/)[\w-]{6,}(?:[^\s)\]]*)?/i;

function normalizeHeading(heading: string) {
  return heading
    .replace(/[\p{Extended_Pictographic}\p{Emoji_Presentation}\uFE0F]/gu, '')
    .replace(/[*_`]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .toLocaleLowerCase('ko-KR');
}

function getSectionKey(heading: string): CinemaSectionKey | null {
  return SECTION_ALIASES[normalizeHeading(heading)] ?? null;
}

function appendSection(
  sections: Partial<Record<CinemaSectionKey, string>>,
  key: CinemaSectionKey,
  content: string,
) {
  if (!content) return;
  sections[key] = sections[key] ? `${sections[key]}\n\n${content}` : content;
}

function removeVideoUrl(content: string, videoUrl: string) {
  return content
    .replace(videoUrl, '')
    .replace(/^\s*[-*]?\s*(?:원본 영상|영상 보기|유튜브에서 보기)\s*:?[ \t]*$/gim, '')
    .trim();
}

export function parseCinemaContent(content: string): ParsedCinemaContent | null {
  const videoUrl = content.match(YOUTUBE_URL_PATTERN)?.[0];
  if (!videoUrl) return null;

  const matches = Array.from(content.matchAll(HEADING_PATTERN));
  const firstRecognizedMatch = matches.find((match) => getSectionKey(match[1]) !== null);
  if (firstRecognizedMatch?.index === undefined) return null;

  const sections: Partial<Record<CinemaSectionKey, string>> = {};
  const additionalSections: AdditionalCinemaSection[] = [];

  matches.forEach((match, index) => {
    if (match.index === undefined) return;

    const contentStart = match.index + match[0].length;
    const contentEnd = matches[index + 1]?.index ?? content.length;
    const sectionContent = removeVideoUrl(content.slice(contentStart, contentEnd), videoUrl);
    const key = getSectionKey(match[1]);

    if (key) {
      appendSection(sections, key, sectionContent);
    } else if (sectionContent) {
      additionalSections.push({ heading: match[1].trim(), content: sectionContent });
    }
  });

  const meaningfulSections = Object.keys(sections).filter((key) => key !== 'source').length;
  if (meaningfulSections < 2) return null;

  return {
    videoUrl,
    preamble: removeVideoUrl(content.slice(0, firstRecognizedMatch.index), videoUrl),
    sections,
    additionalSections,
  };
}
