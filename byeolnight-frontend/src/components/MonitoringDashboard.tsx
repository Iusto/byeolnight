import { useEffect, useState } from 'react';
import axios from '../lib/axios';

interface MonitoringStats {
  totalRequests: number;
  httpRequests: number;
  wsRequests: number;
  banStatusRequests: number;
  memory: {
    used: number;
    max: number;
    usagePercent: number;
  };
  timestamp: number;
}

export default function MonitoringDashboard() {
  const [stats, setStats] = useState<MonitoringStats | null>(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    if (!isVisible) return;

    const fetchStats = async () => {
      try {
        const response = await axios.get('/monitoring/stats');
        setStats(response.data);
      } catch (error) {
        console.error('모니터링 데이터 로드 실패:', error);
      }
    };

    fetchStats();
    const interval = setInterval(fetchStats, 2000); // 2초마다 업데이트

    return () => clearInterval(interval);
  }, [isVisible]);

  if (!isVisible) {
    return (
      <button
        onClick={() => setIsVisible(true)}
        className="fixed bottom-4 right-4 bg-purple-600 text-white px-3 py-2 rounded text-sm z-50"
      >
        📊 모니터링
      </button>
    );
  }

  return (
    <div className="fixed bottom-4 right-4 bg-black/90 text-white p-4 rounded-lg text-sm z-50 min-w-[300px]">
      <div className="flex justify-between items-center mb-3">
        <h3 className="font-bold">📊 실시간 모니터링</h3>
        <button
          onClick={() => setIsVisible(false)}
          className="text-gray-400 hover:text-white"
        >
          ✕
        </button>
      </div>
      
      {stats ? (
        <div className="space-y-2">
          <div>
            <div className="text-green-400">총 요청: {stats.totalRequests.toLocaleString()}</div>
            <div className="text-blue-400">HTTP: {stats.httpRequests.toLocaleString()}</div>
            <div className="text-yellow-400">WebSocket: {stats.wsRequests.toLocaleString()}</div>
            <div className="text-red-400">밴상태: {stats.banStatusRequests.toLocaleString()}</div>
          </div>
          
          <div className="border-t border-gray-600 pt-2">
            <div className="text-blue-400">메모리 사용량</div>
            <div className="text-xs">
              {stats.memory.used}MB / {stats.memory.max}MB 
              ({stats.memory.usagePercent.toFixed(1)}%)
            </div>
            <div className="w-full bg-gray-700 rounded-full h-2 mt-1">
              <div 
                className="bg-blue-500 h-2 rounded-full transition-all"
                style={{ width: `${Math.min(stats.memory.usagePercent, 100)}%` }}
              />
            </div>
          </div>
          
          <div className="text-xs text-gray-400">
            마지막 업데이트: {new Date(stats.timestamp).toLocaleTimeString()}
          </div>
        </div>
      ) : (
        <div>로딩 중...</div>
      )}
    </div>
  );
}