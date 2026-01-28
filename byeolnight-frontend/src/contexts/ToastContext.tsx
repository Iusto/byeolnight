import { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { createPortal } from 'react-dom';

type ToastType = 'success' | 'error' | 'warning' | 'info';

interface Toast {
  id: string;
  message: string;
  type: ToastType;
}

interface ToastContextType {
  showToast: (message: string, type?: ToastType) => void;
  success: (message: string) => void;
  error: (message: string) => void;
  warning: (message: string) => void;
  info: (message: string) => void;
}

const ToastContext = createContext<ToastContextType | null>(null);

const TOAST_DURATION = 3000;

const toastStyles: Record<ToastType, { bg: string; icon: string; border: string }> = {
  success: {
    bg: 'bg-green-600/90',
    icon: '✓',
    border: 'border-green-400',
  },
  error: {
    bg: 'bg-red-600/90',
    icon: '✕',
    border: 'border-red-400',
  },
  warning: {
    bg: 'bg-yellow-600/90',
    icon: '⚠',
    border: 'border-yellow-400',
  },
  info: {
    bg: 'bg-blue-600/90',
    icon: 'ℹ',
    border: 'border-blue-400',
  },
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback((message: string, type: ToastType = 'info') => {
    const id = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    setToasts((prev) => [...prev, { id, message, type }]);

    setTimeout(() => {
      removeToast(id);
    }, TOAST_DURATION);
  }, [removeToast]);

  const success = useCallback((message: string) => showToast(message, 'success'), [showToast]);
  const error = useCallback((message: string) => showToast(message, 'error'), [showToast]);
  const warning = useCallback((message: string) => showToast(message, 'warning'), [showToast]);
  const info = useCallback((message: string) => showToast(message, 'info'), [showToast]);

  return (
    <ToastContext.Provider value={{ showToast, success, error, warning, info }}>
      {children}
      {createPortal(
        <div className="fixed top-4 right-4 z-[9999] flex flex-col gap-2 pointer-events-none">
          {toasts.map((toast) => {
            const style = toastStyles[toast.type];
            return (
              <div
                key={toast.id}
                className={`
                  ${style.bg} ${style.border}
                  border backdrop-blur-sm rounded-lg px-4 py-3
                  text-white shadow-lg min-w-[280px] max-w-[400px]
                  flex items-center gap-3
                  animate-slide-in pointer-events-auto
                  transition-all duration-300
                `}
              >
                <span className="text-lg font-bold">{style.icon}</span>
                <span className="text-sm flex-1">{toast.message}</span>
                <button
                  onClick={() => removeToast(toast.id)}
                  className="text-white/70 hover:text-white transition-colors"
                >
                  ✕
                </button>
              </div>
            );
          })}
        </div>,
        document.body
      )}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
}