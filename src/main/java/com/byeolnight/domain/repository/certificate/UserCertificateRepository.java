package com.byeolnight.domain.repository.certificate;

import com.byeolnight.domain.entity.certificate.Certificate;
import com.byeolnight.domain.entity.certificate.UserCertificate;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCertificateRepository extends JpaRepository<UserCertificate, Long> {

    // 사용자의 모든 인증서 조회
    List<UserCertificate> findByUserOrderByCreatedAtDesc(User user);

    // 사용자의 대표 인증서 조회
    Optional<UserCertificate> findByUserAndIsRepresentativeTrue(User user);

    // 특정 인증서 보유 여부 확인
    boolean existsByUserAndCertificateType(User user, Certificate.CertificateType certificateType);

    // 사용자의 기존 대표 인증서 해제
    @Modifying
    @Query("UPDATE UserCertificate uc SET uc.isRepresentative = false WHERE uc.user = :user")
    void unsetAllRepresentativeCertificates(@Param("user") User user);

    // 인증서별 보유자 수 통계
    @Query("SELECT uc.certificateType, COUNT(uc) FROM UserCertificate uc GROUP BY uc.certificateType")
    List<Object[]> getCertificateStatistics();

    // 사용자의 특정 인증서 조회
    Optional<UserCertificate> findByUserAndCertificateType(User user, Certificate.CertificateType certificateType);
}