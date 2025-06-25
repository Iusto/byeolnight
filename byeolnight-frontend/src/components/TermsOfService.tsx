export default function TermsOfService() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] text-starlight p-8 max-w-3xl mx-auto">
      <h1 className="text-3xl font-bold mb-6 text-center">📄 이용약관</h1>

      <p className="text-sm mb-4">
        본 약관은 ‘<strong>별 헤는 밤</strong>’ 커뮤니티(이하 “서비스”)에 가입하고 이용하는 모든 회원에게 적용됩니다. 회원가입 시 본 약관에 동의하는 것으로 간주합니다.
      </p>

      <h2 className="text-xl font-semibold mt-6 mb-2">1. 목적</h2>
      <p className="text-sm mb-2 ml-1">
        이 약관은 서비스의 이용 조건 및 절차, 이용자와 운영자의 권리와 의무, 책임사항 등을 규정함을 목적으로 합니다.
      </p>

      <h2 className="text-xl font-semibold mt-6 mb-2">2. 회원가입</h2>
      <ul className="list-disc text-sm ml-6 space-y-1">
        <li>실명 또는 실사용 가능한 이메일과 전화번호를 기반으로 가입해야 합니다.</li>
        <li>중복 가입, 다중 계정 생성, 제재 회피 목적의 가입은 금지되며 발견 시 계정은 즉시 정지됩니다.</li>
      </ul>

      <h2 className="text-xl font-semibold mt-6 mb-2">3. 이용 제한</h2>
      <p className="text-sm ml-1 mb-2">
        다음 행위가 확인될 경우, 사전 경고 없이 계정 이용이 제한되거나 영구 정지될 수 있습니다.
      </p>
      <ul className="list-disc text-sm ml-6 space-y-1">
        <li>욕설, 혐오 표현, 특정인/단체 비방</li>
        <li>음란, 불법 콘텐츠 게시</li>
        <li>타인의 개인정보 도용 또는 무단 게시</li>
        <li>스팸성 글/댓글, 홍보 목적의 무단 게시물</li>
      </ul>

      <h2 className="text-xl font-semibold mt-6 mb-2">4. 게시물 관리</h2>
      <p className="text-sm ml-1 mb-2">
        작성된 게시물은 작성자 본인에게 저작권이 있으며, 서비스 운영 및 홍보 목적으로 편집 또는 노출될 수 있습니다.
      </p>
      <ul className="list-disc text-sm ml-6 space-y-1">
        <li>탈퇴 시 게시물은 삭제되지 않으며, 사전 삭제가 필요합니다.</li>
        <li>신고 또는 약관 위반 게시물은 임의로 블라인드 처리될 수 있습니다.</li>
      </ul>

      <h2 className="text-xl font-semibold mt-6 mb-2">5. 책임의 한계</h2>
      <p className="text-sm ml-1">
        서비스는 사용자 간 커뮤니케이션을 중개할 뿐이며, 개별 사용자 간 발생한 분쟁 또는 법적 문제에 직접 책임지지 않습니다.
      </p>

      <p className="mt-8 text-xs text-gray-400">
        본 약관은 2025년 6월 25일부터 적용됩니다.
      </p>
    </div>
  );
}
