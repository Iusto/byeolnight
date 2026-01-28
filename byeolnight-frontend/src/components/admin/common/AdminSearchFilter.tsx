interface FilterOption {
  value: string;
  label: string;
  color?: string;
}

interface FilterGroup {
  options: FilterOption[];
  value: string;
  onChange: (value: string) => void;
}

interface AdminSearchFilterProps {
  searchValue: string;
  onSearchChange: (value: string) => void;
  searchPlaceholder?: string;
  filterGroups?: FilterGroup[];
}

const defaultColors: Record<string, string> = {
  ALL: 'bg-blue-600',
  ACTIVE: 'bg-green-600',
  WITHDRAWN: 'bg-red-600',
  REGULAR: 'bg-orange-600',
  SOCIAL: 'bg-cyan-600',
};

export default function AdminSearchFilter({
  searchValue,
  onSearchChange,
  searchPlaceholder = '검색...',
  filterGroups = [],
}: AdminSearchFilterProps) {
  return (
    <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-4 border border-purple-500/20">
      <div className="flex flex-col gap-4">
        {/* 검색 입력 */}
        <div className="flex justify-center">
          <input
            type="text"
            placeholder={searchPlaceholder}
            value={searchValue}
            onChange={(e) => onSearchChange(e.target.value)}
            className="w-80 px-4 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
          />
        </div>

        {/* 필터 버튼 그룹들 */}
        {filterGroups.length > 0 && (
          <div className="flex gap-2 justify-center flex-wrap">
            {filterGroups.map((group, groupIndex) => (
              <div key={groupIndex} className="flex gap-2 items-center">
                {groupIndex > 0 && <div className="w-px h-8 bg-gray-600 mx-2" />}
                {group.options.map((option) => {
                  const isActive = group.value === option.value;
                  const colorClass = option.color || defaultColors[option.value] || 'bg-purple-600';

                  return (
                    <button
                      key={option.value}
                      onClick={() => group.onChange(option.value)}
                      className={`px-4 py-2 rounded text-sm transition ${
                        isActive
                          ? `${colorClass} text-white`
                          : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                      }`}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}