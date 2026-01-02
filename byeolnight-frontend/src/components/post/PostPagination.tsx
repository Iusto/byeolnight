interface PostPaginationProps {
  currentPage: number;
  hasMore: boolean;
  onPageChange: (page: number) => void;
}

export default function PostPagination({ currentPage, hasMore, onPageChange }: PostPaginationProps) {
  return (
    <div className="mt-8 flex justify-center items-center gap-4">
      {currentPage > 0 && (
        <button
          onClick={() => onPageChange(currentPage - 1)}
          className="flex items-center justify-center w-12 h-12 bg-purple-600/80 hover:bg-purple-600 active:bg-purple-700 rounded-full text-white font-medium transition-all duration-200 shadow-lg hover:shadow-purple-500/25 transform hover:scale-105 active:scale-95"
        >
          ←
        </button>
      )}
      <span className="px-6 py-3 bg-slate-800/80 rounded-lg text-white font-medium border border-purple-500/30">
        {currentPage + 1}
      </span>
      {hasMore && (
        <button
          onClick={() => onPageChange(currentPage + 1)}
          className="flex items-center justify-center w-12 h-12 bg-purple-600/80 hover:bg-purple-600 active:bg-purple-700 rounded-full text-white font-medium transition-all duration-200 shadow-lg hover:shadow-purple-500/25 transform hover:scale-105 active:scale-95"
        >
          →
        </button>
      )}
    </div>
  );
}
