export default function TermsOfService() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b0c2a] to-[#1a1c40] text-starlight p-8 max-w-3xl mx-auto">
      <h1 className="text-3xl font-bold mb-6 text-center">📄 이용약관</h1>

      <p className="text-sm mb-4">
        본 약관은 ‘<strong>별 헤는 밤</strong>’ 커뮤니티(이하 “서비스”)에 가입하고 이용하는 모든 회원에게 적용됩니다. 회원가입 시 본 약관에 동의하는 것으로 간주합니다.
      </p>

      <h2 className="text-xl font-semibold mt-6 mb-2">1. 정의</h2>
      <ul className="list-disc text-sm ml-6 space-y-1">
        <li>“회원”이라 함은 본 약관에 동의하고 서비스를 이용하는 개인 또는 단체를 말합니다.</li>
        <li>“운영자”라 함은 본 서비스를 관리, 운영하는 주체(byeolservice 관리자)를 말합니다.</li>
      </ul>

      <h2 className="text-xl font-semibold mt-6 mb-2">2. 목적</h2>
      <p className="text-sm ml-1 mb-2">
        이 약관은 서비스의 이용 조건 및 절차, 회원과 운영자의 권리와 의무, 책임사항 등을 규정함을 목적으로 합니다.
      </p>

      <h2 className="text-xl font-semibold mt-6 mb-2">3. 회원가입</h2>
      <ul className="list-disc text-sm ml-6 space-y-1">
        <li>실명 또는 실사용 가능한 이메일과 전화번호를 기반으로 가입해야 합니다.</li>
        <li>중복 가입, 다중 계정 생성, 제재 회피 목적의 가입은 금지되며, 발견 시 계정은 즉시 정지됩니다.</li>
      </ul>

      <h2 className="text-xl font-semibold mt-6 mb-2">4. 이용 제한</h2>
      <p className="text-sm ml-1 mb-2">
        다음 행위가 확인될 경우, 사전 경고 없이 계정 이용이 제한되거나 영구 정지될 수 있습니다:
      </p>
      <ul className="list-disc text-sm ml-6 space-y-1">
        <li>욕설, 혐오 표현, 특정인/단체 비방</li>
        <li>음란물, 불법 콘텐츠 게시</li>
        <li>타인의 개인정보 도용 또는 무단 게시</li>
        <li>스팸성 글/댓글, 무단 홍보 행위</li>
      </ul>
      <p className="text-sm mt-2 ml-1">
        제재는 위반의 정도에 따라 7일 이용 정지부터 영구 정지까지 적용되며, <strong>byeolservice@gmail.com</strong>으로 이의제기를 요청할 수 있습니다.
      </p>

      <h2 className="text-xl font-semibold mt-6 mb-2">5. 게시물 관리</h2>
      <p className="text-sm ml-1 mb-2">
        회원이 작성한 게시물은 작성자에게 저작권이 있으나, 서비스 운영 및 홍보를 위해 일부 콘텐츠는 편집 또는 노출될 수 있습니다.
      </p>
      <ul className="list-disc text-sm ml-6 space-y-1">
        <li>회원 탈퇴 시 작성한 게시물은 자동 삭제되지 않으며, 필요한 경우 탈퇴 전 직접 삭제하셔야 합니다.</li>
        <li>신고되거나 약관을 위반한 게시물은 운영자 판단에 따라 블라인드 처리될 수 있습니다.</li>
      </ul>

      <h2 className="text-xl font-semibold mt-6 mb-2">6. 책임의 한계</h2>
      <p className="text-sm ml-1 mb-2">
        서비스는 회원 간 커뮤니케이션을 위한 플랫폼을 제공할 뿐이며, 회원 간의 분쟁이나 법적 문제에 대해 직접적인 책임을 지지 않습니다.
      </p>

      <h2 className="text-xl font-semibold mt-6 mb-2">7. 약관의 변경</h2>
      <p className="text-sm ml-1 mb-2">
        운영자는 본 약관을 개정할 수 있으며, 개정 내용은 서비스 내 공지사항을 통해 사전 고지합니다. 변경된 약관은 공지 시점부터 효력을 가지며, 회원이 계속해서 서비스를 이용하는 경우 변경된 약관에 동의한 것으로 간주됩니다.
      </p>

      <p className="mt-8 text-xs text-gray-400">
        본 약관은 2025년 7월 13일부터 적용됩니다.
      </p>
    </div>
  );
}
