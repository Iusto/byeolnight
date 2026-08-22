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
            // 湲곕낯 ?ㅼ젙 ?뺣낫
            statusBuilder.bucketName(s3Service.getBucketName()).configuredRegion(s3Service.getRegion());

            // ?먭꺽 利앸챸 ?뺤씤
            boolean accessKeyConfigured = s3Service.getAccessKey() != null && !s3Service.getAccessKey().trim().isEmpty();
            boolean secretKeyConfigured = s3Service.getSecretKey() != null && !s3Service.getSecretKey().trim().isEmpty();

            if (!accessKeyConfigured || !secretKeyConfigured) {
                return statusBuilder
                        .connectionStatus(S3StatusDto.ConnectionStatus.ERROR)
                        .bucketExists(false)
                        .regionMatch(false)
                        .error("AWS ?먭꺽 利앸챸???ㅼ젙?섏? ?딆븯?듬땲??")
                        .suggestion("application.yml?먯꽌 AWS Access Key? Secret Key瑜??뺤씤?댁＜?몄슂.")
                        .build();
            }

            // S3 ?대씪?댁뼵?몃줈 ?ㅼ젣 ?곌껐 ?뚯뒪??
            S3Client s3Client = s3Service.createS3Client();

            // 踰꾪궥 議댁옱 ?щ? ?뺤씤
            try {
                HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                        .bucket(s3Service.getBucketName())
                        .build();
                s3Client.headBucket(headBucketRequest);

                statusBuilder.connectionStatus(S3StatusDto.ConnectionStatus.SUCCESS).bucketExists(true);

                // 踰꾪궥???ㅼ젣 由ъ쟾 ?뺤씤
                try {
                    GetBucketLocationRequest locationRequest = GetBucketLocationRequest.builder()
                            .bucket(s3Service.getBucketName())
                            .build();
                    GetBucketLocationResponse locationResponse = s3Client.getBucketLocation(locationRequest);

                    String actualRegion = locationResponse.locationConstraintAsString();
                    // us-east-1??寃쎌슦 null??諛섑솚?????덉쓬
                    if (actualRegion == null || actualRegion.isEmpty()) {
                        actualRegion = "us-east-1";
                    }

                    boolean regionMatch = actualRegion.equals(s3Service.getRegion());
                    statusBuilder.actualRegion(actualRegion).regionMatch(regionMatch);

                    if (!regionMatch) {
                        statusBuilder.warning(String.format("?ㅼ젙??由ъ쟾(%s)怨??ㅼ젣 踰꾪궥 由ъ쟾(%s)???ㅻ쫭?덈떎.", s3Service.getRegion(), actualRegion))
                                     .suggestion("application.yml??cloud.aws.region.static ?ㅼ젙??" + actualRegion + "?쇰줈 蹂寃쏀빐二쇱꽭??");
                    }

                } catch (S3Exception regionError) {
                    if (regionError.statusCode() == 403) {
                        log.info("s3:GetBucketLocation 沅뚰븳 ?놁쓬 - ?ㅼ젙??由ъ쟾 ?ъ슜: {}", s3Service.getRegion());
                        statusBuilder.actualRegion("沅뚰븳 ?놁쓬 (?ㅼ젙媛??ъ슜)")
                                     .regionMatch(true)
                                     .info("由ъ쟾 議고쉶 沅뚰븳???놁뼱 ?ㅼ젙??由ъ쟾???ъ슜?⑸땲??");
                    } else {
                        log.warn("踰꾪궥 由ъ쟾 議고쉶 ?ㅽ뙣: {}", regionError.getMessage());
                        statusBuilder.actualRegion("議고쉶 ?ㅽ뙣").regionMatch(false).warning("踰꾪궥 由ъ쟾???뺤씤?????놁뒿?덈떎.");
                    }
                } catch (Exception regionError) {
                    log.warn("踰꾪궥 由ъ쟾 議고쉶 ?ㅽ뙣: {}", regionError.getMessage());
                    statusBuilder.actualRegion("議고쉶 ?ㅽ뙣").regionMatch(false).warning("踰꾪궥 由ъ쟾???뺤씤?????놁뒿?덈떎.");
                }

            } catch (NoSuchBucketException e) {
                statusBuilder.connectionStatus(S3StatusDto.ConnectionStatus.ERROR)
                             .bucketExists(false)
                             .regionMatch(false)
                             .error("吏?뺣맂 S3 踰꾪궥??議댁옱?섏? ?딆뒿?덈떎.")
                             .suggestion("AWS 肄섏넄?먯꽌 " + s3Service.getBucketName() + " 踰꾪궥???앹꽦?섍굅???щ컮瑜?踰꾪궥紐낆쓣 ?ㅼ젙?댁＜?몄슂.");

            } catch (S3Exception e) {
                statusBuilder.connectionStatus(S3StatusDto.ConnectionStatus.ERROR)
                             .bucketExists(false)
                             .regionMatch(false);

                if (e.statusCode() == 403) {
                    statusBuilder.error("S3 踰꾪궥??????묎렐 沅뚰븳???놁뒿?덈떎.")
                                 .suggestion("IAM ?뺤콉?먯꽌 s3:HeadBucket, s3:GetBucketLocation 沅뚰븳???뺤씤?댁＜?몄슂.");
                } else {
                    statusBuilder.error("S3 ?곌껐 ?ㅻ쪟: " + e.getMessage())
                                 .suggestion("AWS ?먭꺽 利앸챸怨?由ъ쟾 ?ㅼ젙???뺤씤?댁＜?몄슂.");
                }
            }

        } catch (Exception e) {
            log.error("S3 ?곹깭 ?뺤씤 以??ㅻ쪟 諛쒖깮", e);
            statusBuilder.connectionStatus(S3StatusDto.ConnectionStatus.ERROR)
                         .bucketExists(false)
                         .regionMatch(false)
                         .error("S3 ?곹깭 ?뺤씤 ?ㅽ뙣: " + e.getMessage())
                         .suggestion("AWS ?ㅼ젙???뺤씤?섍퀬 ?ㅽ듃?뚰겕 ?곌껐???먭??댁＜?몄슂.");
        }

        return statusBuilder.build();
    }
}
