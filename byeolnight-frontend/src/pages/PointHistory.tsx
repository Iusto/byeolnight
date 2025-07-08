import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import axios from '../lib/axios';

interface PointHistory {
  id: number;
  amount: number;
  type: string;
  typeDescription: string;
  reason: string;
  referenceId?: string;
  createdAt: string;
  isEarned: boolean;
}

export default function PointHistory() {
  const { user, refreshUserInfo } = useAuth();
  const [activeTab, setActiveTab] = useState<'all' | 'earned' | 'spent'>('all');
  const [histories, setHistories] = useState<PointHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [todayAttended, setTodayAttended] = useState(false);

  useEffect(() => {
    if (user) {
      fetchPointHistory();
      checkTodayAttendance();
    }
  }, [user, activeTab]);
  
  // 사용자 정보가 변경될 때마다 출석 상태 재확인
  useEffect(() => {
    if (user) {
      checkTodayAttendance();
    }
  }, [user?.points]); // 포인트가 변경되면 출석 상태도 재확인

  const fetchPointHistory = async () => {
    try {
      setLoading(true);
      let endpoint = '/member/points/history';
      if (activeTab === 'earned') endpoint += '/earned';
      if (activeTab === 'spent') endpoint += '/spent';

      console.log(`포인트 히스토리 API 호출: ${endpoint}`);
      const res = await axios.get(endpoint, { params: { size: 50 } });
      console.log('포인트 히스토리 API 응답:', res.data);
      
      // API 응답 구조 확인 및 데이터 추출
      let historyData = [];
      if (res.data?.data?.content) {
        // CommonResponse 구조: {success: true, data: {content: [...], ...}}
        historyData = res.data.data.content;
      } else if (res.data?.content) {
        // 직접 Page 구조: {content: [...], ...}
        historyData = res.data.content;
      } else if (Array.isArray(res.data?.data)) {
        // List 구조: {success: true, data: [...]}
        historyData = res.data.data;
      } else if (Array.isArray(res.data)) {
        // 직접 Array: [...]
        historyData = res.data;
      }
      
      console.log(`최종 히스토리 개수: ${historyData.length}`);
      console.log('최종 히스토리 데이터:', historyData);
      setHistories(historyData);
    } catch (err) {
      console.error('포인트 히스토리 조회 실패:', err);
    } finally {
      setLoading(false);
    }
  };

  const checkTodayAttendance = async () => {
    try {
      const res = await axios.get('/member/points/attendance/today');
      const attended = res.data?.data === true;
      console.log('출석 여부 API 응답:', res.data);
      console.log('출석 여부 확인 결과:', attended);
      setTodayAttended(attended);
    } catch (err) {
      console.error('출석 확인 실패:', err);
      setTodayAttended(false);
    }
  };

  const handleAttendance = async () => {
    try {
      const res = await axios.post('/member/points/attendance');
      
      // CommonResponse 구조에서 data 필드 확인
      const isAttendanceSuccess = res.data?.data === true;
      console.log('출석 체크 API 응답:', res.data);
      console.log('출석 체크 결과:', isAttendanceSuccess);
      
      if (isAttendanceSuccess) {
        alert('출석 체크 완료! 10 스텔라를 획득했습니다.');
        
        // 상태 업데이트
        setTodayAttended(true);
        
        // 데이터 새로고침 (순차적으로 실행)
        await refreshUserInfo(); // 사용자 정보 (포인트) 업데이트
        await fetchPointHistory(); // 포인트 히스토리 업데이트
        
        console.log('출석 체크 완료 및 데이터 새로고침 완료');
      } else {
        alert('이미 오늘 출석하셨습니다.');
      }
    } catch (err) {
      console.error('출석 체크 실패:', err);
      alert('출석 체크에 실패했습니다.');
    }
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleString('ko-KR');
  };

  const getAmountColor = (amount: number) => {
    return amount > 0 ? 'text-green-400' : 'text-red-400';
  };

  const getAmountPrefix = (amount: number) => {
    return amount > 0 ? '+' : '';
  };

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white flex items-center justify-center">
        <div className="text-center">
          <p className="text-lg mb-4">로그인이 필요합니다.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-4xl mx-auto">
        {/* 헤더 */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold mb-2">✨ 스텔라 포인트</h1>
          <p className="text-gray-400">커뮤니티 활동으로 스텔라를 모아보세요!</p>
        </div>

        {/* 현재 포인트 & 출석 체크 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 mb-8 shadow-xl">
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-xl font-semibold mb-2">보유 스텔라</h2>
              <div className="flex items-center gap-2">
                <span className="text-3xl font-bold text-yellow-400">{user.points?.toLocaleString() || 0}</span>
                <span className="text-yellow-400">✨</span>
              </div>
            </div>
            <div className="text-center">
              <button
                onClick={handleAttendance}
                disabled={todayAttended}
                className={`px-6 py-3 rounded-lg font-medium transition-all duration-200 ${
                  todayAttended
                    ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                    : 'bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white shadow-lg hover:shadow-xl transform hover:scale-105'
                }`}
              >
                {todayAttended ? '✅ 출석 완료' : '📅 출석 체크 (+10)'}
              </button>
            </div>
          </div>
        </div>

        {/* 포인트 획득 방법 안내 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 mb-8 shadow-xl">
          <h3 className="text-lg font-semibold mb-4">💡 스텔라 획득 방법</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div className="flex justify-between">
              <span>📅 매일 출석</span>
              <span className="text-green-400">+10</span>
            </div>
            <div className="flex justify-between">
              <span>📝 게시글 작성</span>
              <span className="text-green-400">+20</span>
            </div>
            <div className="flex justify-between">
              <span>💬 댓글 작성</span>
              <span className="text-green-400">+5</span>
            </div>
            <div className="flex justify-between">
              <span>❤️ 추천 받기 (1개당)</span>
              <span className="text-green-400">+2</span>
            </div>
            <div className="flex justify-between">
              <span>🚨 유효한 신고</span>
              <span className="text-green-400">+10</span>
            </div>
            <div className="flex justify-between">
              <span>⚠️ 규정 위반 페널티</span>
              <span className="text-red-400">-10</span>
            </div>
          </div>
        </div>

        {/* 탭 메뉴 */}
        <div className="flex gap-2 mb-6">
          <button
            onClick={() => setActiveTab('all')}
            className={`px-4 py-2 rounded-lg font-medium transition-all duration-200 ${
              activeTab === 'all'
                ? 'bg-purple-600 text-white'
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            전체 내역
          </button>
          <button
            onClick={() => setActiveTab('earned')}
            className={`px-4 py-2 rounded-lg font-medium transition-all duration-200 ${
              activeTab === 'earned'
                ? 'bg-green-600 text-white'
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            획득 내역
          </button>
          <button
            onClick={() => setActiveTab('spent')}
            className={`px-4 py-2 rounded-lg font-medium transition-all duration-200 ${
              activeTab === 'spent'
                ? 'bg-red-600 text-white'
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            사용 내역
          </button>
        </div>

        {/* 포인트 히스토리 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl shadow-xl overflow-hidden">
          {loading ? (
            <div className="p-8 text-center text-gray-400">로딩 중...</div>
          ) : histories.length === 0 ? (
            <div className="p-8 text-center text-gray-400">
              {activeTab === 'all' && '포인트 내역이 없습니다.'}
              {activeTab === 'earned' && '획득한 포인트가 없습니다.'}
              {activeTab === 'spent' && '사용한 포인트가 없습니다.'}
            </div>
          ) : (
            <div className="divide-y divide-gray-600">
              {histories.map((history) => (
                <div key={history.id} className="p-4 hover:bg-[#252842]/50 transition-colors">
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <span className="font-medium">{history.typeDescription}</span>
                        <span className={`font-bold ${getAmountColor(history.amount)}`}>
                          {getAmountPrefix(history.amount)}{history.amount}
                        </span>
                      </div>
                      <p className="text-sm text-gray-400 mb-1">{history.reason}</p>
                      <p className="text-xs text-gray-500">{formatDate(history.createdAt)}</p>
                    </div>
                    {activeTab === 'all' && (
                      history.amount > 0 ? (
                        <span className="bg-green-600/20 text-green-300 px-2 py-1 rounded text-xs">
                          획득
                        </span>
                      ) : (
                        <span className="bg-red-600/20 text-red-300 px-2 py-1 rounded text-xs">
                          사용
                        </span>
                      )
                    )}
                    {activeTab === 'spent' && (
                      <span className="bg-red-600/20 text-red-300 px-2 py-1 rounded text-xs">
                        사용
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}