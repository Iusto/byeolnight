// utils/updateTuiPlaceholder.ts

/**
 * TUI Editor의 placeholder 텍스트를 동적으로 변경하는 함수
 *
 * @param instance - Toast UI Editor 인스턴스
 * @param placeholderText - 새로 적용할 placeholder 텍스트
 */
export function updateTuiPlaceholder(instance: any, placeholderText: string) {
  try {
    const editorRoot = instance?.root;
    if (!editorRoot) return;

    const placeholderEl = editorRoot.querySelector('.toastui-editor-ww-container .toastui-editor-placeholder');
    if (placeholderEl) {
      placeholderEl.textContent = placeholderText;
    }
  } catch (error) {
    console.error('TUI placeholder 업데이트 실패:', error);
  }
}
