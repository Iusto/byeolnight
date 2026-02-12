/** 허용되는 이미지 URL 정규식 (jpg, jpeg, png, gif, webp, svg, bmp) */
export const IMAGE_URL_REGEX = /^https?:\/\/.+\.(jpg|jpeg|png|gif|webp|svg|bmp)(\?.*)?$/i;

/** 이미지 URL 유효성 검증 */
export const isValidImageUrl = (url: string): boolean => {
  return IMAGE_URL_REGEX.test(url);
};
