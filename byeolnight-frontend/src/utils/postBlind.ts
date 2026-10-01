export function getPostBlindLabel(blindType?: string, compact = false) {
  switch (blindType) {
    case 'ADMIN_BLIND':
      return compact ? '관리자' : '관리자 블라인드';
    case 'REPORT_BLIND':
      return compact ? '신고' : '신고 블라인드';
    case 'PENDING_REVIEW':
      return compact ? '검수 대기' : '자동 콘텐츠 검수 대기';
    default:
      return '블라인드';
  }
}

export function getBlindedPostTitle(blindType?: string) {
  switch (blindType) {
    case 'ADMIN_BLIND':
      return '관리자 블라인드 처리됨';
    case 'REPORT_BLIND':
      return '신고로 블라인드 처리됨';
    case 'PENDING_REVIEW':
      return '자동 콘텐츠 검수 대기';
    default:
      return '블라인드 처리됨';
  }
}

export function getPostBlindBadgeClass(blindType?: string, solid = false) {
  if (blindType === 'ADMIN_BLIND') {
    return solid
      ? 'bg-red-600/90 text-red-100 border border-red-400/50'
      : 'bg-red-600/20 text-red-400 border border-red-500/30';
  }
  if (blindType === 'PENDING_REVIEW') {
    return solid
      ? 'bg-blue-600/90 text-blue-100 border border-blue-400/50'
      : 'bg-blue-600/20 text-blue-300 border border-blue-500/30';
  }
  return solid
    ? 'bg-yellow-600/90 text-yellow-100 border border-yellow-400/50'
    : 'bg-yellow-600/20 text-yellow-400 border border-yellow-500/30';
}
