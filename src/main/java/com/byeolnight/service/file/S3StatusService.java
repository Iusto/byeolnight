package com.byeolnight.service.file;

import com.byeolnight.dto.file.S3StatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** ??? ?? ???? ???? S3 ????? ?? ??? ????. */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StatusService {

    private final S3Service s3Service;

    public S3StatusDto getS3Status() {
        S3StatusDto.S3StatusDtoBuilder statusBuilder = S3StatusDto.builder();

        try {
            // 기본 설정 정보
            statusBuilder.bucketName(s3Service.getBucketName()).configuredRegion(s3Service.getRegion());

            // 자격 증명 확인
            boolean accessKeyConfigured = s3Service.getAccessKey() != null && !s3Service.getAccessKey().trim().isEmpty();
            boolean secretKeyConfigured = s3Service.getSecretKey() != null && !s3Service.getSecretKey().trim().isEmpty();

            if (!accessKeyConfigured || !secretKeyConfigured) {
                return statusBuilder
                        .connectionStatus(S3StatusDto.ConnectionStatus.ERROR)
                        .bucketExists(false)
                        .regionMatch(false)
                        .error("AWS 자격 증명이 설정되지 않았습니다.")
                        .suggestion("application.yml에서 AWS Access Key와 Secret Key를 확인해주세요.")
                        .build();
            }

            // S3 클라이언트로 실제 연결 테스트
            S3Client s3Client = s3Service.createS3Client();

            // 버킷 존재 여부 확인
            try {
                HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                        .bucket(s3Service.getBucketName())
                        .build();
                s3Client.headBucket(headBucketRequest);

                statusBuilder.connectionStatus(S3StatusDto.ConnectionStatus.SUCCESS).bucketExists(true);

                // 버킷의 실제 리전 확인
                try {
                    GetBucketLocationRequest locationRequest = GetBucketLocationRequest.builder()
                            .bucket(s3Service.getBucketName())
                            .build();
                    GetBucketLocationResponse locationResponse = s3Client.getBucketLocation(locationRequest);

                    String actualRegion = locationResponse.locationConstraintAsString();
                    // us-east-1의 경우 null이 반환될 수 있음
                    if (actualRegion == null || actualRegion.isEmpty()) {
                        actualRegion = "us-east-1";
                    }

                    boolean regionMatch = actualRegion.equals(s3Service.getRegion());
                    statusBuilder.actualRegion(actualRegion).regionMatch(regionMatch);

                    if (!regionMatch) {
                        statusBuilder.warning(String.format("설정된 리전(%s)과 실제 버킷 리전(%s)이 다릅니다.", s3Service.getRegion(), actualRegion))
                                     .suggestion("application.yml의 cloud.aws.region.static 설정을 " + actualRegion + "으로 변경해주세요.");
                    }

                } catch (S3Exception regionError) {
                    if (regionError.statusCode() == 403) {
                        log.info("s3:GetBucketLocation 권한 없음 - 설정된 리전 사용: {}", s3Service.getRegion());
                        statusBuilder.actualRegion("권한 없음 (설정값 사용)")
                                     .regionMatch(true)
                                     .info("리전 조회 권한이 없어 설정된 리전을 사용합니다.");
                    } else {
                        log.warn("버킷 리전 조회 실패: {}", regionError.getMessage());
                        statusBuilder.actualRegion("조회 실패").regionMatch(false).warning("버킷 리전을 확인할 수 없습니다.");
                    }
                } catch (Exception regionError) {
                    log.warn("버킷 리전 조회 실패: {}", regionError.getMessage());
                    statusBuilder.actualRegion("조회 실패").regionMatch(false).warning("버킷 리전을 확인할 수 없습니다.");
                }

            } catch (NoSuchBucketException e) {
                statusBuilder.connectionStatus(S3StatusDto.ConnectionStatus.ERROR)
                             .bucketExists(false)
                             .regionMatch(false)
                             .error("지정된 S3 버킷이 존재하지 않습니다.")
                             .suggestion("AWS 콘솔에서 " + s3Service.getBucketName() + " 버킷을 생성하거나 올바른 버킷명을 설정해주세요.");

            } catch (S3Exception e) {
                statusBuilder.connectionStatus(S3StatusDto.ConnectionStatus.ERROR)
                             .bucketExists(false)
                             .regionMatch(false);

                if (e.statusCode() == 403) {
                    statusBuilder.error("S3 버킷에 대한 접근 권한이 없습니다.")
                                 .suggestion("IAM 정책에서 s3:HeadBucket, s3:GetBucketLocation 권한을 확인해주세요.");
                } else {
                    statusBuilder.error("S3 연결 오류: " + e.getMessage())
                                 .suggestion("AWS 자격 증명과 리전 설정을 확인해주세요.");
                }
            }

        } catch (Exception e) {
            log.error("S3 상태 확인 중 오류 발생", e);
            statusBuilder.connectionStatus(S3StatusDto.ConnectionStatus.ERROR)
                         .bucketExists(false)
                         .regionMatch(false)
                         .error("S3 상태 확인 실패: " + e.getMessage())
                         .suggestion("AWS 설정을 확인하고 네트워크 연결을 점검해주세요.");
        }

        return statusBuilder.build();
    }
}
