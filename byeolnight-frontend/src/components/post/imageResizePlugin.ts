import type { LinkMdNode, PluginInfo, PluginNodeViews } from '@toast-ui/editor';
import { getImageWidth } from '../../utils/imageUtils';

export default function createImageResizePlugin(getLabel: () => string) {
  const imageView: PluginNodeViews = (initialNode, view, getPos) => {
    let node = initialNode;
    let drag: { pointerId: number; startX: number; startWidth: number; width: number } | null = null;
    const dom = document.createElement('span');
    const image = document.createElement('img');
    const handle = document.createElement('button');
    dom.className = 'editor-resizable-image';
    dom.contentEditable = 'false';
    handle.type = 'button';
    handle.className = 'editor-image-resize-handle';
    handle.textContent = '↔';
    handle.title = getLabel();
    handle.setAttribute('aria-label', getLabel());
    dom.append(image, handle);

    const showWidth = (width?: number) => {
      dom.style.width = width ? `${width}px` : '';
      if (width) image.width = width;
      else image.removeAttribute('width');
    };
    const render = () => {
      image.src = node.attrs.imageUrl;
      image.alt = node.attrs.altText || '';
      image.draggable = false;
      const title = node.attrs.htmlAttrs?.title;
      const width = getImageWidth(title);
      image.title = width ? '' : title || '';
      showWidth(width);
    };
    const clampWidth = (width: number) => {
      const container = dom.parentElement || view.dom;
      const right = Math.min(container.getBoundingClientRect().right, view.dom.getBoundingClientRect().right);
      const padding = parseFloat(getComputedStyle(container).paddingRight) || 0;
      const max = Math.max(1, Math.min(4096, Math.floor(right - padding - dom.getBoundingClientRect().left)));
      return Math.min(max, Math.max(64, Math.round(width)));
    };
    const saveWidth = (width: number) => {
      const pos = getPos();
      const current = typeof pos === 'number' ? view.state.doc.nodeAt(pos) : null;
      if (current?.type.name !== 'image') return;
      view.dispatch(view.state.tr.setNodeMarkup(pos, undefined, {
        ...current.attrs,
        htmlAttrs: { ...current.attrs.htmlAttrs, title: `width=${width}`, width },
      }));
    };
    const finish = (commit: boolean) => {
      if (!drag) return;
      const { pointerId, width } = drag;
      drag = null;
      dom.classList.remove('is-resizing');
      if (handle.hasPointerCapture(pointerId)) handle.releasePointerCapture(pointerId);
      if (commit) saveWidth(width);
      else render();
    };

    handle.onpointerdown = (event) => {
      if (!event.isPrimary || event.button !== 0) return;
      event.preventDefault();
      handle.focus({ preventScroll: true });
      const width = image.getBoundingClientRect().width;
      drag = { pointerId: event.pointerId, startX: event.clientX, startWidth: width, width: clampWidth(width) };
      handle.setPointerCapture(event.pointerId);
      dom.classList.add('is-resizing');
    };
    handle.onpointermove = (event) => {
      if (!drag || drag.pointerId !== event.pointerId) return;
      drag.width = clampWidth(drag.startWidth + event.clientX - drag.startX);
      showWidth(drag.width);
    };
    handle.onpointerup = (event) => {
      if (drag?.pointerId === event.pointerId) finish(true);
    };
    handle.onpointercancel = handle.onlostpointercapture = () => finish(false);
    handle.onkeydown = (event) => {
      if (event.key === 'Escape') finish(false);
      if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
      event.preventDefault();
      const delta = event.key === 'ArrowLeft' ? -10 : 10;
      saveWidth(clampWidth(image.getBoundingClientRect().width + delta));
    };
    render();

    return {
      dom,
      update(nextNode) {
        if (nextNode.type !== node.type) return false;
        node = nextNode;
        render();
        return true;
      },
      stopEvent: (event) => handle.contains(event.target as Node),
      ignoreMutation: () => true,
      destroy: () => finish(false),
    };
  };

  // 제목 필드는 표준 마크다운에 포함되므로 모드 전환·저장·재편집 후에도 너비가 유지된다.
  return (): PluginInfo => ({
    wysiwygNodeViews: { image: imageView },
    toHTMLRenderers: {
      image(node, context) {
        const token = context.origin!();
        const width = getImageWidth((node as LinkMdNode).title);
        if (width && token && !Array.isArray(token) && token.type === 'openTag') {
          token.attributes = { ...token.attributes, width };
        }
        return token;
      },
    },
    toMarkdownRenderers: {
      image({ node }, context) {
        const result = context.origin!();
        const width = getImageWidth(node.attrs.htmlAttrs?.title);
        if (!width) return result;
        return {
          ...result,
          rawHTML: null,
          attrs: {
            ...result.attrs,
            imageUrl: `${node.attrs.imageUrl.replace(/&amp;/g, '&')} "width=${width}"`,
          },
        };
      },
    },
  });
}
