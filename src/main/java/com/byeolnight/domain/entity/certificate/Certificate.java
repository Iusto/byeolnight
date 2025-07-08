package com.byeolnight.domain.entity.certificate;

// Certificate 엔티티는 enum으로만 관리하므로 테이블 생성하지 않음
public class Certificate {

    public enum CertificateType {
        STARLIGHT_EXPLORER("🌠", "별빛 탐험가", "우주 커뮤니틴에 첫발을 내디딘 탐험자"),
        SPACE_CITIZEN("🌍", "우주인 등록증", "인증을 마치고 기록을 시작한 공식 우주인"),
        GALAXY_COMMUNICATOR("📡", "은하 통신병", "커뮤니티 내 활발히 의견을 나누는 소통자"),
        STAR_OBSERVER("🔭", "별 관측 매니아", "밤하늘의 아름다움을 기록한 사진가"),
        CHAT_MASTER("🗨️", "별빛 채팅사", "실시간 우주 대화에 익숙한 우주인"),
        NIGHT_CITIZEN("🏅", "별 헤는 밤 시민증", "활동으로 신뢰를 얻은 커뮤니티 시민"),
        GUARDIAN("🛡️", "별빛 수호자", "질서를 지키는 정의로운 감시자"),
        EXPERIMENTER("⚙️", "우주 실험자", "다양한 분야에 기록을 남긴 콘텐츠 기여자"),
        SUGGESTION_KING("💡", "건의왕", "커뮤니티 발전에 직접 기여한 행동가"),
        ADMIN_MEDAL("🏆", "은하 관리자 훈장", "특별한 기여를 인정받은 명예 인증");

        private final String icon;
        private final String name;
        private final String description;

        CertificateType(String icon, String name, String description) {
            this.icon = icon;
            this.name = name;
            this.description = description;
        }

        public String getIcon() { return icon; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
}