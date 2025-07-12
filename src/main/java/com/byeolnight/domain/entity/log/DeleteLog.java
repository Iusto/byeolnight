package com.byeolnight.domain.entity.log;

import com.byeolnight.domain.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "delete_logs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    @Column(nullable = false)
    private Long deletedBy;

    @Column(length = 500)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String originalContent;

    public enum TargetType {
        POST, COMMENT, MESSAGE
    }

    public enum ActionType {
        SOFT_DELETE, BLIND, PERMANENT_DELETE
    }
}