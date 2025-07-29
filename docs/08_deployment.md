# 08. 운영 및 배포 전략

> 개발자 혼자 운영까지 고려한, 실전 대응 중심의 배포 구조

## 🚀 배포 흐름 요약

```plaintext
[GitHub Push] → [GitHub Actions] → [Docker Build] → [EC2 배포] → [Spring 재기동]
```

## ⚙️ 운영환경 구성

| 항목                | 설명                                  |
| ----------------- | ----------------------------------- |
| **서버**            | AWS EC2 t3.medium (2vCPU / 4GB RAM) |
| **OS**            | Amazon Linux 2                      |
| **백엔드**           | Docker + Spring Boot 3 (JAR 실행)     |
| **프론트엔드**         | Vite + React → 정적 빌드 후 Nginx 서빙     |
| **Reverse Proxy** | Nginx + SSL (Let's Encrypt)         |
| **DB**            | RDS MySQL 8.0                       |
| **Cache/Session** | ElastiCache Redis                   |
| **파일 업로드**        | AWS S3 (Presigned URL 방식)           |

## 🔄 무중단 배포 전략

* Blue/Green 까진 적용하지 않지만, 다음 구조로 **서비스 중단 최소화**:

    * 1. docker-compose로 백엔드 + Nginx 분리 운영
    * 2. 배포 후 `health_check_url`로 상태 확인 → 성공 시 `nginx reload`
    * 3. `/health` API 기반 가용 상태 판단

## 🧼 운영 보조 도구

* **CloudWatch**: CPU, Memory 모니터링
* **Crontab**: 정기 이미지 정리 및 백업
* **logrotate**: 로그 파일 주기적 압축/정리
* **Mail Alert**: 500 에러 발생 시 이메일 알림 (Gmail SMTP 활용)

## 📁 운영용 디렉토리 구조 (서버 내)

```
/var/www/byeolnight
├── backend/        # Dockerfile + jar + env
├── frontend/       # 정적 빌드 + nginx.conf
├── nginx/          # SSL, 로그 설정 포함
├── logs/           # 서비스별 로그 저장소
└── deploy/         # 배포 스크립트 (start.sh, stop.sh 등)
```

---

👉 다음 문서: [09. 향후 계획](./09_roadmap.md)
