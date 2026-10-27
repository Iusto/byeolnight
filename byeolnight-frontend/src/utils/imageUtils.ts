/** 허용되는 이미지 URL 정규식 (jpg, jpeg, png, gif, webp, svg, bmp) */
export const IMAGE_URL_REGEX = /^https?:\/\/.+\.(jpg|jpeg|png|gif|webp|svg|bmp)(\?.*)?$/i;

/** 이미지 URL 유효성 검증 */
export const isValidImageUrl = (url: string): boolean => {
  return IMAGE_URL_REGEX.test(url);
};

/** 기존 너비 전용 제목과 가로·세로 크기 제목을 검증한다. */
export const getImageDimensions = (title?: string | null): { width?: number; height?: number } => {
  const match = title?.match(/^width=(\d{1,4})(?: height=(\d{1,4}))?$/);
  if (!match) return {};
  const width = Number(match[1]);
  const height = match[2] ? Number(match[2]) : undefined;
  if (width < 1 || width > 4096 || (height !== undefined && (height < 1 || height > 4096))) return {};
  return height === undefined ? { width } : { width, height };
};
