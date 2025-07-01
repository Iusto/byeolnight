package com.byeolnight.domain.entity.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_attendance", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "attendance_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public DailyAttendance(User user, LocalDate attendanceDate) {
        this.user = user;
        this.attendanceDate = attendanceDate;
    }

    public static DailyAttendance of(User user, LocalDate date) {
        return DailyAttendance.builder()
                .user(user)
                .attendanceDate(date)
                .build();
    }
}