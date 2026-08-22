package com.byeolnight.service.post;

import com.byeolnight.dto.file.FileDto;
import com.byeolnight.entity.file.File;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.file.FileRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.file.S3Service;
import com.byeolnight.service.notification.NotificationService;
import com.byeolnight.service.user.PointService;
import com.byeolnight.service.log.DeleteLogService;
import com.byeolnight.entity.log.DeleteLog;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final FileRepository fileRepository;
    private final S3Service s3Service;
    private final CertificateService certificateService;
    private final PointService pointService;
    private final NotificationService notificationService;
    private final DeleteLogService deleteLogService;

    @Transactional
    public Long createPost(PostRequestDto dto, User user) {
        validateAdminCategoryWrite(dto.getCategory(), user);

        // HTML ?뷀떚???붿퐫??泥섎━
        String decodedTitle = HtmlUtils.htmlUnescape(dto.getTitle());
        String decodedContent = HtmlUtils.htmlUnescape(dto.getContent());

        Post post = Post.builder()
                .title(decodedTitle)
                .content(decodedContent)
                .category(dto.getCategory())
                .writer(user)
                .build();

        if (dto.getOriginTopicId() != null) {
            Post originTopic = postRepository.findById(dto.getOriginTopicId())
                    .orElseThrow(() -> new NotFoundException("?먮낯 ?좊줎 二쇱젣瑜?李얠쓣 ???놁뒿?덈떎."));
            if (!originTopic.isDiscussionTopic()) {
                throw new IllegalArgumentException("?좏슚?섏? ?딆? ?좊줎 二쇱젣?낅땲??");
            }
            post.setOriginTopicId(dto.getOriginTopicId());
        }

        postRepository.save(post);

        // ?대?吏 ?뚯씪 泥섎━: PENDING ?곹깭 ?뚯씪??CONFIRMED濡?蹂寃쏀븯嫄곕굹 ?덈줈 ?앹꽦
        dto.getImages().forEach(image -> {
            Optional<File> existingFile = fileRepository.findByS3Key(image.s3Key());
            if (existingFile.isPresent()) {
                // 寃???꾨즺 ?낅줈?????앹꽦??PENDING ?뚯씪??CONFIRMED濡?蹂寃?
                existingFile.get().confirmWithPost(post);
            } else {
                // ?댁쟾 踰꾩쟾 ?명솚?? PENDING ?덉퐫?쒓? ?놁쑝硫??덈줈 ?앹꽦
                File file = File.of(post, image.originalName(), image.s3Key(), image.url());
                fileRepository.save(file);
            }
        });

        certificateService.checkAndIssueCertificates(user, CertificateService.CertificateCheckType.POST_WRITE);

        if (dto.getCategory() == Post.Category.IMAGE) {
            certificateService.checkAndIssueCertificates(user, CertificateService.CertificateCheckType.IMAGE_UPLOAD);
        }

        pointService.awardPostWritePoints(user, post.getId(), dto.getContent());

        // ?ъ씤???ъ꽦 ?몄쬆??泥댄겕
        try {
            certificateService.checkAndIssueCertificates(user, CertificateService.CertificateCheckType.POINT_ACHIEVEMENT);
        } catch (Exception e) {
            log.warn("?ъ씤???몄쬆??諛쒓툒 ?ㅽ뙣: {}", e.getMessage());
        }

        if (dto.getCategory() == Post.Category.NOTICE) {
            try {
                notificationService.notifyNewNotice(post.getId(), dto.getTitle());
            } catch (Exception e) {
                log.warn("怨듭??ы빆 ?뚮┝ ?꾩넚 ?ㅽ뙣: {}", e.getMessage());
            }
        }

        return post.getId();
    }

    @Transactional
    public void updatePost(Long postId, PostRequestDto dto, User user) {
        Post post = getPostOrThrow(postId);

        if (!post.getWriter().equals(user)) {
            throw new IllegalArgumentException("蹂몄씤???묒꽦??湲留??섏젙?????덉뒿?덈떎.");
        }

        validateAdminCategoryWrite(dto.getCategory(), user);

        // HTML ?뷀떚???붿퐫??泥섎━
        String decodedTitle = HtmlUtils.htmlUnescape(dto.getTitle());
        String decodedContent = HtmlUtils.htmlUnescape(dto.getContent());

        post.update(decodedTitle, decodedContent, dto.getCategory());

        // 湲곗〈 ?뚯씪 紐⑸줉 議고쉶
        List<File> oldFiles = fileRepository.findAllByPost(post);
        List<FileDto> images = dto.getImages() != null ? dto.getImages() : List.of();
        Set<String> newImageUrls = images.stream()
                .map(FileDto::url)
                .collect(Collectors.toSet());
        
        // ???댁긽 ?ъ슜?섏? ?딅뒗 ?뚯씪留???젣
        List<File> filesToDelete = oldFiles.stream()
                .filter(file -> !newImageUrls.contains(file.getUrl()))
                .toList();
        
        filesToDelete.forEach(file -> {
            s3Service.deleteObject(file.getS3Key());
            fileRepository.delete(file);
        });
        
        // 湲곗〈 ?뚯씪 URL 紐⑸줉
        Set<String> existingUrls = oldFiles.stream()
                .map(File::getUrl)
                .collect(Collectors.toSet());
        
        // ?덈줈???대?吏 泥섎━: PENDING ?곹깭 ?뚯씪??CONFIRMED濡?蹂寃쏀븯嫄곕굹 ?덈줈 ?앹꽦
        images.stream()
                .filter(image -> !existingUrls.contains(image.url()))
                .forEach(image -> {
                    // s3Key媛 鍮꾩뼱?덉쑝硫??ㅽ궢 (肄섑뀗痢좎뿉??異붿텧???몃?/?덇굅???대?吏)
                    if (image.s3Key() == null || image.s3Key().isBlank()) {
                        return;
                    }
                    Optional<File> existingFile = fileRepository.findByS3Key(image.s3Key());
                    if (existingFile.isPresent()) {
                        // 寃???꾨즺 ?낅줈?????앹꽦??PENDING ?뚯씪??CONFIRMED濡?蹂寃?
                        existingFile.get().confirmWithPost(post);
                    } else {
                        // ?댁쟾 踰꾩쟾 ?명솚?? PENDING ?덉퐫?쒓? ?놁쑝硫??덈줈 ?앹꽦
                        File file = File.of(post, image.originalName(), image.s3Key(), image.url());
                        fileRepository.save(file);
                    }
                });
    }

    @Transactional
    public void deletePost(Long postId, User user) {
        Post post = getPostOrThrow(postId);

        if (!post.getWriter().equals(user)) {
            throw new IllegalArgumentException("蹂몄씤???묒꽦??湲留???젣?????덉뒿?덈떎.");
        }

        validateAdminCategoryWrite(post.getCategory(), user);

        // ??젣 濡쒓렇 湲곕줉
        deleteLogService.logDeletion(
            postId,
            DeleteLog.TargetType.POST,
            DeleteLog.ActionType.SOFT_DELETE,
            user.getId(),
            "?ъ슜????젣",
            post.getTitle() + ": " + post.getContent()
        );

        List<File> files = fileRepository.findAllByPost(post);
        files.forEach(file -> s3Service.deleteObject(file.getS3Key()));
        fileRepository.deleteAllByPost(post);

        post.softDelete();
    }

private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("?대떦 寃뚯떆湲??李얠쓣 ???놁뒿?덈떎."));
    }

private void validateAdminCategoryWrite(Post.Category category, User user) {
        if ((category == Post.Category.NEWS || category == Post.Category.NOTICE)
                && user.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("?대떦 移댄뀒怨좊━??寃뚯떆湲? 愿由ъ옄留??묒꽦?????덉뒿?덈떎.");
        }
    }



}
