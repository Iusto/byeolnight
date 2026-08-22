import { useCallback, useEffect, useState } from 'react';
import axios from '../../lib/axios';
import { getErrorMessage } from '../../types/api';

interface CinemaStatus {
  totalCinemaPosts?: number;
  latestPostExists?: boolean;
  latestPostTitle?: string;
  lastUpdated?: string;
  daysSinceLastUpdate?: number;
  systemHealthy?: boolean;
  warning?: string;
  todayPosts?: number;
  statusMessage?: string;
  error?: string;
  googleApiConfigured?: boolean;
  openaiApiConfigured?: boolean;
  lastExecution?: CinemaCollectionResult;
}

interface CinemaCollectionResult {
  status?: string;
  message?: string;
  searchedCandidates?: number;
  validCandidates?: number;
  selectedVideoId?: string;
  selectedTitle?: string;
  selectedScore?: number;
  executedAt?: string;
}

function unwrapData<T>(response: { data?: { data?: T } | T }): T | undefined {
  const body = response.data;
  if (body && typeof body === 'object' && 'data' in body) {
    return (body as { data?: T }).data;
  }
  return body as T | undefined;
}

function formatDate(value?: string) {
  if (!value) return '기록 없음';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('ko-KR');
}

function resultMessage(data: unknown, fallback: string) {
  if (typeof data === 'string' && data.trim()) return data;
  if (data && typeof data === 'object') {
    const result = data as Record<string, unknown>;
    for (const key of ['message', 'statusMessage', 'lastRunMessage']) {
      if (typeof result[key] === 'string' && result[key]) return result[key] as string;
    }
  }
  return fallback;
}

export default function CinemaManagementCard() {
  const [status, setStatus] = useState<CinemaStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [notice, setNotice] = useState<{ type: 'success' | 'warning' | 'error'; message: string } | null>(null);

  const fetchStatus = useCallback(async () => {
    try {
      const response = await axios.get('/admin/cinema/status');
      setStatus(unwrapData<CinemaStatus>(response) ?? null);
    } catch (error) {
      setNotice({ type: 'error', message: `별빛시네마 상태 조회 실패: ${getErrorMessage(error)}` });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchStatus();
  }, [fetchStatus]);

  const generateCinema = async () => {
    if (!window.confirm('오늘의 별빛시네마 영상을 다시 수집하시겠습니까?')) return;

    setGenerating(true);
    setNotice(null);
    try {
      const response = await axios.post('/admin/cinema/generate-post');
      const data = unwrapData<CinemaCollectionResult>(response);
      const succeeded = data?.status === 'CREATED' || data?.status === 'ALREADY_CREATED_TODAY';
      setNotice({
        type: succeeded ? 'success' : 'warning',
        message: resultMessage(data, '별빛시네마 수집 요청이 완료되었습니다.'),
      });
      await fetchStatus();
    } catch (error) {
      setNotice({ type: 'error', message: getErrorMessage(error) });
    } finally {
      setGenerating(false);
    }
  };

  const todayCreated = (status?.todayPosts ?? 0) > 0;
  const healthy = status?.systemHealthy ?? todayCreated;
  const lastExecution = status?.lastExecution;
  const runMessage = lastExecution?.message ?? status?.statusMessage ?? status?.warning ?? status?.error;
  const apiConfigured = status?.googleApiConfigured !== false && status?.openaiApiConfigured !== false;

  return (
    <section className="lg:col-span-2 rounded-xl border border-fuchsia-500/20 bg-[#1f2336]/80 p-6 backdrop-blur-md">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="flex items-center gap-2 text-lg font-semibold text-white">🎬 별빛시네마 운영</h2>
          <p className="mt-1 text-sm text-gray-400">매일 큐레이션 상태와 최근 실행 결과를 확인합니다.</p>
        </div>
        <button
          type="button"
          onClick={generateCinema}
          disabled={generating}
          className="rounded-lg bg-fuchsia-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-fuchsia-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {generating ? '후보 검토 중…' : '오늘 영상 수집 실행'}
        </button>
      </div>

      {loading ? (
        <div className="mt-5 h-24 animate-pulse rounded-lg bg-slate-700/40" />
      ) : (
        <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <StatusItem label="오늘 등록" value={todayCreated ? '완료' : '미등록'} ok={todayCreated} />
          <StatusItem label="시스템 상태" value={healthy && apiConfigured ? '정상' : '확인 필요'} ok={healthy && apiConfigured} />
          <StatusItem label="마지막 등록" value={formatDate(status?.lastUpdated)} />
          <StatusItem label="검토 후보" value={`${lastExecution?.validCandidates ?? 0}개`} />
        </div>
      )}

      {status?.latestPostTitle && (
        <p className="mt-4 truncate text-sm text-slate-300">
          <span className="text-slate-500">최근 영상</span> · {status.latestPostTitle}
        </p>
      )}
      {(lastExecution?.status || runMessage) && (
        <div className="mt-3 rounded-lg border border-slate-600/50 bg-slate-900/40 p-3 text-sm text-slate-300">
          {lastExecution?.status && <strong className="mr-2 text-fuchsia-300">{lastExecution.status}</strong>}
          {runMessage}
          {lastExecution?.selectedVideoId && (
            <a
              href={`https://www.youtube.com/watch?v=${lastExecution.selectedVideoId}`}
              target="_blank"
              rel="noreferrer"
              className="ml-2 text-purple-300 underline hover:text-purple-200"
            >
              선택 영상 보기
            </a>
          )}
        </div>
      )}
      {notice && (
        <div
          role="status"
          className={`mt-3 rounded-lg border p-3 text-sm ${
            notice.type === 'success'
              ? 'border-green-500/30 bg-green-500/10 text-green-300'
              : notice.type === 'warning'
                ? 'border-amber-500/30 bg-amber-500/10 text-amber-300'
              : 'border-red-500/30 bg-red-500/10 text-red-300'
          }`}
        >
          {notice.message}
        </div>
      )}
    </section>
  );
}

function StatusItem({ label, value, ok }: { label: string; value: string; ok?: boolean }) {
  return (
    <div className="rounded-lg bg-[#2a2e45] p-4">
      <p className="text-xs text-gray-400">{label}</p>
      <div className="mt-1 flex items-center gap-2">
        {ok !== undefined && <span className={`h-2.5 w-2.5 rounded-full ${ok ? 'bg-green-500' : 'bg-amber-500'}`} />}
        <p className="text-sm font-semibold text-white">{value}</p>
      </div>
    </div>
  );
}
