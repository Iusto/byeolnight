import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useTranslation } from 'react-i18next';
import StellaIcon from '../components/StellaIcon';
import type { StellaIcon as StellaIconType, UserIcon } from '../types/stellaIcon';
import { stellaIcons } from '../data/stellaIcons';
import { stellaIcons as stellaIconsEn } from '../data/stellaIcons_en';
import { stellaIcons as stellaIconsJa } from '../data/stellaIcons_ja';
import axios from '../lib/axios';

export default function StellaShop() {
  const { user, refreshUserInfo } = useAuth();
  const { t, i18n } = useTranslation();
  const [icons, setIcons] = useState<StellaIconType[]>([]);
  const [ownedIcons, setOwnedIcons] = useState<UserIcon[]>([]);
  const [loading, setLoading] = useState(true);
  const [purchasing, setPurchasing] = useState<number | null>(null);
  const [selectedGrade, setSelectedGrade] = useState<string>('ALL');

  const grades = ['ALL', 'COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'MYTHIC'];

  const getLocalizedIcons = () => {
    switch (i18n.language) {
      case 'en':
        return stellaIconsEn;
      case 'ja':
        return stellaIconsJa;
      default:
        return stellaIcons;
    }
  };

  useEffect(() => {
    const fetchAll = async () => {
      await fetchIcons();
      if (user) {
        await fetchOwnedIcons();
      }
      setLoading(false);
    };

    fetchAll();
  }, [user, i18n.language]);

  const fetchIcons = async () => {
    try {
      const response = await axios.get('/public/shop/icons');
      if (response.data.success) {
        const localizedIcons = getLocalizedIcons();
        const mergedIcons = response.data.data.map((serverIcon: StellaIconType) => {
          const localIcon = localizedIcons.find(local => local.id === serverIcon.id);
          return {
            ...serverIcon,
            name: localIcon?.name || serverIcon.name,
            description: localIcon?.description || serverIcon.description
          };
        });
        setIcons(mergedIcons);
      } else {
        console.error('아이콘 목록 조회 실패:', response.data.message);
        setIcons(getLocalizedIcons());
      }
    } catch (err) {
      console.error('아이콘 목록 조회 실패:', err);
      setIcons(getLocalizedIcons());
    }
  };

  const fetchOwnedIcons = async () => {
    try {
      const response = await axios.get('/member/shop/my-icons');
      if (response.data.success) {
        setOwnedIcons(response.data.data);
      }
    } catch (err) {
      console.error('보유 아이콘 조회 실패:', err);
    }
  };

  const handlePurchase = async (iconId: number, price: number) => {
    if (!user) return;
    
    if (user.points < price) {
      alert(t('shop.insufficient_points'));
      return;
    }

    setPurchasing(iconId);
    try {
      const response = await axios.post(`/member/shop/purchase/${iconId}`);
      if (response.data.success) {
        alert(t('shop.purchase_success'));
        await fetchOwnedIcons();
        await refreshUserInfo();
      } else {
        alert(response.data.message || t('shop.purchase_failed'));
      }
    } catch (err: any) {
      alert(err.response?.data?.message || t('shop.purchase_failed'));
    } finally {
      setPurchasing(null);
    }
  };

  const isOwned = (iconId: number) => {
    return ownedIcons.some(owned => owned.iconId === iconId);
  };

  const handleEquip = async (iconId: number) => {
    try {
      const response = await axios.post(`/member/shop/icons/${iconId}/equip`);
      if (response.data.success) {
        alert(t('shop.equip_success'));
        await refreshUserInfo();
      } else {
        alert(response.data.message || t('shop.equip_failed'));
      }
    } catch (err: any) {
      alert(err.response?.data?.message || t('shop.equip_failed'));
    }
  };

  const filteredIcons = selectedGrade === 'ALL' 
    ? icons 
    : icons.filter(icon => icon.grade === selectedGrade);

  const getButtonClassName = (isSelected: boolean, color: string) => {
    const baseClasses = 'px-3 py-2 sm:px-6 sm:py-3 rounded-xl font-bold transition-all duration-300 transform hover:scale-105 active:scale-95 shadow-lg mobile-button touch-target touch-feedback min-h-[44px] flex items-center justify-center';
    if (isSelected) {
      return `${baseClasses} ${color} text-white shadow-xl ring-2 ring-white ring-opacity-50`;
    }
    return `${baseClasses} bg-gray-700 bg-opacity-50 text-gray-300 hover:bg-gray-600 hover:bg-opacity-70 active:bg-gray-500 backdrop-blur-sm`;
  };

  const getPurchaseButtonClassName = (canPurchase: boolean, isPurchasing: boolean) => {
    const baseClasses = 'w-full py-3 sm:py-2 px-4 rounded-lg font-medium transition-all duration-200 mobile-button touch-target touch-feedback min-h-[44px] flex items-center justify-center';
    if (!canPurchase) {
      return `${baseClasses} bg-red-600 bg-opacity-50 text-red-300 cursor-not-allowed`;
    }
    if (isPurchasing) {
      return `${baseClasses} bg-gray-600 text-gray-400 cursor-not-allowed`;
    }
    return `${baseClasses} bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 active:from-purple-800 active:to-blue-800 text-white shadow-lg hover:shadow-xl transform hover:scale-105 active:scale-95`;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-6 sm:py-12 px-3 sm:px-6 mobile-optimized mobile-scroll">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-6 sm:mb-8 mobile-header">
          <h1 className="text-2xl sm:text-4xl font-bold mb-2 sm:mb-4 bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent mobile-title">
            {t('shop.title')}
          </h1>
          <p className="text-gray-400 mb-3 sm:mb-4 text-sm sm:text-base mobile-subtitle">{t('shop.subtitle')}</p>
          {user ? (
            <div className="bg-[#1f2336] bg-opacity-80 backdrop-blur-md rounded-xl p-3 sm:p-4 inline-block mobile-card">
              <p className="text-yellow-400 font-bold text-base sm:text-lg mobile-text">
                {t('shop.owned_stella')} {user.points?.toLocaleString() || 0}
              </p>
            </div>
          ) : (
            <div className="bg-[#1f2336] bg-opacity-80 backdrop-blur-md rounded-xl p-3 sm:p-4 inline-block mobile-card">
              <p className="text-gray-400 text-base sm:text-lg mobile-text">
                {t('shop.login_required_purchase')}
              </p>
            </div>
          )}
        </div>

        <div className="flex flex-wrap justify-center gap-2 sm:gap-3 mb-6 sm:mb-8">
          {grades.map(grade => {
            const gradeInfo = {
              ALL: { name: t('shop.grades.all'), icon: '🌌', color: 'bg-gradient-to-r from-gray-600 to-gray-700' },
              COMMON: { name: t('shop.grades.common'), icon: '⭐', color: 'bg-gradient-to-r from-slate-600 to-gray-600' },
              RARE: { name: t('shop.grades.rare'), icon: '✨', color: 'bg-gradient-to-r from-cyan-600 to-blue-600' },
              EPIC: { name: t('shop.grades.epic'), icon: '🔮', color: 'bg-gradient-to-r from-purple-600 to-violet-600' },
              LEGENDARY: { name: t('shop.grades.legendary'), icon: '🌟', color: 'bg-gradient-to-r from-yellow-600 to-orange-600' },
              MYTHIC: { name: t('shop.grades.mythic'), icon: '🌌', color: 'bg-gradient-to-r from-pink-600 to-purple-600' }
            };
            const info = gradeInfo[grade as keyof typeof gradeInfo];
            return (
              <button
                key={grade}
                onClick={() => setSelectedGrade(grade)}
                className={getButtonClassName(selectedGrade === grade, info.color)}
              >
                <span className="mr-1 sm:mr-2 text-sm sm:text-base">{info.icon}</span>
                <span className="text-xs sm:text-sm mobile-caption">{info.name}</span>
              </button>
            );
          })}
        </div>

        {loading ? (
          <div className="text-center py-8 sm:py-12">
            <div className="animate-spin rounded-full h-8 w-8 sm:h-12 sm:w-12 border-b-2 border-purple-400 mx-auto"></div>
            <p className="mt-3 sm:mt-4 text-gray-400 text-sm sm:text-base mobile-text">{t('shop.loading_icons')}</p>
          </div>
        ) : icons.length === 0 ? (
          <div className="text-center py-8 sm:py-12">
            <p className="text-red-400 text-base sm:text-lg mobile-text">{t('shop.no_icon_data')}</p>
          </div>
        ) : (
          <>
            {selectedGrade === 'ALL' ? (
              ['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'MYTHIC'].map(grade => {
                const gradeIcons = icons.filter(icon => icon.grade === grade);
                if (gradeIcons.length === 0) return null;
                
                const gradeInfo = {
                  COMMON: { name: t('shop.grade_names.common'), icon: '⭐', color: 'from-slate-400 to-gray-400', count: gradeIcons.length },
                  RARE: { name: t('shop.grade_names.rare'), icon: '✨', color: 'from-cyan-400 to-blue-400', count: gradeIcons.length },
                  EPIC: { name: t('shop.grade_names.epic'), icon: '🔮', color: 'from-purple-400 to-violet-400', count: gradeIcons.length },
                  LEGENDARY: { name: t('shop.grade_names.legendary'), icon: '🌟', color: 'from-yellow-400 to-orange-400', count: gradeIcons.length },
                  MYTHIC: { name: t('shop.grade_names.mythic'), icon: '🌌', color: 'from-pink-400 to-purple-400', count: gradeIcons.length }
                };
                const info = gradeInfo[grade as keyof typeof gradeInfo];
                
                return (
                  <div key={grade} className="mb-8 sm:mb-12">
                    <div className="flex items-center justify-center mb-4 sm:mb-6">
                      <div className={`bg-gradient-to-r ${info.color} bg-clip-text text-transparent`}>
                        <h2 className="text-lg sm:text-2xl font-bold flex items-center gap-2 sm:gap-3 mobile-title">
                          <span className="text-xl sm:text-3xl">{info.icon}</span>
                          <span className="mobile-text">{info.name}</span>
                          <span className="text-sm sm:text-lg text-gray-400 mobile-caption">({info.count}{t('shop.count_suffix')})</span>
                        </h2>
                      </div>
                    </div>
                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 sm:gap-6 mobile-grid-2">
                      {gradeIcons.map(icon => (
                        <div key={icon.id} className="bg-[#1f2336] bg-opacity-80 backdrop-blur-md rounded-xl p-3 sm:p-4 hover:bg-[#252842] hover:bg-opacity-80 transition-all duration-300 hover:scale-105 active:scale-95 mobile-card-compact touch-feedback border border-transparent hover:border-purple-500/30">
                          <StellaIcon
                            icon={icon}
                            size="large"
                            owned={isOwned(icon.id)}
                            showName={true}
                            showPrice={true}
                          />
                          
                          <div className="mt-3 sm:mt-4">
                            {!user ? (
                              <button
                                onClick={() => alert(t('shop.login_required_alert'))}
                                className="w-full bg-gray-600 bg-opacity-50 text-gray-300 py-3 sm:py-2 px-4 rounded-lg font-medium cursor-not-allowed mobile-button touch-target min-h-[44px] flex items-center justify-center"
                              >
                                <span className="text-xs sm:text-sm mobile-caption">{t('shop.login_required')}</span>
                              </button>
                            ) : isOwned(icon.id) ? (
                              <button
                                onClick={() => handleEquip(icon.id)}
                                className="w-full bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white py-3 sm:py-2 px-4 rounded-lg font-medium transition-all duration-200 mobile-button touch-target touch-feedback min-h-[44px] flex items-center justify-center"
                              >
                                <span className="text-xs sm:text-sm mobile-caption">{t('shop.equip')}</span>
                              </button>
                            ) : (
                              <button
                                onClick={() => handlePurchase(icon.id, icon.price)}
                                disabled={purchasing === icon.id || user.points < icon.price}
                                className={getPurchaseButtonClassName(user.points >= icon.price, purchasing === icon.id)}
                              >
                                {purchasing === icon.id ? (
                                  <>
                                    <div className="animate-spin rounded-full h-3 w-3 sm:h-4 sm:w-4 border-b-2 border-white mr-2"></div>
                                    <span className="text-xs sm:text-sm mobile-caption">{t('shop.purchasing')}</span>
                                  </>
                                ) : user.points < icon.price ? (
                                  <span className="text-xs sm:text-sm mobile-caption">{t('shop.insufficient_stella')}</span>
                                ) : (
                                  <span className="text-xs sm:text-sm mobile-caption">{`${t('shop.purchase')} ${icon.price.toLocaleString()}`}</span>
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
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 sm:gap-6 mobile-grid-2">
                {filteredIcons.map(icon => (
                  <div key={icon.id} className="bg-[#1f2336] bg-opacity-80 backdrop-blur-md rounded-xl p-3 sm:p-4 hover:bg-[#252842] hover:bg-opacity-80 transition-all duration-300 hover:scale-105 active:scale-95 mobile-card-compact touch-feedback border border-transparent hover:border-purple-500/30">
                    <StellaIcon
                      icon={icon}
                      size="large"
                      owned={isOwned(icon.id)}
                      showName={true}
                      showPrice={true}
                    />
                    
                    <div className="mt-3 sm:mt-4">
                      {!user ? (
                        <button
                          onClick={() => alert(t('shop.login_required_alert'))}
                          className="w-full bg-gray-600 bg-opacity-50 text-gray-300 py-3 sm:py-2 px-4 rounded-lg font-medium cursor-not-allowed mobile-button touch-target min-h-[44px] flex items-center justify-center"
                        >
                          <span className="text-xs sm:text-sm mobile-caption">{t('shop.login_required')}</span>
                        </button>
                      ) : isOwned(icon.id) ? (
                        <button
                          onClick={() => handleEquip(icon.id)}
                          className="w-full bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white py-3 sm:py-2 px-4 rounded-lg font-medium transition-all duration-200 mobile-button touch-target touch-feedback min-h-[44px] flex items-center justify-center"
                        >
                          <span className="text-xs sm:text-sm mobile-caption">{t('shop.equip')}</span>
                        </button>
                      ) : (
                        <button
                          onClick={() => handlePurchase(icon.id, icon.price)}
                          disabled={purchasing === icon.id || user.points < icon.price}
                          className={getPurchaseButtonClassName(user.points >= icon.price, purchasing === icon.id)}
                        >
                          {purchasing === icon.id ? (
                            <>
                              <div className="animate-spin rounded-full h-3 w-3 sm:h-4 sm:w-4 border-b-2 border-white mr-2"></div>
                              <span className="text-xs sm:text-sm mobile-caption">{t('shop.purchasing')}</span>
                            </>
                          ) : user.points < icon.price ? (
                            <span className="text-xs sm:text-sm mobile-caption">{t('shop.insufficient_stella')}</span>
                          ) : (
                            <span className="text-xs sm:text-sm mobile-caption">{`${t('shop.purchase')} ${icon.price.toLocaleString()}`}</span>
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
          <div className="text-center py-8 sm:py-12">
            <div className="text-4xl sm:text-6xl mb-3 sm:mb-4">🚀</div>
            <p className="text-gray-400 text-base sm:text-lg mobile-text">{t('shop.no_icons_in_grade')}</p>
            <p className="text-gray-500 text-xs sm:text-sm mt-2 mobile-caption">{t('shop.try_other_grade')}</p>
          </div>
        )}
      </div>
    </div>
  );
}