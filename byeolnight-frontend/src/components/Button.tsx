import { ButtonHTMLAttributes } from 'react';
import clsx from 'clsx';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'danger' | 'ghost';
}

export default function Button({ children, variant = 'primary', className, ...props }: ButtonProps) {
  const base = 'px-4 py-2 rounded-lg text-sm font-semibold transition-shadow duration-200';
  const styles = {
    primary: 'bg-purple-600 hover:bg-purple-700 text-white shadow-glow',
    danger: 'bg-red-500 hover:bg-red-600 text-white shadow-md',
    ghost: 'bg-transparent border border-gray-500 hover:bg-gray-800 text-white',
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
