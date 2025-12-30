package com.byeolnight.service.user;

import com.byeolnight.entity.user.DailyAttendance;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.config.QueryDslConfig;
import com.byeolnight.repository.user.DailyAttendanceRepository;
import com.byeolnight.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 출석 체크 중복 방지 테스트
 * - DB UNIQUE 제약조건으로 멱등성 보장
 * - 분산락 없이도 중복 출석 방지
 */
@DataJpaTest
@Import(QueryDslConfig.class)
class PointServiceAttendanceTest {

    @Autowired
    private DailyAttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .role(User.Role.USER)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("출석 체크 - 정상 케이스")
    void checkAttendance_Success() {
        // Given
        LocalDate today = LocalDate.now();

        // When
        DailyAttendance attendance = DailyAttendance.of(testUser, today);
        DailyAttendance saved = attendanceRepository.save(attendance);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getAttendanceDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("출석 체크 중복 - DB 제약조건으로 방지")
    void checkAttendance_Duplicate_ThrowsException() {
        // Given
        LocalDate today = LocalDate.now();
        DailyAttendance first = DailyAttendance.of(testUser, today);
        attendanceRepository.save(first);
        entityManager.flush(); // DB에 즉시 반영

        // When & Then - 같은 날짜에 다시 출석 시도
        DailyAttendance duplicate = DailyAttendance.of(testUser, today);

        assertThatThrownBy(() -> {
            attendanceRepository.save(duplicate);
            entityManager.flush(); // flush 시점에 제약조건 위반 발생
        })
        .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("다른 날짜 출석 - 정상 처리")
    void checkAttendance_DifferentDate_Success() {
        // Given
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();

        // When
        DailyAttendance yesterdayAttendance = attendanceRepository.save(DailyAttendance.of(testUser, yesterday));
        DailyAttendance todayAttendance = attendanceRepository.save(DailyAttendance.of(testUser, today));

        // Then
        assertThat(yesterdayAttendance.getId()).isNotNull();
        assertThat(todayAttendance.getId()).isNotNull();
        assertThat(attendanceRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("다른 사용자 같은 날짜 출석 - 정상 처리")
    void checkAttendance_DifferentUser_SameDate_Success() {
        // Given
        User anotherUser = User.builder()
                .email("another@test.com")
                .nickname("another")
                .role(User.Role.USER)
                .build();
        anotherUser = userRepository.save(anotherUser);

        LocalDate today = LocalDate.now();

        // When
        DailyAttendance userAttendance = attendanceRepository.save(DailyAttendance.of(testUser, today));
        DailyAttendance anotherAttendance = attendanceRepository.save(DailyAttendance.of(anotherUser, today));

        // Then
        assertThat(userAttendance.getId()).isNotNull();
        assertThat(anotherAttendance.getId()).isNotNull();
        assertThat(attendanceRepository.count()).isEqualTo(2);
    }
}
