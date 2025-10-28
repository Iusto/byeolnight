interface LoadingSpinnerProps {
  message?: string;
}

export default function LoadingSpinner({ message = '우주를 탐험하는 중...' }: LoadingSpinnerProps) {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white flex items-center justify-center">
      <div className="text-center">
        <div className="text-6xl mb-4 animate-spin-slow">🌌</div>
        <p className="text-xl text-purple-300">{message}</p>
      </div>
    </div>
  );
}
