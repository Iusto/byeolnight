import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';

interface Certificate {
  type: string;
  name: string;
  icon: string;
  description: string;
  howToGet: string;
  owned: boolean;
  isRepresentative: boolean;
  issuedAt?: string;
}

export default function Certificates() {
  const { user } = useAuth();
  const [certificates, setCertificates] = useState<Certificate[]>([]);
  const [representative, setRepresentative] = useState<Certificate | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    
    fetchCertificates();
    fetchRepresentative();
  }, [user]);

  const fetchCertificates = async () => {
    try {
      const res = await axios.get('/member/certificates/all');
      setCertificates(res.data.data || []);
    } catch (err) {
      console.error('인증서 조회 실패', err);
    }
  };

  const fetchRepresentative = async () => {
    try {
      const res = await axios.get('/member/certificates/representative');
      setRepresentative(res.data.data);
    } catch (err) {
      console.error('대표 인증서 조회 실패', err);
    } finally {
      setLoading(false);
    }
  };

  const setAsRepresentative = async (certificateType: string) => {
    try {
      await axios.put(`/member/certificates/representative/${certificateType}`);
      fetchRepresentative();
      fetchCertificates();
      alert('대표 인증서가 설정되었습니다!');
    } catch (err) {
      alert('대표 인증서 설정에 실패했습니다.');
    }
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('ko-KR');
  };

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white flex items-center justify-center">
        <p>로그인이 필요합니다.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white flex items-center justify-center">
        <p>🌠 로딩 중...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-6xl mx-auto">
        <h1 className="text-4xl font-bold mb-8 text-center text-white drop-shadow-glow">
          🏆 우주 인증서 보관함
        </h1>

        {/* 대표 인증서 */}
        {representative && (
          <div className="mb-12 text-center">
            <h2 className="text-2xl font-semibold mb-4 text-purple-300">✨ 대표 인증서</h2>
            <div className="inline-block bg-gradient-to-r from-purple-600 to-blue-600 p-6 rounded-xl shadow-2xl">
              <div className="text-6xl mb-4">{representative.icon}</div>
              <h3 className="text-2xl font-bold mb-2">{representative.name}</h3>
              <p className="text-sm text-gray-200">{representative.description}</p>
              <p className="text-xs text-gray-300 mt-2">발급일: {formatDate(representative.issuedAt)}</p>
            </div>
          </div>
        )}

        {/* 인증서 통계 */}
        <div className="mb-8 text-center">
          <div className="inline-flex items-center gap-8 bg-[#1f2336]/80 backdrop-blur-md px-8 py-4 rounded-xl">
            <div>
              <div className="text-3xl font-bold text-purple-400">{certificates.filter(c => c.owned).length}</div>
              <div className="text-sm text-gray-300">보유 인증서</div>
            </div>
            <div className="w-px h-12 bg-gray-600"></div>
            <div>
              <div className="text-3xl font-bold text-blue-400">{certificates.length}</div>
              <div className="text-sm text-gray-300">전체 인증서</div>
            </div>
          </div>
        </div>

        {/* 인증서 목록 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {certificates.map((cert) => (
            <div
              key={cert.type}
              className={`backdrop-blur-md border rounded-xl p-6 shadow-xl transition-all duration-300 transform hover:scale-105 ${
                cert.owned 
                  ? 'bg-[#1f2336]/80 border-gray-700 hover:shadow-purple-700' 
                  : 'bg-gray-800/50 border-gray-600 opacity-75'
              }`}
            >
              <div className="text-center">
                <div className={`text-5xl mb-4 ${!cert.owned ? 'grayscale' : ''}`}>{cert.icon}</div>
                <h3 className={`text-xl font-bold mb-2 ${cert.owned ? 'text-white' : 'text-gray-400'}`}>
                  {cert.name}
                </h3>
                <p className={`text-sm mb-4 ${cert.owned ? 'text-gray-300' : 'text-gray-500'}`}>
                  {cert.description}
                </p>
                
                {cert.owned ? (
                  <>
                    <p className="text-xs text-gray-400 mb-4">발급일: {formatDate(cert.issuedAt!)}</p>
                    {cert.isRepresentative ? (
                      <div className="bg-gradient-to-r from-purple-600 to-blue-600 text-white px-4 py-2 rounded-lg text-sm font-semibold">
                        ✨ 대표 인증서
                      </div>
                    ) : (
                      <button
                        onClick={() => setAsRepresentative(cert.type)}
                        className="bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded-lg text-sm transition-colors"
                      >
                        대표로 설정
                      </button>
                    )}
                  </>
                ) : (
                  <>
                    <div className="bg-gray-700 text-gray-300 px-4 py-2 rounded-lg text-sm mb-3">
                      획득 방법
                    </div>
                    <p className="text-xs text-gray-400 leading-relaxed">{cert.howToGet}</p>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>

        {certificates.filter(c => c.owned).length === 0 && (
          <div className="text-center py-16">
            <div className="text-6xl mb-4">🌌</div>
            <h3 className="text-2xl font-semibold mb-2 text-gray-300">아직 보유한 인증서가 없습니다</h3>
            <p className="text-gray-400">다양한 활동을 통해 우주 인증서를 수집해보세요!</p>
          </div>
        )}
      </div>
    </div>
  );
}