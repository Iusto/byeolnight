import { useEffect, useState, useCallback } from 'react';
import axios from '../../lib/axios';
import { AdminPageHeader, AdminStatsCard } from '../../components/admin';

interface S3Status {
  bucketName: string;
  configuredRegion: string;
  actualRegion?: string;
  regionMatch: boolean;
  connectionStatus: string;
  bucketExists: boolean;
  error?: string;
  warning?: string;
  suggestion?: string;
}

export default function AdminFilesPage() {
  const [orphanImageCount, setOrphanImageCount] = useState(0);
  const [isCleaningFiles, setIsCleaningFiles] = useState(false);
  const [s3Status, setS3Status] = useState<S3Status | null>(null);
  const [showS3Status, setShowS3Status] = useState(false);
  const [loading, setLoading] = useState(true);

  const fetchOrphanImageCount = useCallback(async () => {
    try {
      const res = await axios.get('/admin/files/orphan-count');
      const count = res.data?.data || 0;
      setOrphanImageCount(count);
    } catch (err) {
      console.error('고아 이미지 개수 조회 실패:', err);
      setOrphanImageCount(0);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchS3Status = useCallback(async () => {
    try {
      const res = await axios.get('/admin/files/s3-status');
      const status = res.data?.data || {};
      setS3Status(status);
    } catch (err) {
      console.error('S3 상태 조회 실패:', err);
    }
  }, []);

  useEffect(() => {
    fetchOrphanImageCount();
    fetchS3Status();
  }, [fetchOrphanImageCount, fetchS3Status]);

  const handleCleanupOrphanImages = async () => {
    if (!confirm(`정말 ${orphanImageCount}개의 오래된 파일을 정리하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.`)) return;

    setIsCleaningFiles(true);
    try {
      const res = await axios.post('/admin/files/cleanup-orphans');
      const deletedCount = res.data?.data || 0;
      const message = res.data?.message || `${deletedCount}개의 파일이 정리되었습니다.`;
      alert(message);
      fetchOrphanImageCount();
    } catch (err) {
      console.error('파일 정리 실패:', err);
      alert('파일 정리에 실패했습니다.');
    } finally {
      setIsCleaningFiles(false);
    }
  };

  const handleRefresh = () => {
    fetchOrphanImageCount();
    fetchS3Status();
  };

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
        title="파일 정리 관리"
        description="고아 이미지 파일을 관리합니다."
      />

      {/* 통계 */}
      <div className="grid grid-cols-2 gap-4">
        <AdminStatsCard
          title="정리 대상 파일"
          value={orphanImageCount}
          icon="🗑️"
          color={orphanImageCount > 0 ? 'red' : 'green'}
          alert={orphanImageCount > 10}
        />
        <AdminStatsCard
          title="S3 상태"
          value={s3Status?.connectionStatus === 'SUCCESS' ? '정상' : '확인 필요'}
          icon="☁️"
          color={s3Status?.connectionStatus === 'SUCCESS' ? 'green' : 'orange'}
        />
      </div>

      {/* 고아 이미지 정리 카드 */}
      <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-lg font-semibold text-white mb-2">🗑️ 고아 이미지 정리</h3>
            <p className="text-gray-400 text-sm">
              업로드 후 게시글에 사용되지 않은 오래된 이미지 파일들을 정리합니다.
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setShowS3Status(!showS3Status)}
              className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-2 rounded text-sm transition"
            >
              🔍 S3 상태
            </button>
            <button
              onClick={handleRefresh}
              className="bg-gray-600 hover:bg-gray-700 text-white px-3 py-2 rounded text-sm transition"
              disabled={isCleaningFiles}
            >
              🔄 새로고침
            </button>
          </div>
        </div>

        <div className="flex items-center justify-between bg-[#2a2e45] p-4 rounded-lg">
          <div className="flex items-center gap-4">
            <div className="text-3xl">📊</div>
            <div>
              <div className="text-2xl font-bold text-white">
                {orphanImageCount.toLocaleString()}개
              </div>
              <div className="text-sm text-gray-400">정리 대상 파일</div>
            </div>
          </div>

          <button
            onClick={handleCleanupOrphanImages}
            disabled={orphanImageCount === 0 || isCleaningFiles}
            className={`px-6 py-3 rounded-lg font-medium transition ${
              orphanImageCount === 0 || isCleaningFiles
                ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                : 'bg-red-600 hover:bg-red-700 text-white hover:scale-105 shadow-lg'
            }`}
          >
            {isCleaningFiles ? (
              <div className="flex items-center gap-2">
                <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
                정리 중...
              </div>
            ) : (
              '🧹 파일 정리 실행'
            )}
          </button>
        </div>

        {orphanImageCount === 0 && (
          <div className="mt-4 p-3 bg-green-600/20 border border-green-600/50 rounded-lg">
            <div className="flex items-center gap-2 text-green-400">
              <span>✅</span>
              <span className="text-sm">정리할 파일이 없습니다. 시스템이 깨끗합니다!</span>
            </div>
          </div>
        )}
      </div>

      {/* S3 상태 정보 */}
      {showS3Status && s3Status && (
        <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
          <h3 className="text-lg font-semibold text-white mb-4">☁️ S3 스토리지 상태</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-[#2a2e45] p-4 rounded-lg">
              <p className="text-gray-400 text-sm">버킷 이름</p>
              <p className="text-white font-mono">{s3Status.bucketName || '-'}</p>
            </div>
            <div className="bg-[#2a2e45] p-4 rounded-lg">
              <p className="text-gray-400 text-sm">리전</p>
              <p className="text-white font-mono">{s3Status.configuredRegion || '-'}</p>
            </div>
            <div className="bg-[#2a2e45] p-4 rounded-lg">
              <p className="text-gray-400 text-sm">연결 상태</p>
              <p className={`font-medium ${s3Status.connectionStatus === 'SUCCESS' ? 'text-green-400' : 'text-red-400'}`}>
                {s3Status.connectionStatus === 'SUCCESS' ? '✅ 정상' : '❌ 오류'}
              </p>
            </div>
            <div className="bg-[#2a2e45] p-4 rounded-lg">
              <p className="text-gray-400 text-sm">버킷 존재</p>
              <p className={`font-medium ${s3Status.bucketExists ? 'text-green-400' : 'text-red-400'}`}>
                {s3Status.bucketExists ? '✅ 확인됨' : '❌ 없음'}
              </p>
            </div>
          </div>
          {s3Status.error && (
            <div className="mt-4 p-3 bg-red-600/20 border border-red-600/50 rounded-lg">
              <p className="text-red-400 text-sm">{s3Status.error}</p>
            </div>
          )}
          {s3Status.warning && (
            <div className="mt-4 p-3 bg-yellow-600/20 border border-yellow-600/50 rounded-lg">
              <p className="text-yellow-400 text-sm">{s3Status.warning}</p>
            </div>
          )}
        </div>
      )}

      {/* AWS Lifecycle 안내 */}
      <div className="bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-6 border border-purple-500/20">
        <h3 className="text-lg font-semibold text-white mb-4">📋 파일 관리 정책</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div className="bg-[#2a2e45] p-4 rounded-lg">
            <div className="text-blue-400 font-medium mb-2">🗂️ 고아 파일 정의</div>
            <ul className="text-gray-300 space-y-1">
              <li>• 업로드 후 7일 이상 경과</li>
              <li>• 게시글에 연결되지 않음</li>
              <li>• 임시 업로드 폴더에 위치</li>
            </ul>
          </div>
          <div className="bg-[#2a2e45] p-4 rounded-lg">
            <div className="text-green-400 font-medium mb-2">✅ 권장 사항</div>
            <ul className="text-gray-300 space-y-1">
              <li>• 주기적인 파일 정리 실행</li>
              <li>• AWS Lifecycle 정책 설정</li>
              <li>• S3 상태 모니터링</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}