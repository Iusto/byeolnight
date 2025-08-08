package com.byeolnight.entity.log;

import com.byeolnight.entity.common.BaseTimeEntity;
import com.byeolnight.entity.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "nickname_change_history")
public class NicknameChangeHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String previousNickname;
    private String newNickname;
    private String ipAddress;

    // 생성자 대신 정적 팩토리 사용
    protected NicknameChangeHistory() {} // JPA용

    public static NicknameChangeHistory create(User user, String previous, String next, String ip) {
        NicknameChangeHistory history = new NicknameChangeHistory();
        history.user = user;
        history.previousNickname = previous;
        history.newNickname = next;
        history.ipAddress = ip;
        return history;
    }
}
