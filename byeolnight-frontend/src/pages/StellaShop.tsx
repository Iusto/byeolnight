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
    const baseClasses = 'px-6 py-3 rounded-xl font-bold transition-all duration-300 transform hover:scale-105 shadow-lg';
    if (isSelected) {
      return `${baseClasses} ${color} text-white shadow-xl ring-2 ring-white ring-opacity-50`;
    }
    return `${baseClasses} bg-gray-700 bg-opacity-50 text-gray-300 hover:bg-gray-600 hover:bg-opacity-70 backdrop-blur-sm`;
  };

  const getPurchaseButtonClassName = (canPurchase: boolean, isPurchasing: boolean) => {
    const baseClasses = 'w-full py-2 px-4 rounded-lg font-medium transition-all duration-200';
    if (!canPurchase) {
      return `${baseClasses} bg-red-600 bg-opacity-50 text-red-300 cursor-not-allowed`;
    }
    if (isPurchasing) {
      return `${baseClasses} bg-gray-600 text-gray-400 cursor-not-allowed`;
    }
    return `${baseClasses} bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white shadow-lg hover:shadow-xl transform hover:scale-105`;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold mb-4 bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
            {t('shop.title')}
          </h1>
          <p className="text-gray-400 mb-4">{t('shop.subtitle')}</p>
          {user ? (
            <div className="bg-[#1f2336] bg-opacity-80 backdrop-blur-md rounded-xl p-4 inline-block">
              <p className="text-yellow-400 font-bold text-lg">
                {t('shop.owned_stella')} {user.points?.toLocaleString() || 0}
              </p>
            </div>
          ) : (
            <div className="bg-[#1f2336] bg-opacity-80 backdrop-blur-md rounded-xl p-4 inline-block">
              <p className="text-gray-400 text-lg">
                {t('shop.login_required_purchase')}
              </p>
            </div>
          )}
        </div>

        <div className="flex flex-wrap justify-center gap-3 mb-8">
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
                <span className="mr-2">{info.icon}</span>
                {info.name}
              </button>
            );
          })}
        </div>

        {loading ? (
          <div className="text-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-400 mx-auto"></div>
            <p className="mt-4 text-gray-400">{t('shop.loading_icons')}</p>
          </div>
        ) : icons.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-red-400 text-lg">{t('shop.no_icon_data')}</p>
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
                  <div key={grade} className="mb-12">
                    <div className="flex items-center justify-center mb-6">
                      <div className={`bg-gradient-to-r ${info.color} bg-clip-text text-transparent`}>
                        <h2 className="text-2xl font-bold flex items-center gap-3">
                          <span className="text-3xl">{info.icon}</span>
                          {info.name}
                          <span className="text-lg text-gray-400">({info.count}{t('shop.count_suffix')})</span>
                        </h2>
                      </div>
                    </div>
                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
                      {gradeIcons.map(icon => (
                        <div key={icon.id} className="bg-[#1f2336] bg-opacity-80 backdrop-blur-md rounded-xl p-4 hover:bg-[#252842] hover:bg-opacity-80 transition-all duration-300 hover:scale-105">
                          <StellaIcon
                            icon={icon}
                            size="large"
                            owned={isOwned(icon.id)}
                            showName={true}
                            showPrice={true}
                          />
                          
                          <div className="mt-4">
                            {!user ? (
                              <button
                                onClick={() => alert(t('shop.login_required_alert'))}
                                className="w-full bg-gray-600 bg-opacity-50 text-gray-300 py-2 px-4 rounded-lg font-medium cursor-not-allowed"
                              >
                                {t('shop.login_required')}
                              </button>
                            ) : isOwned(icon.id) ? (
                              <button
                                onClick={() => handleEquip(icon.id)}
                                className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded-lg font-medium transition-all duration-200"
                              >
                                {t('shop.equip')}
                              </button>
                            ) : (
                              <button
                                onClick={() => handlePurchase(icon.id, icon.price)}
                                disabled={purchasing === icon.id || user.points < icon.price}
                                className={getPurchaseButtonClassName(user.points >= icon.price, purchasing === icon.id)}
                              >
                                {purchasing === icon.id ? (
                                  <span className="flex items-center justify-center">
                                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                                    {t('shop.purchasing')}
                                  </span>
                                ) : user.points < icon.price ? (
                                  t('shop.insufficient_stella')
                                ) : (
                                  `${t('shop.purchase')} ${icon.price.toLocaleString()}`
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
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
                {filteredIcons.map(icon => (
                  <div key={icon.id} className="bg-[#1f2336] bg-opacity-80 backdrop-blur-md rounded-xl p-4 hover:bg-[#252842] hover:bg-opacity-80 transition-all duration-300 hover:scale-105">
                    <StellaIcon
                      icon={icon}
                      size="large"
                      owned={isOwned(icon.id)}
                      showName={true}
                      showPrice={true}
                    />
                    
                    <div className="mt-4">
                      {!user ? (
                        <button
                          onClick={() => alert(t('shop.login_required_alert'))}
                          className="w-full bg-gray-600 bg-opacity-50 text-gray-300 py-2 px-4 rounded-lg font-medium cursor-not-allowed"
                        >
                          {t('shop.login_required')}
                        </button>
                      ) : isOwned(icon.id) ? (
                        <button
                          onClick={() => handleEquip(icon.id)}
                          className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded-lg font-medium transition-all duration-200"
                        >
                          {t('shop.equip')}
                        </button>
                      ) : (
                        <button
                          onClick={() => handlePurchase(icon.id, icon.price)}
                          disabled={purchasing === icon.id || user.points < icon.price}
                          className={getPurchaseButtonClassName(user.points >= icon.price, purchasing === icon.id)}
                        >
                          {purchasing === icon.id ? (
                            <span className="flex items-center justify-center">
                              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                              {t('shop.purchasing')}
                            </span>
                          ) : user.points < icon.price ? (
                            t('shop.insufficient_stella')
                          ) : (
                            `${t('shop.purchase')} ${icon.price.toLocaleString()}`
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
            <p className="text-gray-400 text-lg">{t('shop.no_icons_in_grade')}</p>
            <p className="text-gray-500 text-sm mt-2">{t('shop.try_other_grade')}</p>
          </div>
        )}
      </div>
    </div>
  );
}