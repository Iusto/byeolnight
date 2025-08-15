import { ButtonHTMLAttributes } from 'react';
import clsx from 'clsx';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'danger' | 'ghost';
}

export default function Button({ children, variant = 'primary', className, ...props }: ButtonProps) {
  const base = 'mobile-button touch-target px-4 py-3 rounded-lg text-sm font-semibold transition-all duration-200 min-h-[44px] flex items-center justify-center';
  const styles = {
    primary: 'bg-purple-600 active:bg-purple-700 mouse:hover:bg-purple-700 text-white shadow-glow',
    danger: 'bg-red-500 active:bg-red-600 mouse:hover:bg-red-600 text-white shadow-md',
    ghost: 'bg-transparent border border-gray-500 active:bg-gray-800 mouse:hover:bg-gray-800 text-white',
  };

  return (
    <button
      className={clsx(base, styles[variant], className)}
      {...props}
    >
      {children}
    </button>
  );
}
