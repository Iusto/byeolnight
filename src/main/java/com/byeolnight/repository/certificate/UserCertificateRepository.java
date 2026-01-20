package com.byeolnight.repository.certificate;

import com.byeolnight.entity.certificate.Certificate;
import com.byeolnight.entity.certificate.UserCertificate;
import com.byeolnight.entity.user.User;
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

    // 사용자의 특정 인증서 조회
    Optional<UserCertificate> findByUserAndCertificateType(User user, Certificate.CertificateType certificateType);

    // 여러 사용자의 대표 인증서 배치 조회 (N+1 방지)
    @Query("SELECT uc FROM UserCertificate uc WHERE uc.user IN :users AND uc.isRepresentative = true")
    List<UserCertificate> findByUserInAndIsRepresentativeTrue(@Param("users") java.util.Set<User> users);
}