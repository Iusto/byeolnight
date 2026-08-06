package com.byeolnight.service.file;

import com.byeolnight.infrastructure.config.SecurityProperties;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.repository.file.FileRepository;
import com.byeolnight.repository.post.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private GoogleVisionService googleVisionService;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private SecurityProperties securityProperties;

    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        s3Service = spy(new S3Service(
                googleVisionService,
                postRepository,
                commentRepository,
                fileRepository,
                securityProperties
        ));
    }

    @Test
    @DisplayName("검열을 통과하지 못한 이미지는 S3 연결 전에 거부한다")
    void rejectsImageBeforeOpeningS3Client() {
        byte[] imageBytes = new byte[]{1, 2, 3};
        when(googleVisionService.isImageSafe(imageBytes)).thenReturn(false);

        assertThatThrownBy(() -> s3Service.uploadModeratedImage("unsafe.png", imageBytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("검열할 수 없는 이미지");

        verify(s3Service, never()).createS3Client();
        verify(fileRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Vision API 검열 오류가 발생하면 안전하다고 간주하지 않고 업로드를 거부한다")
    void rejectsImageWhenModerationFails() {
        byte[] imageBytes = new byte[]{1, 2, 3};
        when(googleVisionService.isImageSafe(imageBytes))
                .thenThrow(new RuntimeException("Vision API 장애"));

        assertThatThrownBy(() -> s3Service.uploadModeratedImage("unknown.png", imageBytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("검열할 수 없는 이미지");

        verify(s3Service, never()).createS3Client();
        verify(fileRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("검열 통과 후에만 S3에 저장하고 공개 URL을 반환한다")
    void storesImageOnlyAfterModerationPasses() {
        byte[] imageBytes = new byte[]{1, 2, 3};
        S3Client s3Client = mock(S3Client.class);
        ReflectionTestUtils.setField(s3Service, "bucketName", "images");
        ReflectionTestUtils.setField(s3Service, "cloudFrontDomain", "cdn.example.com");
        when(googleVisionService.isImageSafe(imageBytes)).thenReturn(true);
        doReturn(s3Client).when(s3Service).createS3Client();

        var result = s3Service.uploadModeratedImage("night.png", imageBytes);

        var ordered = inOrder(googleVisionService, s3Client, fileRepository);
        ordered.verify(googleVisionService).isImageSafe(imageBytes);
        ordered.verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        ordered.verify(fileRepository).saveAndFlush(any());
        assertThat(result.url()).startsWith("https://cdn.example.com/uploads/");
        assertThat(result.contentType()).isEqualTo("image/png");
    }
}
