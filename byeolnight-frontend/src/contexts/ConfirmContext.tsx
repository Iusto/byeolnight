import { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { createPortal } from 'react-dom';

interface ConfirmOptions {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  type?: 'danger' | 'warning' | 'info';
}

interface ConfirmContextType {
  confirm: (options: ConfirmOptions) => Promise<boolean>;
}

const ConfirmContext = createContext<ConfirmContextType | null>(null);

const typeStyles = {
  danger: {
    icon: '⚠️',
    iconBg: 'bg-red-500/20',
    confirmBtn: 'bg-red-600 hover:bg-red-700',
  },
  warning: {
    icon: '⚡',
    iconBg: 'bg-yellow-500/20',
    confirmBtn: 'bg-yellow-600 hover:bg-yellow-700',
  },
  info: {
    icon: 'ℹ️',
    iconBg: 'bg-blue-500/20',
    confirmBtn: 'bg-blue-600 hover:bg-blue-700',
  },
};

export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<{
    isOpen: boolean;
    options: ConfirmOptions;
    resolve: ((value: boolean) => void) | null;
  }>({
    isOpen: false,
    options: { message: '' },
    resolve: null,
  });

  const confirm = useCallback((options: ConfirmOptions): Promise<boolean> => {
    return new Promise((resolve) => {
      setState({
        isOpen: true,
        options,
        resolve,
      });
    });
  }, []);

  const handleConfirm = useCallback(() => {
    state.resolve?.(true);
    setState((prev) => ({ ...prev, isOpen: false, resolve: null }));
  }, [state.resolve]);

  const handleCancel = useCallback(() => {
    state.resolve?.(false);
    setState((prev) => ({ ...prev, isOpen: false, resolve: null }));
  }, [state.resolve]);

  const { options, isOpen } = state;
  const type = options.type || 'danger';
  const style = typeStyles[type];

  return (
    <ConfirmContext.Provider value={{ confirm }}>
      {children}
      {isOpen &&
        createPortal(
          <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4">
            {/* 배경 오버레이 */}
            <div
              className="absolute inset-0 bg-black/60 backdrop-blur-sm"
              onClick={handleCancel}
            />

            {/* 모달 */}
            <div className="relative bg-[#1f2336] border border-gray-600 rounded-xl shadow-2xl w-full max-w-md animate-scale-in">
              {/* 헤더 */}
              <div className="p-6 pb-4">
                <div className="flex items-start gap-4">
                  <div className={`p-3 rounded-full ${style.iconBg}`}>
                    <span className="text-2xl">{style.icon}</span>
                  </div>
                  <div className="flex-1">
                    {options.title && (
                      <h3 className="text-lg font-semibold text-white mb-1">
                        {options.title}
                      </h3>
                    )}
                    <p className="text-gray-300 text-sm leading-relaxed">
                      {options.message}
                    </p>
                  </div>
                </div>
              </div>

              {/* 버튼 */}
              <div className="flex gap-3 p-4 pt-2 border-t border-gray-700">
                <button
                  onClick={handleCancel}
                  className="flex-1 px-4 py-2.5 bg-gray-600 hover:bg-gray-500 text-white rounded-lg font-medium transition-colors"
                >
                  {options.cancelText || '취소'}
                </button>
                <button
                  onClick={handleConfirm}
                  className={`flex-1 px-4 py-2.5 ${style.confirmBtn} text-white rounded-lg font-medium transition-colors`}
                >
                  {options.confirmText || '확인'}
                </button>
              </div>
            </div>
          </div>,
          document.body
        )}
    </ConfirmContext.Provider>
  );
}

export function useConfirm() {
  const context = useContext(ConfirmContext);
  if (!context) {
    throw new Error('useConfirm must be used within a ConfirmProvider');
  }
  return context.confirm;
}