import { useEffect, useState, useCallback } from 'react';
import axios from '../../lib/axios';
import {
  AdminPageHeader,
  AdminSearchFilter,
  AdminStatsCard,
  IpBlockModal,
} from '../../components/admin';

export default function AdminIpPage() {
  const [blockedIps, setBlockedIps] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [showIpModal, setShowIpModal] = useState(false);

  const fetchBlockedIps = useCallback(async () => {
    try {
      const res = await axios.get('/admin/blocked-ips');
      const data = Array.isArray(res.data) ? res.data : (res.data?.data || []);
      setBlockedIps(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('차단된 IP 목록 조회 실패:', err);
      setBlockedIps([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchBlockedIps();
  }, [fetchBlockedIps]);

  const handleBlockIp = async (ip: string, duration: number) => {
    try {
      await axios.post('/admin/blocked-ips', {
        ip: ip,
        durationMinutes: duration
      });
      alert(`IP ${ip}가 ${duration}분간 차단되었습니다.`);
      fetchBlockedIps();
      setShowIpModal(false);
    } catch (err) {
      console.error('IP 차단 실패:', err);
      alert('IP 차단에 실패했습니다.');
    }
  };

  const handleUnblockIp = async (ip: string) => {
    if (!confirm(`IP ${ip}의 차단을 해제하시겠습니까?`)) return;
    try {
      await axios.delete(`/admin/blocked-ips?ip=${encodeURIComponent(ip)}`);
      alert(`IP ${ip}의 차단이 해제되었습니다.`);
      fetchBlockedIps();
    } catch (err) {
      console.error('IP 차단 해제 실패:', err);
      alert('IP 차단 해제에 실패했습니다.');
    }
  };

  const filteredIps = blockedIps.filter(ip =>
    searchTerm === '' || ip.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-purple-500"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="IP 차단 관리"
        description="IP 주소 차단 및 해제를 관리합니다."
      />

      <div className="flex justify-between items-center">
        <AdminSearchFilter
          searchValue={searchTerm}
          onSearchChange={setSearchTerm}
          searchPlaceholder="IP 주소 검색..."
        />
        <button
          onClick={() => setShowIpModal(true)}
          className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg font-medium transition"
        >
          + IP 차단 추가
        </button>
      </div>

      {/* 통계 */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        <AdminStatsCard
          title="차단된 IP"
          value={blockedIps.length}
          icon="🚫"
          color="red"
        />
        <AdminStatsCard
          title="검색 결과"
          value={filteredIps.length}
          color="cyan"
        />
      </div>

      {/* IP 목록 */}
      <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
        {filteredIps.length === 0 ? (
          <p className="text-center text-gray-400 py-8">
            {blockedIps.length === 0 ? '차단된 IP가 없습니다.' : '검색 결과가 없습니다.'}
          </p>
        ) : (
          <div className="grid gap-3">
            {filteredIps.map((ip) => (
              <div
                key={ip}
                className="flex items-center justify-between bg-[#2a2e45] p-4 rounded-lg"
              >
                <div className="flex items-center gap-3">
                  <span className="text-2xl">🚫</span>
                  <span className="text-white font-mono">{ip}</span>
                </div>
                <button
                  onClick={() => handleUnblockIp(ip)}
                  className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded text-sm font-medium transition"
                >
                  차단 해제
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* IP 차단 모달 */}
      {showIpModal && (
        <IpBlockModal
          onClose={() => setShowIpModal(false)}
          onConfirm={handleBlockIp}
        />
      )}
    </div>
  );
}