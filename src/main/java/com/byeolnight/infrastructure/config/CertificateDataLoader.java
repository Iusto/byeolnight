package com.byeolnight.infrastructure.config;

import com.byeolnight.domain.entity.certificate.Certificate;
import com.byeolnight.domain.entity.certificate.UserCertificate;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.certificate.UserCertificateRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Order(3) // 사용자 데이터 로드 후 실행
@RequiredArgsConstructor
public class CertificateDataLoader implements CommandLineRunner {

    private final UserCertificateRepository userCertificateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        long certificateCount = userCertificateRepository.count();
        log.info("인증서 데이터 현황: {} 개", certificateCount);
        
        // 관리자 계정에 관리자 훈장 발급
        issueAdminMedals();
        
        log.info("인증서 시스템 초기화 완료!");
    }

    private void issueAdminMedals() {
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        
        for (User admin : admins) {
            if (!userCertificateRepository.existsByUserAndCertificateType(admin, Certificate.CertificateType.ADMIN_MEDAL)) {
                UserCertificate adminMedal = UserCertificate.of(admin, Certificate.CertificateType.ADMIN_MEDAL);
                userCertificateRepository.save(adminMedal);
                log.info("관리자 훈장 발급: {}", admin.getNickname());
            }
        }
    }
}