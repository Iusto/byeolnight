/** 에디터 훅과 별도 업로더가 중복 업로드하지 않도록 공유하는 단일 상태다. */
export const isHandlingImageUpload = { current: false };
