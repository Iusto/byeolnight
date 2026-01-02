import { useState } from 'react';
import { useTranslation } from 'react-i18next';

interface PostSearchBarProps {
  searchKeyword: string;
  searchType: string;
  onSearch: (searchType: string, keyword: string) => void;
  onReset: () => void;
}

export default function PostSearchBar({ searchKeyword, searchType, onSearch, onReset }: PostSearchBarProps) {
  const { t } = useTranslation();
  const [searchInput, setSearchInput] = useState(searchKeyword);
  const [searchTypeInput, setSearchTypeInput] = useState(searchType);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchInput.trim()) {
      onSearch(searchTypeInput, searchInput.trim());
    }
  };

  return (
    <div className="mb-4 sm:mb-6">
      <div className="bg-slate-800/50 rounded-xl p-3 sm:p-4 border border-slate-600/30">
        <form onSubmit={handleSubmit}>
          <div className="flex gap-1 w-full overflow-hidden">
            <select
              value={searchTypeInput}
              onChange={(e) => setSearchTypeInput(e.target.value)}
              className="bg-slate-700/50 text-white rounded-lg px-1 py-2 text-xs border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 transition-all duration-200 w-14 sm:w-auto sm:min-w-[80px] flex-shrink-0"
            >
              <option value="title">제목</option>
              <option value="content">내용</option>
              <option value="titleAndContent">전체</option>
              <option value="writer">작성자</option>
            </select>
            
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="검색"
              className="flex-1 min-w-0 bg-slate-700/50 text-white rounded-lg px-2 py-2 text-sm border border-slate-600/50 focus:outline-none focus:ring-2 focus:ring-purple-500 placeholder-gray-400 transition-all duration-200"
            />
            
            <button
              type="submit"
              className="px-2 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-sm transition-all duration-200 flex-shrink-0"
            >
              🔍
            </button>
            
            {(searchKeyword || searchInput) && (
              <button
                type="button"
                onClick={() => {
                  setSearchInput('');
                  onReset();
                }}
                className="px-2 py-2 bg-slate-600 hover:bg-slate-700 text-white rounded-lg text-sm transition-all duration-200 flex-shrink-0"
              >
                ✕
              </button>
            )}
          </div>
        </form>
        
        {searchKeyword && (
          <div className="mt-2 p-2 bg-purple-900/30 rounded-lg border border-purple-600/30">
            <p className="text-xs sm:text-sm text-purple-200 flex flex-wrap items-center gap-1">
              <span className="text-purple-300">{t('home.search_result')}:</span> 
              <span className="font-semibold text-white bg-purple-800/30 px-1.5 py-0.5 rounded text-xs">"{searchKeyword}"</span> 
              <span className="text-purple-300 text-xs bg-purple-800/20 px-1.5 py-0.5 rounded-full">
                {searchType === 'titleAndContent' ? '전체' : 
                 searchType === 'title' ? '제목' : 
                 searchType === 'content' ? '내용' : '작성자'}
              </span>
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
