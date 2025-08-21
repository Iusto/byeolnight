import { Link } from 'react-router-dom';

const Unauthorized = () => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 flex items-center justify-center px-4">
      <div className="max-w-md w-full bg-white/10 backdrop-blur-md rounded-2xl p-8 text-center border border-white/20">
        <div className="mb-6">
          <div className="text-6xl mb-4">🚫</div>
          <h1 className="text-3xl font-bold text-white mb-2">접근 권한이 없습니다</h1>
          <p className="text-gray-300">
            관리자만 접근 가능한 페이지입니다.
          </p>
        </div>
        
        <div className="space-y-4">
          <Link
            to="/"
            className="block w-full bg-purple-600 hover:bg-purple-700 text-white font-medium py-3 px-6 rounded-lg transition-colors"
          >
            홈으로 돌아가기
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Unauthorized;