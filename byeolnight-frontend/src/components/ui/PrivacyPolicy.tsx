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
        <li>회원 탈퇴 시, 게시물 작성자 식별을 위한 닉네임 및 이메일은 내부 정책에 따라 비식별 처리되어 최대 1년간 보관됩니다.</li>
        <li>이 보관은 게시물 소유자 표기 및 중복 가입 방지 등 정당한 운영 목적에 한해 사용되며, 기간 경과 후 즉시 삭제됩니다.</li>
        <li>작성한 게시물 및 댓글은 커뮤니티 연속성을 위해 "탈퇴한 사용자"로 표시되어 유지됩니다</li>
        <li>쪽지는 양쪽 모두 삭제 후 3년 경과 시 자동으로 영구 삭제됩니다</li>
        <li>관계 법령에 따라 일정 기간 보관이 필요한 경우 예외적으로 보관됩니다 (예: 수사 협조 등)</li>
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
      <p className="text-sm ml-1 mb-2">
        서비스 품질 향상을 위해 방문자 수, 페이지 이용 시간, 접속 기록 등이 자동 수집될 수 있으며, 이는 통계 분석 외의 용도로 사용되지 않습니다.
      </p>
      <h3 className="text-md font-semibold mt-4 mb-1 ml-1">쿠키 설정 거부 안내</h3>
      <p className="text-sm ml-2">
        이용자는 브라우저 설정을 통해 쿠키 저장을 거부하거나 삭제할 수 있습니다. 다만 이 경우 일부 서비스 이용에 제한이 있을 수 있습니다.
      </p>
      <h3 className="text-md font-semibold mt-4 mb-1 ml-1">삭제 로그 관리</h3>
      <p className="text-sm ml-2">
        모든 게시글, 댓글, 쪽지 삭제 작업은 운영 투명성을 위해 로그로 기록되며, 개인정보는 자동으로 마스킹 처리됩니다.
      </p>

      <h2 className="text-xl font-semibold mt-6 mb-2">6. 권리 및 문의</h2>
      <p className="text-sm ml-1">
        회원은 언제든지 개인정보 조회, 수정, 삭제, 처리정지 등을 요청할 수 있으며, 관련 문의는 <strong>byeolnightservice@gmail.com</strong> 또는 고객센터를 통해 가능합니다.
      </p>

      <h2 className="text-xl font-semibold mt-6 mb-2">7. 개인정보처리방침의 변경</h2>
      <p className="text-sm ml-1">
        본 방침은 법령이나 내부 정책 변경에 따라 수정될 수 있으며, 변경 시 별도 공지를 통해 사전에 안내드립니다.
      </p>

      <p className="mt-8 text-xs text-gray-400">
        본 방침은 2025년 1월 13일부터 적용됩니다.
      </p>
    </div>
  );
}
