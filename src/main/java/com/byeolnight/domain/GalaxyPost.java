package com.byeolnight.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class GalaxyPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String content;

    public GalaxyPost() {}

    // 🔧 테스트 코드에서 사용하는 필드
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // (선택) 추가 필드용 getter/setter
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
