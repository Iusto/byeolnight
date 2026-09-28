export default function PrivacyPolicy() {
  return (
    <article className="mx-auto max-w-3xl rounded-2xl border border-purple-500/20 bg-slate-900/60 p-6 text-gray-200 shadow-xl sm:p-10">
      <h1 className="mb-3 text-center text-3xl font-bold text-white">개인정보처리방침</h1>
      <p className="mb-8 text-center text-sm text-gray-400">시행일: 2026년 9월 28일</p>

      <p className="text-sm leading-6">
        <strong className="text-white">별 헤는 밤</strong>은 이용자의 개인정보를 소중히 다루며 개인정보 보호법 등
        관련 법령을 준수합니다. 이 방침은 서비스에서 어떤 정보를 처리하고 어떻게 보호하는지 안내합니다.
      </p>

      <h2 className="mt-8 mb-2 text-xl font-semibold text-white">1. 수집 항목 및 이용 목적</h2>
      <ul className="ml-6 list-disc space-y-1 text-sm leading-6">
        <li>이메일, 비밀번호, 닉네임, 전화번호: 회원가입, 본인 확인 및 계정 관리</li>
        <li>IP 주소, 접속 기록, 기기 및 브라우저 정보: 보안, 장애 대응 및 부정 이용 방지</li>
        <li>게시물, 댓글, 쪽지 및 활동 기록: 커뮤니티 기능 제공과 서비스 품질 개선</li>
        <li>소셜 로그인 식별 정보: 소셜 계정 연동과 로그인 제공</li>
      </ul>

      <h2 className="mt-8 mb-2 text-xl font-semibold text-white">2. 개인정보 보호 조치</h2>
      <ul className="ml-6 list-disc space-y-1 text-sm leading-6">
        <li>전화번호 등 보호가 필요한 정보는 암호화하여 저장합니다.</li>
        <li>비밀번호는 단방향 해시로 처리하며 원문을 저장하지 않습니다.</li>
        <li>개인정보 접근 권한을 필요한 관리자에게만 제한합니다.</li>
      </ul>

      <h2 className="mt-8 mb-2 text-xl font-semibold text-white">3. 보유 및 이용 기간</h2>
      <ul className="ml-6 list-disc space-y-1 text-sm leading-6">
        <li>회원 탈퇴 시 계정 개인정보는 지체 없이 삭제하거나 비식별 처리합니다.</li>
        <li>중복 가입 방지와 게시물 작성자 표시를 위해 필요한 최소 정보는 내부 정책에 따라 최대 1년간 비식별 형태로 보관할 수 있습니다.</li>
        <li>작성한 게시물과 댓글은 커뮤니티의 연속성을 위해 작성자가 “탈퇴한 사용자”로 변경된 뒤 유지될 수 있으며, 탈퇴 전에 직접 삭제할 수 있습니다.</li>
        <li>쪽지는 양쪽 이용자가 모두 삭제한 뒤 3년이 지나면 영구 삭제됩니다.</li>
        <li>법령에서 별도 보관 기간을 정한 경우에는 해당 기간 동안 보관합니다.</li>
      </ul>

      <h2 className="mt-8 mb-2 text-xl font-semibold text-white">4. 외부 서비스 및 개인정보 처리</h2>
      <p className="text-sm leading-6">
        서비스는 로그인, 이미지 보관, 보안, 광고 등 기능 제공을 위해 외부 서비스를 사용할 수 있습니다.
        법령상 요구되는 경우를 제외하고 이용자의 개인정보를 목적과 무관하게 판매하지 않습니다.
      </p>

      <h3 className="mt-5 mb-2 font-semibold text-purple-200">Google AdSense 광고</h3>
      <ul className="ml-6 list-disc space-y-1 text-sm leading-6">
        <li>Google을 포함한 제3자 광고 사업자는 쿠키를 사용하여 이용자의 이전 웹사이트 방문 기록을 바탕으로 광고를 게재할 수 있습니다.</li>
        <li>Google과 광고 파트너는 광고 쿠키를 이용해 이 사이트 또는 다른 사이트 방문 기록에 기반한 맞춤형 광고를 제공할 수 있습니다.</li>
        <li>광고 제공 과정에서 쿠키 식별자, IP 주소, 기기·브라우저 정보 및 광고 상호작용 정보가 처리될 수 있습니다.</li>
        <li>
          이용자는{' '}
          <a className="text-purple-300 underline underline-offset-2 hover:text-purple-200" href="https://adssettings.google.com/" target="_blank" rel="noreferrer">
            Google 광고 설정
          </a>
          에서 맞춤형 광고를 관리하거나 해제할 수 있습니다.
        </li>
        <li>
          자세한 내용은{' '}
          <a className="text-purple-300 underline underline-offset-2 hover:text-purple-200" href="https://policies.google.com/technologies/partner-sites?hl=ko" target="_blank" rel="noreferrer">
            Google 파트너 사이트에서의 정보 사용 안내
          </a>
          를 확인할 수 있습니다.
        </li>
      </ul>

      <h2 className="mt-8 mb-2 text-xl font-semibold text-white">5. 쿠키 및 선택권</h2>
      <p className="text-sm leading-6">
        로그인 유지, 보안, 이용 통계 및 광고 제공을 위해 쿠키 또는 유사 기술을 사용할 수 있습니다. 이용자는
        브라우저 설정에서 쿠키를 삭제하거나 저장을 거부할 수 있으며, 이 경우 로그인 등 일부 기능이 제한될 수
        있습니다. 동의가 필요한 지역에서는 동의 관리 화면을 통해 광고 관련 선택을 제공하며, 표시되는 개인정보
        선택 메뉴에서 결정을 변경할 수 있습니다.
      </p>

      <h2 className="mt-8 mb-2 text-xl font-semibold text-white">6. 이용자의 권리 및 문의</h2>
      <p className="text-sm leading-6">
        이용자는 개인정보의 열람, 정정, 삭제 및 처리정지를 요청할 수 있습니다. 관련 요청은{' '}
        <a className="text-purple-300 underline underline-offset-2 hover:text-purple-200" href="mailto:byeolnightservice@gmail.com">
          byeolnightservice@gmail.com
        </a>
        으로 보내 주세요. 요청자 확인이 필요한 경우 최소한의 확인 절차를 거칠 수 있습니다.
      </p>

      <h2 className="mt-8 mb-2 text-xl font-semibold text-white">7. 방침의 변경</h2>
      <p className="text-sm leading-6">
        법령, 서비스 또는 외부 서비스 변경에 따라 이 방침을 수정할 수 있으며 중요한 변경 사항은 서비스 내
        공지를 통해 안내합니다.
      </p>
    </article>
  );
}
