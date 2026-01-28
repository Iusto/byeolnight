import { ReactNode } from 'react';

export interface Column<T> {
  key: string;
  header: string;
  render?: (item: T) => ReactNode;
  className?: string;
}

interface AdminTableProps<T> {
  columns: Column<T>[];
  data: T[];
  keyExtractor: (item: T) => string | number;
  emptyMessage?: string;
  loading?: boolean;
}

export default function AdminTable<T>({
  columns,
  data,
  keyExtractor,
  emptyMessage = '데이터가 없습니다.',
  loading = false,
}: AdminTableProps<T>) {
  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-purple-500"></div>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm border border-gray-600 bg-[#1f2336]/80 backdrop-blur rounded-xl overflow-hidden">
        <thead className="bg-[#2a2e45] text-gray-300">
          <tr>
            {columns.map((col) => (
              <th key={col.key} className={`p-3 ${col.className || ''}`}>
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.length > 0 ? (
            data.map((item) => (
              <tr
                key={keyExtractor(item)}
                className="border-t border-gray-700 hover:bg-[#252842]/50"
              >
                {columns.map((col) => (
                  <td key={col.key} className={`p-3 ${col.className || ''}`}>
                    {col.render
                      ? col.render(item)
                      : (item as Record<string, unknown>)[col.key] as ReactNode}
                  </td>
                ))}
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={columns.length} className="p-8 text-center text-gray-400">
                {emptyMessage}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}