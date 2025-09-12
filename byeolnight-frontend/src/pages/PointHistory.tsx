import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation();
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
        alert(t('points.attendance_success'));
        
        // 상태 업데이트
        setTodayAttended(true);
        
        // 데이터 새로고침 (순차적으로 실행)
        await refreshUserInfo(); // 사용자 정보 (포인트) 업데이트
        await fetchPointHistory(); // 포인트 히스토리 업데이트
        
        console.log('출석 체크 완료 및 데이터 새로고침 완료');
      } else {
        alert(t('points.already_attended'));
      }
    } catch (err) {
      console.error('출석 체크 실패:', err);
      alert(t('points.attendance_failed'));
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
          <p className="text-lg mb-4">{t('points.login_required')}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-4xl mx-auto">
        {/* 헤더 */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold mb-2">✨ {t('points.title')}</h1>
          <p className="text-gray-400">{t('points.subtitle')}</p>
        </div>

        {/* 현재 포인트 & 출석 체크 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 mb-8 shadow-xl">
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-xl font-semibold mb-2">{t('points.owned_stella')}</h2>
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
                {todayAttended ? t('points.attendance_completed') : t('points.attendance_check')}
              </button>
            </div>
          </div>
        </div>

        {/* 포인트 획득 방법 안내 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-4 sm:p-6 mb-8 shadow-xl">
          <h2 className="text-lg sm:text-xl font-bold mb-3 sm:mb-4 text-center bg-gradient-to-r from-yellow-400 to-orange-400 bg-clip-text text-transparent">
            ⭐ {t('shop.how_to_earn_points')}
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 sm:gap-4">
            <div className="bg-[#252842] bg-opacity-60 rounded-lg p-3">
              <div className="flex items-center gap-2 mb-2">
                <span className="text-lg">📅</span>
                <span className="font-semibold text-sm sm:text-base">{t('shop.daily_attendance')}</span>
              </div>
              <p className="text-yellow-400 font-bold text-xs sm:text-sm">+10 {t('shop.points')}</p>
              <p className="text-gray-400 text-xs">{t('shop.daily_attendance_desc')}</p>
            </div>
            
            <div className="bg-[#252842] bg-opacity-60 rounded-lg p-3">
              <div className="flex items-center gap-2 mb-2">
                <span className="text-lg">✍️</span>
                <span className="font-semibold text-sm sm:text-base">{t('shop.post_write')}</span>
              </div>
              <p className="text-yellow-400 font-bold text-xs sm:text-sm">+20 {t('shop.points')}</p>
              <p className="text-gray-400 text-xs">{t('shop.post_write_desc')}</p>
            </div>
            
            <div className="bg-[#252842] bg-opacity-60 rounded-lg p-3">
              <div className="flex items-center gap-2 mb-2">
                <span className="text-lg">💬</span>
                <span className="font-semibold text-sm sm:text-base">{t('shop.comment_write')}</span>
              </div>
              <p className="text-yellow-400 font-bold text-xs sm:text-sm">+5 {t('shop.points')}</p>
              <p className="text-gray-400 text-xs">{t('shop.comment_write_desc')}</p>
            </div>
            
            <div className="bg-[#252842] bg-opacity-60 rounded-lg p-3">
              <div className="flex items-center gap-2 mb-2">
                <span className="text-lg">❤️</span>
                <span className="font-semibold text-sm sm:text-base">{t('shop.receive_like')}</span>
              </div>
              <p className="text-yellow-400 font-bold text-xs sm:text-sm">+2 {t('shop.points')}</p>
              <p className="text-gray-400 text-xs">{t('shop.receive_like_desc')}</p>
            </div>
            
            <div className="bg-[#252842] bg-opacity-60 rounded-lg p-3">
              <div className="flex items-center gap-2 mb-2">
                <span className="text-lg">👍</span>
                <span className="font-semibold text-sm sm:text-base">{t('shop.give_like')}</span>
              </div>
              <p className="text-yellow-400 font-bold text-xs sm:text-sm">+1 {t('shop.points')}</p>
              <p className="text-gray-400 text-xs">{t('shop.give_like_desc')}</p>
            </div>
            
            <div className="bg-[#252842] bg-opacity-60 rounded-lg p-3">
              <div className="flex items-center gap-2 mb-2">
                <span className="text-lg">🚨</span>
                <span className="font-semibold text-sm sm:text-base">{t('shop.valid_report')}</span>
              </div>
              <p className="text-yellow-400 font-bold text-xs sm:text-sm">+10 {t('shop.points')}</p>
              <p className="text-gray-400 text-xs">{t('shop.valid_report_desc')}</p>
            </div>
          </div>
          
          <div className="mt-4 p-3 bg-blue-900 bg-opacity-30 rounded-lg border border-blue-500 border-opacity-30">
            <p className="text-blue-300 text-xs sm:text-sm text-center">
              💡 {t('shop.daily_limits_notice')}
            </p>
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
            {t('points.tabs.all')}
          </button>
          <button
            onClick={() => setActiveTab('earned')}
            className={`px-4 py-2 rounded-lg font-medium transition-all duration-200 ${
              activeTab === 'earned'
                ? 'bg-green-600 text-white'
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            {t('points.tabs.earned')}
          </button>
          <button
            onClick={() => setActiveTab('spent')}
            className={`px-4 py-2 rounded-lg font-medium transition-all duration-200 ${
              activeTab === 'spent'
                ? 'bg-red-600 text-white'
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            {t('points.tabs.spent')}
          </button>
        </div>

        {/* 포인트 히스토리 */}
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl shadow-xl overflow-hidden">
          {loading ? (
            <div className="p-8 text-center text-gray-400">{t('points.loading')}</div>
          ) : histories.length === 0 ? (
            <div className="p-8 text-center text-gray-400">
              {activeTab === 'all' && t('points.no_history')}
              {activeTab === 'earned' && t('points.no_earned')}
              {activeTab === 'spent' && t('points.no_spent')}
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
                          {t('points.earned_badge')}
                        </span>
                      ) : (
                        <span className="bg-red-600/20 text-red-300 px-2 py-1 rounded text-xs">
                          {t('points.spent_badge')}
                        </span>
                      )
                    )}
                    {activeTab === 'spent' && (
                      <span className="bg-red-600/20 text-red-300 px-2 py-1 rounded text-xs">
                        {t('points.spent_badge')}
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