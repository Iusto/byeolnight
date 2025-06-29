import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import StellaIcon from '../components/StellaIcon';

interface StellaIconData {
  id: number;
  name: string;
  description: string;
  iconUrl: string;
  price: number;
  grade: string;
  gradeColor: string;
  type: string;
  animationClass?: string;
  owned: boolean;
}

interface UserIconData {
  id: number;
  iconId: number;
  name: string;
  description: string;
  iconUrl: string;
  grade: string;
  gradeColor: string;
  type: string;
  animationClass?: string;
  purchasePrice: number;
  equipped: boolean;
  purchasedAt: string;
}

export default function StellaShop() {
  const [icons, setIcons] = useState<StellaIconData[]>([]);
  const [myIcons, setMyIcons] = useState<UserIconData[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'shop' | 'inventory'>('shop');
  const { user } = useAuth();

  useEffect(() => {
    fetchIcons();
    if (user) {
      fetchMyIcons();
    }
  }, [user]);

  const fetchIcons = async () => {
    try {
      const res = await axios.get('/shop/icons');
      setIcons(res.data.data || []);
    } catch (err) {
      console.error('아이콘 목록 조회 실패', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchMyIcons = async () => {
    try {
      const res = await axios.get('/shop/my-icons');
      setMyIcons(res.data.data || []);
    } catch (err) {
      console.error('내 아이콘 조회 실패', err);
    }
  };

  const handlePurchase = async (iconId: number, price: number) => {
    if (!user) {
      alert('로그인이 필요합니다.');
      return;
    }

    if (user.points < price) {
      alert('포인트가 부족합니다.');
      return;
    }

    if (!confirm(`${price} 포인트를 사용하여 이 아이콘을 구매하시겠습니까?`)) {
      return;
    }

    try {
      await axios.post(`/shop/purchase/${iconId}`);
      alert('아이콘을 성공적으로 구매했습니다!');
      fetchIcons();
      fetchMyIcons();
      // 사용자 정보 새로고침 필요
      window.location.reload();
    } catch (err: any) {
      alert(err?.response?.data?.message || '구매에 실패했습니다.');
    }
  };

  const handleEquip = async (iconId: number) => {
    try {
      await axios.post(`/shop/equip/${iconId}`);
      alert('아이콘을 장착했습니다!');
      fetchMyIcons();
    } catch (err: any) {
      alert(err?.response?.data?.message || '장착에 실패했습니다.');
    }
  };

  const handleUnequip = async () => {
    try {
      await axios.post('/shop/unequip');
      alert('아이콘을 해제했습니다!');
      fetchMyIcons();
    } catch (err: any) {
      alert(err?.response?.data?.message || '해제에 실패했습니다.');
    }
  };

  const ShopIconCard = ({ icon }: { icon: StellaIconData }) => (
    <div className="bg-[#1f2336]/80 backdrop-blur-md border border-gray-700 rounded-xl p-6 shadow-xl hover:shadow-2xl transition-all duration-300">
      <div className="text-center">
        <div className="relative mb-4">
          <div className="w-16 h-16 mx-auto flex items-center justify-center">
            <StellaIcon
              iconUrl={icon.iconUrl}
              animationClass={icon.animationClass}
              grade={icon.grade}
              size="lg"
            />
          </div>
          <div 
            className="absolute -top-2 -right-2 px-2 py-1 rounded-full text-xs font-bold text-white"
            style={{ backgroundColor: icon.gradeColor }}
          >
            {icon.grade}
          </div>
        </div>
        
        <h3 className="text-lg font-bold text-white mb-2">{icon.name}</h3>
        <p className="text-sm text-gray-300 mb-4">{icon.description}</p>
        
        <div className="flex items-center justify-between mb-4">
          <span className="text-yellow-400 font-bold">💰 {icon.price}P</span>
          <span className="text-xs text-gray-400">{icon.type}</span>
        </div>

        {icon.owned ? (
          <div className="bg-green-600 text-white px-4 py-2 rounded-lg font-medium">
            ✅ 보유중
          </div>
        ) : (
          <button
            onClick={() => handlePurchase(icon.id, icon.price)}
            className="w-full bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg font-medium transition-colors"
          >
            구매하기
          </button>
        )}
      </div>
    </div>
  );

  const MyIconCard = ({ icon }: { icon: UserIconData }) => (
    <div className="bg-[#1f2336]/80 backdrop-blur-md border border-gray-700 rounded-xl p-6 shadow-xl hover:shadow-2xl transition-all duration-300">
      <div className="text-center">
        <div className="relative mb-4">
          <div className="w-16 h-16 mx-auto flex items-center justify-center">
            <StellaIcon
              iconUrl={icon.iconUrl}
              animationClass={icon.animationClass}
              grade={icon.grade}
              size="lg"
            />
          </div>
          <div 
            className="absolute -top-2 -right-2 px-2 py-1 rounded-full text-xs font-bold text-white"
            style={{ backgroundColor: icon.gradeColor }}
          >
            {icon.grade}
          </div>
          {icon.equipped && (
            <div className="absolute -bottom-2 -right-2 bg-green-500 text-white rounded-full p-1">
              <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
              </svg>
            </div>
          )}
        </div>
        
        <h3 className="text-lg font-bold text-white mb-2">{icon.name}</h3>
        <p className="text-sm text-gray-300 mb-4">{icon.description}</p>
        
        <div className="flex items-center justify-between mb-4">
          <span className="text-yellow-400 font-bold">💰 {icon.purchasePrice}P</span>
          <span className="text-xs text-gray-400">{icon.type}</span>
        </div>

        {icon.equipped ? (
          <button
            onClick={handleUnequip}
            className="w-full bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg font-medium transition-colors"
          >
            해제하기
          </button>
        ) : (
          <button
            onClick={() => handleEquip(icon.iconId)}
            className="w-full bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg font-medium transition-colors"
          >
            장착하기
          </button>
        )}
      </div>
    </div>
  );

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin w-12 h-12 border-4 border-purple-500 border-t-transparent rounded-full mx-auto mb-4"></div>
          <p>로딩 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-7xl mx-auto">
        {/* 헤더 */}
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold mb-4 bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
            🌟 스텔라 아이콘 상점
          </h1>
          <p className="text-gray-300">우주 감성 아이콘으로 당신의 닉네임을 꾸며보세요</p>
          {user && (
            <div className="mt-4 inline-flex items-center gap-2 bg-[#1f2336]/80 px-4 py-2 rounded-lg">
              <span className="text-yellow-400">💰</span>
              <span className="font-bold">{user.points} 포인트</span>
            </div>
          )}
        </div>

        {/* 탭 메뉴 */}
        <div className="flex justify-center mb-8">
          <div className="bg-[#1f2336]/80 rounded-lg p-1">
            <button
              onClick={() => setActiveTab('shop')}
              className={`px-6 py-2 rounded-md transition-colors ${
                activeTab === 'shop'
                  ? 'bg-purple-600 text-white'
                  : 'text-gray-300 hover:text-white'
              }`}
            >
              🛒 상점
            </button>
            <button
              onClick={() => setActiveTab('inventory')}
              className={`px-6 py-2 rounded-md transition-colors ${
                activeTab === 'inventory'
                  ? 'bg-purple-600 text-white'
                  : 'text-gray-300 hover:text-white'
              }`}
            >
              📦 보관함
            </button>
          </div>
        </div>

        {/* 상점 탭 */}
        {activeTab === 'shop' && (
          <div>
            <h2 className="text-2xl font-bold mb-6 text-center">✨ 구매 가능한 아이콘</h2>
            
            {icons.length === 0 ? (
              <div className="text-center py-16">
                <div className="text-6xl mb-4">🌌</div>
                <h3 className="text-2xl font-semibold mb-2 text-gray-300">준비 중입니다</h3>
                <p className="text-gray-400">곧 멋진 스텔라 아이콘들이 출시될 예정입니다!</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {icons.map((icon) => (
                  <ShopIconCard key={icon.id} icon={icon} />
                ))}
              </div>
            )}
          </div>
        )}

        {/* 보관함 탭 */}
        {activeTab === 'inventory' && (
          <div>
            <h2 className="text-2xl font-bold mb-6 text-center">📦 내 아이콘 보관함</h2>
            
            {myIcons.length === 0 ? (
              <div className="text-center py-16">
                <div className="text-6xl mb-4">📦</div>
                <h3 className="text-2xl font-semibold mb-2 text-gray-300">아이콘이 없습니다</h3>
                <p className="text-gray-400">상점에서 멋진 스텔라 아이콘을 구매해보세요!</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {myIcons.map((icon) => (
                  <MyIconCard key={icon.id} icon={icon} />
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}