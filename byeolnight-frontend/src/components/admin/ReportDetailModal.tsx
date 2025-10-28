import { useState, useEffect } from 'react';

interface ReportDetail {
  reportId: number;
  reporterNickname: string;
  reason: string;
  description?: string;
  reportedAt: string;
  reviewed: boolean;
  accepted?: boolean;
}

interface ReportDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  postTitle: string;
  reports: ReportDetail[];
  onApprove?: (reportId: number) => void;
  onReject?: (reportId: number, reason: string) => void;
  onRefresh?: () => void;
}

export default function ReportDetailModal({ isOpen, onClose, postTitle, reports, onApprove, onReject, onRefresh }: ReportDetailModalProps) {
  const [isVisible, setIsVisible] = useState(false);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [selectedReportId, setSelectedReportId] = useState<number | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  useEffect(() => {
    if (isOpen) {
      setIsVisible(true);
      console.log('모달 열림 - 게시글:', postTitle);
      console.log('모달 열림 - 신고 데이터:', reports);
    }
  }, [isOpen, postTitle, reports]);

  const handleClose = () => {
    setIsVisible(false);
    setTimeout(onClose, 150);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className={`bg-[#1f2336] rounded-xl p-6 w-full max-w-2xl mx-4 transform transition-all duration-150 ${
        isVisible ? 'scale-100 opacity-100' : 'scale-95 opacity-0'
      }`}>
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xl font-semibold text-white">📋 신고 상세 내역</h3>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-white text-2xl"
          >
            ×
          </button>
        </div>
        
        <div className="mb-4">
          <h4 className="text-lg text-white mb-2 truncate" title={postTitle}>게시글: {postTitle}</h4>
          <div className="flex justify-between items-center">
            <p className="text-sm text-gray-400">총 {reports.length}건의 신고</p>
            {reports.length > 5 && (
              <p className="text-xs text-yellow-400">⚠️ 많은 신고가 접수되었습니다</p>
            )}
          </div>
        </div>

        <div className="max-h-96 overflow-y-auto space-y-4 pr-2">
          {reports.length === 0 ? (
            <div className="text-center text-gray-400 py-8">
              신고 내역이 없습니다.
            </div>
          ) : (
            reports.map((report, index) => (
              <div key={report.reportId || index} className="bg-[#2a2e45] p-4 rounded-lg border-l-4 border-red-500">
                <div className="flex justify-between items-start mb-3">
                  <div className="flex items-center gap-2">
                    <span className="bg-red-600 text-white px-2 py-1 rounded text-xs font-medium">
                      {report.reason}
                    </span>
                    {report.reviewed ? (
                      <span className={`px-2 py-1 rounded text-xs ${
                        report.accepted ? 'bg-green-600 text-white' : 'bg-gray-600 text-white'
                      }`}>
                        {report.accepted ? '✅ 승인됨' : '❌ 거부됨'}
                      </span>
                    ) : (
                      <span className="px-2 py-1 rounded text-xs bg-yellow-600 text-white">
                        ⏳ 검토 대기
                      </span>
                    )}
                  </div>
                  <div className="text-right">
                    <div className="text-xs text-gray-400">
                      {new Date(report.reportedAt).toLocaleString()}
                    </div>
                    <div className="text-xs text-gray-500 mt-1">
                      신고자: {report.reporterNickname}
                    </div>
                  </div>
                  {!report.reviewed && onApprove && onReject && (
                    <div className="flex gap-2 mt-2">
                      <button
                        onClick={async () => {
                          if (onApprove) {
                            await onApprove(report.reportId);
                            if (onRefresh) await onRefresh();
                            handleClose();
                          }
                        }}
                        className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded text-xs font-medium transition"
                      >
                        ✅ 승인
                      </button>
                      <button
                        onClick={() => {
                          setSelectedReportId(report.reportId);
                          setShowRejectModal(true);
                        }}
                        className="bg-red-600 hover:bg-red-700 text-white px-3 py-1 rounded text-xs font-medium transition"
                      >
                        ❌ 거부
                      </button>
                    </div>
                  )}
                </div>
                {report.description && (
                  <div className="mt-3">
                    <div className="text-xs text-gray-400 mb-1">상세 설명:</div>
                    <p className="text-gray-300 text-sm p-3 bg-[#1f2336] rounded border-l-2 border-gray-600">
                      {report.description}
                    </p>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
        
        {reports.length > 10 && (
          <div className="mt-4 p-3 bg-yellow-900/30 border border-yellow-600/50 rounded-lg">
            <p className="text-yellow-400 text-sm">
              📊 대량 신고 감지: {reports.length}건의 신고가 접수되었습니다. 자동 블라인드 처리를 검토해주세요.
            </p>
          </div>
        )}

        <div className="flex justify-between items-center mt-6">
          <div className="text-sm text-gray-400">
            검토 완료: {reports.filter(r => r.reviewed).length}/{reports.length}건
          </div>
          <button
            onClick={handleClose}
            className="px-4 py-2 bg-gray-600 hover:bg-gray-700 text-white rounded transition"
          >
            닫기
          </button>
        </div>
        
        {/* 거부 사유 입력 모달 */}
        {showRejectModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-60">
            <div className="bg-[#1f2336] rounded-xl p-6 w-full max-w-md mx-4">
              <h4 className="text-lg font-semibold text-white mb-4">신고 거부 사유</h4>
              <textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="거부 사유를 입력하세요..."
                className="w-full h-24 px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none"
              />
              <div className="flex justify-end gap-2 mt-4">
                <button
                  onClick={() => {
                    setShowRejectModal(false);
                    setSelectedReportId(null);
                    setRejectReason('');
                  }}
                  className="px-4 py-2 bg-gray-600 hover:bg-gray-700 text-white rounded transition"
                >
                  취소
                </button>
                <button
                  onClick={async () => {
                    if (selectedReportId && onReject) {
                      await onReject(selectedReportId, rejectReason);
                      if (onRefresh) await onRefresh();
                      handleClose();
                    }
                    setShowRejectModal(false);
                    setSelectedReportId(null);
                    setRejectReason('');
                  }}
                  disabled={!rejectReason.trim()}
                  className="px-4 py-2 bg-red-600 hover:bg-red-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white rounded transition"
                >
                  거부
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}