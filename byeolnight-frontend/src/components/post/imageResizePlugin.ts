import type { LinkMdNode, PluginInfo, PluginNodeViews } from '@toast-ui/editor';
import { getImageDimensions } from '../../utils/imageUtils';

export default function createImageResizePlugin(getLabel: () => string) {
  const imageView: PluginNodeViews = (initialNode, view, getPos) => {
    let node = initialNode;
    let drag: {
      pointerId: number; startX: number; startY: number;
      startWidth: number; startHeight: number; width: number; height: number;
    } | null = null;
    const dom = document.createElement('span');
    const image = document.createElement('img');
    const handle = document.createElement('button');
    dom.className = 'editor-resizable-image';
    dom.contentEditable = 'false';
    handle.type = 'button';
    handle.className = 'editor-image-resize-handle';
    handle.textContent = '↘';
    handle.title = getLabel();
    handle.setAttribute('aria-label', getLabel());
    dom.append(image, handle);

    const showSize = (width?: number, height?: number) => {
      dom.style.width = width ? `${width}px` : '';
      if (width) image.width = width;
      else image.removeAttribute('width');
      if (height) image.height = height;
      else image.removeAttribute('height');
      // 원본 비율 대신 사용자가 정한 비율을 사용하고, 좁은 화면에서는 함께 축소한다.
      image.style.aspectRatio = width && height ? `${width} / ${height}` : '';
    };
    const render = () => {
      image.src = node.attrs.imageUrl;
      image.alt = node.attrs.altText || '';
      image.draggable = false;
      const title = node.attrs.htmlAttrs?.title;
      const { width, height } = getImageDimensions(title);
      image.title = width ? '' : title || '';
      showSize(width, height);
    };
    const clampWidth = (width: number) => {
      const container = dom.parentElement || view.dom;
      const right = Math.min(container.getBoundingClientRect().right, view.dom.getBoundingClientRect().right);
      const padding = parseFloat(getComputedStyle(container).paddingRight) || 0;
      const max = Math.max(1, Math.min(4096, Math.floor(right - padding - dom.getBoundingClientRect().left)));
      return Math.min(max, Math.max(64, Math.round(width)));
    };
    const clampHeight = (height: number) => Math.min(4096, Math.max(64, Math.round(height)));
    const saveSize = (width: number, height: number) => {
      const pos = getPos();
      const current = typeof pos === 'number' ? view.state.doc.nodeAt(pos) : null;
      if (current?.type.name !== 'image') return;
      view.dispatch(view.state.tr.setNodeMarkup(pos, undefined, {
        ...current.attrs,
        htmlAttrs: { ...current.attrs.htmlAttrs, title: `width=${width} height=${height}`, width, height },
      }));
    };
    const finish = (commit: boolean) => {
      if (!drag) return;
      const { pointerId, width, height } = drag;
      drag = null;
      dom.classList.remove('is-resizing');
      if (handle.hasPointerCapture(pointerId)) handle.releasePointerCapture(pointerId);
      if (commit) saveSize(width, height);
      else render();
    };

    handle.onpointerdown = (event) => {
      if (!event.isPrimary || event.button !== 0) return;
      event.preventDefault();
      handle.focus({ preventScroll: true });
      const { width, height } = image.getBoundingClientRect();
      drag = {
        pointerId: event.pointerId, startX: event.clientX, startY: event.clientY,
        startWidth: width, startHeight: height, width: clampWidth(width), height: clampHeight(height),
      };
      handle.setPointerCapture(event.pointerId);
      dom.classList.add('is-resizing');
    };
    handle.onpointermove = (event) => {
      if (!drag || drag.pointerId !== event.pointerId) return;
      drag.width = clampWidth(drag.startWidth + event.clientX - drag.startX);
      drag.height = clampHeight(drag.startHeight + event.clientY - drag.startY);
      showSize(drag.width, drag.height);
    };
    handle.onpointerup = (event) => {
      if (drag?.pointerId === event.pointerId) finish(true);
    };
    handle.onpointercancel = handle.onlostpointercapture = () => finish(false);
    handle.onkeydown = (event) => {
      if (event.key === 'Escape') finish(false);
      if (!['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(event.key)) return;
      event.preventDefault();
      const { width, height } = image.getBoundingClientRect();
      const deltaX = event.key === 'ArrowLeft' ? -10 : event.key === 'ArrowRight' ? 10 : 0;
      const deltaY = event.key === 'ArrowUp' ? -10 : event.key === 'ArrowDown' ? 10 : 0;
      saveSize(clampWidth(width + deltaX), clampHeight(height + deltaY));
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

  // 제목 필드는 표준 마크다운에 포함되므로 모드 전환·저장·재편집 후에도 크기가 유지된다.
  return (): PluginInfo => ({
    wysiwygNodeViews: { image: imageView },
    toHTMLRenderers: {
      image(node, context) {
        const token = context.origin!();
        const { width, height } = getImageDimensions((node as LinkMdNode).title);
        if (width && token && !Array.isArray(token) && token.type === 'openTag') {
          token.attributes = {
            ...token.attributes, width,
            ...(height ? { height, style: `aspect-ratio: ${width} / ${height}` } : {}),
          };
        }
        return token;
      },
    },
    toMarkdownRenderers: {
      image({ node }, context) {
        const result = context.origin!();
        const title = node.attrs.htmlAttrs?.title;
        if (!title) return result;
        const { width, height } = getImageDimensions(title);
        const savedTitle = width ? `width=${width}${height ? ` height=${height}` : ''}` : title;
        return {
          ...result,
          rawHTML: null,
          attrs: {
            ...result.attrs,
            imageUrl: `${node.attrs.imageUrl.replace(/&amp;/g, '&')} "${savedTitle.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`,
          },
        };
      },
    },
  });
}
