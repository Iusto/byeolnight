import { Link } from 'react-router-dom';

export default function About() {
  return (
    <article className="mx-auto max-w-3xl rounded-2xl border border-purple-500/20 bg-slate-900/60 p-6 text-gray-200 shadow-xl sm:p-10">
      <header className="mb-8 text-center">
        <p className="mb-2 text-sm font-semibold text-purple-300">ABOUT BYEOLNIGHT</p>
        <h1 className="text-3xl font-bold text-white">별 헤는 밤 소개</h1>
      </header>

      <div className="space-y-7 leading-7">
        <section>
          <h2 className="mb-2 text-xl font-semibold text-white">우주를 좋아하는 사람들의 커뮤니티</h2>
          <p>
            별 헤는 밤은 천문 관측, 우주 과학, 사진, 영화와 일상 이야기를 함께 나누는 커뮤니티입니다.
            게시판과 댓글, 실시간 채팅을 통해 서로의 경험과 지식을 편안하게 공유할 수 있습니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-xl font-semibold text-white">콘텐츠 운영 원칙</h2>
          <p>
            회원이 직접 작성한 콘텐츠와 출처가 표시된 우주 관련 소식을 제공합니다. 자동화 도구를 활용한
            콘텐츠는 출처, 중복 여부, 품질 기준을 확인하며 문제가 확인되면 수정하거나 공개를 중단합니다.
            신고된 게시물과 댓글은 운영 정책에 따라 검토합니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-xl font-semibold text-white">함께 만드는 안전한 공간</h2>
          <p>
            불법 콘텐츠, 저작권 침해, 혐오 표현, 개인정보 침해 및 스팸을 허용하지 않습니다. 콘텐츠에 문제가
            있다면 게시물의 신고 기능 또는 문의 채널을 이용해 주세요.
          </p>
        </section>
      </div>

      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <Link to="/posts" className="rounded-full bg-purple-600 px-5 py-2.5 font-medium text-white transition-colors hover:bg-purple-500">
          게시판 둘러보기
        </Link>
        <Link to="/contact" className="rounded-full border border-purple-400/50 px-5 py-2.5 font-medium text-purple-200 transition-colors hover:bg-purple-500/10">
          운영자에게 문의하기
        </Link>
      </div>
    </article>
  );
}
