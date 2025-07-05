import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import StellaIcon from '../components/StellaIcon';
import type { StellaIcon as StellaIconType, UserIcon } from '../types/stellaIcon';
import { stellaIcons } from '../data/stellaIcons';
import axios from '../lib/axios';

export default function StellaShop() {
  const { user, refreshUserInfo } = useAuth();
  const [icons, setIcons] = useState<StellaIconType[]>([]);
  const [ownedIcons, setOwnedIcons] = useState<UserIcon[]>([]);
  const [loading, setLoading] = useState(true);
  const [purchasing, setPurchasing] = useState<number | null>(null);
  const [selectedGrade, setSelectedGrade] = useState<string>('ALL');

  const grades = ['ALL', 'COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'MYTHIC'];

  useEffect(() => {
    if (user) {
      fetchIcons();
      fetchOwnedIcons();
    }
  }, [user]);

  const fetchIcons = async () => {
    try {
      const response = await axios.get('/shop/icons');
      if (response.data.success) {
        setIcons(response.data.data);
      } else {
        console.error('아이콘 목록 조회 실패:', response.data.message);
        setIcons(stellaIcons);
      }
    } catch (err) {
      console.error('아이콘 목록 조회 실패:', err);
      setIcons(stellaIcons);
    }
  };

  const fetchOwnedIcons = async () => {
    try {
      const response = await axios.get('/shop/my-icons');
      if (response.data.success) {
        setOwnedIcons(response.data.data);
      }
    } catch (err) {
      console.error('보유 아이콘 조회 실패:', err);
    } finally {
      setLoading(false);
    }
  };

  const handlePurchase = async (iconId: number, price: number) => {
    if (!user) return;
    
    if (user.points < price) {
      alert('스텔라 포인트가 부족합니다!');
      return;
    }

    setPurchasing(iconId);
    try {
      const response = await axios.post(`/shop/purchase/${iconId}`);
      if (response.data.success) {
        alert('아이콘을 성공적으로 구매했습니다!');
        await fetchOwnedIcons();
        await refreshUserInfo();
      } else {
        alert(response.data.message || '구매에 실패했습니다.');
      }
    } catch (err: any) {
      alert(err.response?.data?.message || '구매에 실패했습니다.');
    } finally {
      setPurchasing(null);
    }
  };

  const isOwned = (iconId: number) => {
    return ownedIcons.some(owned => owned.iconId === iconId);
  };

  const handleEquip = async (iconId: number) => {
    try {
      const response = await axios.post(`/shop/equip/${iconId}`);
      if (response.data.success) {
        alert('아이콘을 장착했습니다!');
        await refreshUserInfo();
      } else {
        alert(response.data.message || '장착에 실패했습니다.');
      }
    } catch (err: any) {
      alert(err.response?.data?.message || '장착에 실패했습니다.');
    }
  };

  const filteredIcons = selectedGrade === 'ALL' 
    ? icons 
    : icons.filter(icon => icon.grade === selectedGrade);

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white flex items-center justify-center">
        <div className="text-center">
          <p className="text-lg mb-4">스텔라 상점은 로그인이 필요합니다.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-6xl mx-auto">
        {/* 헤더 */}
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold mb-4 bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
            ⭐ 스텔라 아이콘 상점
          </h1>
          <p className="text-gray-400 mb-4">특별한 아이콘으로 당신의 개성을 표현해보세요</p>
          <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-4 inline-block">
            <p className="text-yellow-400 font-bold text-lg">
              보유 스텔라: ⭐ {user.points?.toLocaleString() || 0}
            </p>
          </div>
        </div>

        {/* 등급 필터 */}
        <div className="flex flex-wrap justify-center gap-3 mb-8">
          {grades.map(grade => {
            const gradeInfo = {
              ALL: { name: '전체', icon: '🌌', color: 'bg-gradient-to-r from-gray-600 to-gray-700' },
              COMMON: { name: '커몬', icon: '⭐', color: 'bg-gradient-to-r from-slate-600 to-gray-600' },
              RARE: { name: '레어', icon: '✨', color: 'bg-gradient-to-r from-cyan-600 to-blue-600' },
              EPIC: { name: '에픽', icon: '🔮', color: 'bg-gradient-to-r from-purple-600 to-violet-600' },
              LEGENDARY: { name: '레전드', icon: '🌟', color: 'bg-gradient-to-r from-yellow-600 to-orange-600' },
              MYTHIC: { name: '미스틱', icon: '🌌', color: 'bg-gradient-to-r from-pink-600 to-purple-600' }
            };
            const info = gradeInfo[grade as keyof typeof gradeInfo];
            return (
              <button
                key={grade}
                onClick={() => setSelectedGrade(grade)}
                className={`px-6 py-3 rounded-xl font-bold transition-all duration-300 transform hover:scale-105 shadow-lg ${
                  selectedGrade === grade
                    ? `${info.color} text-white shadow-xl ring-2 ring-white/50`
                    : 'bg-gray-700/50 text-gray-300 hover:bg-gray-600/70 backdrop-blur-sm'
                }`}
              >
                <span className="mr-2">{info.icon}</span>
                {info.name}
              </button>
            );
          })}
        </div>

        {/* 아이콘 그리드 */}
        {loading ? (
          <div className="text-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-400 mx-auto"></div>
            <p className="mt-4 text-gray-400">아이콘을 불러오는 중...</p>
          </div>
        ) : (
          <>
            {/* 등급별 아이콘 표시 */}
            {selectedGrade === 'ALL' ? (
              // 전체 보기 - 등급별로 그룹화
              ['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'MYTHIC'].map(grade => {
                const gradeIcons = icons.filter(icon => icon.grade === grade);
                if (gradeIcons.length === 0) return null;
                
                const gradeInfo = {
                  COMMON: { name: '커몬 등급', icon: '⭐', color: 'from-slate-400 to-gray-400', count: gradeIcons.length },
                  RARE: { name: '레어 등급', icon: '✨', color: 'from-cyan-400 to-blue-400', count: gradeIcons.length },
                  EPIC: { name: '에픽 등급', icon: '🔮', color: 'from-purple-400 to-violet-400', count: gradeIcons.length },
                  LEGENDARY: { name: '레전드 등급', icon: '🌟', color: 'from-yellow-400 to-orange-400', count: gradeIcons.length },
                  MYTHIC: { name: '미스틱 등급', icon: '🌌', color: 'from-pink-400 to-purple-400', count: gradeIcons.length }
                };
                const info = gradeInfo[grade as keyof typeof gradeInfo];
                
                return (
                  <div key={grade} className="mb-12">
                    <div className="flex items-center justify-center mb-6">
                      <div className={`bg-gradient-to-r ${info.color} bg-clip-text text-transparent`}>
                        <h2 className="text-2xl font-bold flex items-center gap-3">
                          <span className="text-3xl">{info.icon}</span>
                          {info.name}
                          <span className="text-lg text-gray-400">({info.count}개)</span>
                        </h2>
                      </div>
                    </div>
                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
                      {gradeIcons.map(icon => (
                        <div key={icon.id} className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-4 hover:bg-[#252842]/80 transition-all duration-300 hover:scale-105">
                          <StellaIcon
                            icon={icon}
                            size="large"
                            owned={isOwned(icon.id)}
                            showName={true}
                            showPrice={true}
                          />
                          
                          <div className="mt-4">
                            {isOwned(icon.id) ? (
                              <button
                                onClick={() => handleEquip(icon.id)}
                                className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded-lg font-medium transition-all duration-200"
                              >
                                🎯 장착하기
                              </button>
                            ) : (
                              <button
                                onClick={() => handlePurchase(icon.id, icon.price)}
                                disabled={purchasing === icon.id || user.points < icon.price}
                                className={`w-full py-2 px-4 rounded-lg font-medium transition-all duration-200 ${
                                  user.points < icon.price
                                    ? 'bg-red-600/50 text-red-300 cursor-not-allowed'
                                    : purchasing === icon.id
                                    ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                                    : 'bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white shadow-lg hover:shadow-xl transform hover:scale-105'
                                }`}
                              >
                                {purchasing === icon.id ? (
                                  <span className="flex items-center justify-center">
                                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                                    구매중...
                                  </span>
                                ) : user.points < icon.price ? (
                                  '스텔라 부족'
                                ) : (
                                  `⭐ ${icon.price.toLocaleString()} 구매`
                                )}
                              </button>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })
            ) : (
              // 특정 등급 보기
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
                {filteredIcons.map(icon => (
                  <div key={icon.id} className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-4 hover:bg-[#252842]/80 transition-all duration-300 hover:scale-105">
                    <StellaIcon
                      icon={icon}
                      size="large"
                      owned={isOwned(icon.id)}
                      showName={true}
                      showPrice={true}
                    />
                    
                    <div className="mt-4">
                      {isOwned(icon.id) ? (
                        <button
                          onClick={() => handleEquip(icon.id)}
                          className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded-lg font-medium transition-all duration-200"
                        >
                          🎯 장착하기
                        </button>
                      ) : (
                        <button
                          onClick={() => handlePurchase(icon.id, icon.price)}
                          disabled={purchasing === icon.id || user.points < icon.price}
                          className={`w-full py-2 px-4 rounded-lg font-medium transition-all duration-200 ${
                            user.points < icon.price
                              ? 'bg-red-600/50 text-red-300 cursor-not-allowed'
                              : purchasing === icon.id
                              ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                              : 'bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white shadow-lg hover:shadow-xl transform hover:scale-105'
                          }`}
                        >
                          {purchasing === icon.id ? (
                            <span className="flex items-center justify-center">
                              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                              구매중...
                            </span>
                          ) : user.points < icon.price ? (
                            '스텔라 부족'
                          ) : (
                            `⭐ ${icon.price.toLocaleString()} 구매`
                          )}
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}

        {filteredIcons.length === 0 && !loading && selectedGrade !== 'ALL' && (
          <div className="text-center py-12">
            <div className="text-6xl mb-4">🚀</div>
            <p className="text-gray-400 text-lg">해당 등급의 아이콘이 없습니다.</p>
            <p className="text-gray-500 text-sm mt-2">다른 등급을 선택해보세요!</p>
          </div>
        )}
      </div>
    </div>
  );
}