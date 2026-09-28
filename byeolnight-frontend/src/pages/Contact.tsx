export default function Contact() {
  return (
    <article className="mx-auto max-w-3xl rounded-2xl border border-purple-500/20 bg-slate-900/60 p-6 text-gray-200 shadow-xl sm:p-10">
      <header className="mb-8 text-center">
        <p className="mb-2 text-sm font-semibold text-purple-300">CONTACT</p>
        <h1 className="text-3xl font-bold text-white">문의하기</h1>
        <p className="mt-3 text-gray-300">서비스 이용, 개인정보, 콘텐츠 권리 침해와 관련된 문의를 접수합니다.</p>
      </header>

      <section className="rounded-xl border border-slate-700 bg-slate-950/40 p-6">
        <h2 className="mb-3 text-lg font-semibold text-white">이메일 문의</h2>
        <a
          href="mailto:byeolnightservice@gmail.com"
          className="break-all text-lg font-medium text-purple-300 underline decoration-purple-400/50 underline-offset-4 hover:text-purple-200"
        >
          byeolnightservice@gmail.com
        </a>
        <p className="mt-4 text-sm leading-6 text-gray-400">
          게시물 또는 댓글 관련 문의에는 해당 페이지 주소와 문의 사유를 함께 적어 주세요. 개인정보 관련
          요청은 본인 확인을 위해 추가 정보가 필요할 수 있습니다.
        </p>
      </section>

      <section className="mt-6 text-sm leading-6 text-gray-300">
        <h2 className="mb-2 text-lg font-semibold text-white">문의 가능한 항목</h2>
        <ul className="list-disc space-y-1 pl-5">
          <li>서비스 이용 및 계정 관련 문의</li>
          <li>개인정보 열람, 정정, 삭제 및 처리정지 요청</li>
          <li>저작권 또는 기타 권리 침해 신고</li>
          <li>광고 및 제휴 관련 문의</li>
        </ul>
      </section>
    </article>
  );
}
