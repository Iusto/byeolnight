/** 허용되는 이미지 URL 정규식 (jpg, jpeg, png, gif, webp, svg, bmp) */
export const IMAGE_URL_REGEX = /^https?:\/\/.+\.(jpg|jpeg|png|gif|webp|svg|bmp)(\?.*)?$/i;

/** 이미지 URL 유효성 검증 */
export const isValidImageUrl = (url: string): boolean => {
  return IMAGE_URL_REGEX.test(url);
};

/** 마크다운 이미지 제목에 저장한 표시 너비만 허용한다. */
export const getImageWidth = (title?: string | null): number | undefined => {
  const match = title?.match(/^width=(\d{1,4})$/);
  const width = match ? Number(match[1]) : 0;
  return width > 0 && width <= 4096 ? width : undefined;
};
