package com.byeolnight.domain.entity.certificate;

// Certificate 엔티티는 enum으로만 관리하므로 테이블 생성하지 않음
public class Certificate {

    public enum CertificateType {
        STARLIGHT_EXPLORER("🌠", "별빛 탐험가", "첫 로그인 시 자동 발급"),
        SPACE_CITIZEN("🪐", "우주인 등록", "회원가입과 인증 완료 시 발급"),
        GALAXY_COMMUNICATOR("🚀", "은하 통신병", "댓글을 5회 이상 작성 시 발급"),
        STAR_OBSERVER("🌌", "별 관측 매니아", "IMAGE 게시판에 사진을 3장 이상 업로드 시 발급"),
        TOUR_MASTER("🔭", "견학 마스터", "천문대 견학 일정을 3회 이상 조회 시 발급"),
        CHAT_MASTER("💬", "별빛 채팅사", "채팅방에서 5회 이상 대화 참여 시 발급"),
        NIGHT_CITIZEN("🌟", "별 헤는 밤 시민증", "포인트 200점 이상 누적 시 발급"),
        GUARDIAN("🛰️", "별빛 수호자", "게시글 신고로 커뮤니티 정화에 기여 시 발급"),
        EXPERIMENTER("🧪", "우주 실험자", "게시글을 3건 이상 작성 시 발급"),
        ADMIN_MEDAL("👑", "은하 관리자 훈장", "관리자가 특별 기여를 인정하여 수동 발급");

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