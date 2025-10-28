import React from 'react';

interface AgreementModalProps {
  title: string;
  onClose: () => void;
  onAgree: () => void;
  children: React.ReactNode;
}

export default function AgreementModal({ title, onClose, onAgree, children }: AgreementModalProps) {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center">
      <div className="bg-[#1f2336] text-white p-6 rounded-lg w-full max-w-lg shadow-xl">
        <h2 className="text-xl font-bold mb-4">{title}</h2>
        <div className="h-64 overflow-y-scroll text-sm whitespace-pre-wrap mb-4">
          {children}
        </div>
        <div className="flex justify-end space-x-2">
          <button
            className="px-4 py-2 bg-gray-600 rounded hover:bg-gray-700"
            onClick={onClose}
          >
            닫기
          </button>
          <button
            className="px-4 py-2 bg-blue-500 hover:bg-blue-600 rounded"
            onClick={() => {
              onAgree();
              onClose();
            }}
          >
            동의합니다
          </button>
        </div>
      </div>
    </div>
  );
}
