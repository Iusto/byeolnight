export default function PrivacyPolicy() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] text-starlight p-8 max-w-3xl mx-auto">
      <h1 className="text-3xl font-bold mb-6 text-center">🔐 개인정보 처리방침</h1>

      <p className="text-sm mb-4">
        <strong>별 헤는 밤</strong> 커뮤니티는 이용자의 개인정보를 소중히 다루며, 정보통신망법 및 개인정보 보호법 등 관련 법령을 준수합니다.
      </p>

      <h2 className="text-xl font-semibold mt-6 mb-2">1. 수집 항목 및 목적</h2>
      <ul className="list-disc text-sm ml-6 space-y-1">
        <li>이메일, 비밀번호, 닉네임, 전화번호: 회원가입 및 본인 인증</li>
        <li>IP주소, 접속 로그: 보안 및 부정 이용 방지</li>
        <li>게시물/댓글/활동 기록: 서비스 운영 및 커뮤니티 품질 개선</li>
      </ul>
      
      <h2 className="text-xl font-semibold mt-6 mb-2">2. 개인정보 보안</h2>
      <ul className="list-disc text-sm ml-6 space-y-1">
        <li>🔐 전화번호는 AES-256 암호화 알고리즘을 사용하여 안전하게 저장됩니다</li>
        <li>비밀번호는 단방향 해시 암호화로 저장되어 복호화가 불가능합니다</li>
        <li>개인정보 접근 권한은 최소한의 관리자에게만 부여됩니다</li>
      </ul>

      <h2 className="text-xl font-semibold mt-6 mb-2">3. 보유 및 이용 기간</h2>
      <ul className="list-disc text-sm ml-6 space-y-1">
        <li>회원 탈퇴 시 지체 없이 삭제</li>
        <li>관계 법령에 따라 일정 기간 보관될 수 있음 (예: 수사 협조 등)</li>
      </ul>

      <h2 className="text-xl font-semibold mt-6 mb-2">4. 개인정보 제3자 제공</h2>
      <p className="text-sm ml-1 mb-2">
        원칙적으로 외부에 제공하지 않으며, 다음 경우 예외적으로 제공될 수 있습니다:
      </p>
      <ul className="list-disc text-sm ml-6 space-y-1">
        <li>법령에 의거한 수사기관 요청</li>
        <li>이용자의 별도 동의가 있는 경우</li>
      </ul>

      <h2 className="text-xl font-semibold mt-6 mb-2">5. 쿠키 및 로그</h2>
      <p className="text-sm ml-1">
        서비스 품질 향상을 위해 방문자 수, 페이지 이용 시간, 접속 기록 등이 자동 수집될 수 있으며, 이는 통계적 분석 목적 외에 사용되지 않습니다.
      </p>

      <h2 className="text-xl font-semibold mt-6 mb-2">6. 권리 및 문의</h2>
      <p className="text-sm ml-1">
        회원은 언제든지 개인정보 조회, 수정, 삭제, 처리정지 등을 요청할 수 있으며, 관련 문의는 <strong>privacy@byeolnight.com</strong> 또는 고객센터를 통해 가능합니다.
      </p>

      <p className="mt-8 text-xs text-gray-400">
        본 방침은 2025년 6월 25일부터 적용됩니다.
      </p>
    </div>
  );
}
