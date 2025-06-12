
package com.byeolnight.domain.entity.file;

import com.byeolnight.domain.entity.post.Post;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
public class File {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;  // ✅ 여기도 @Getter 없으면 접근 불가

    private String originalName;
    private String uuidName;

    public void detachFromPost() {
        this.post = null;
    }
}

