package com.byeolnight.service.file;

import com.byeolnight.dto.file.UploadedImageResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecureS3ServiceTest {

    @Mock
    private S3Service s3Service;

    private SecureS3Service secureS3Service;

    @BeforeEach
    void setUp() {
        secureS3Service = new SecureS3Service(s3Service);
    }

    @Test
    @DisplayName("이미지 바이트를 서버 검열 업로드 경로로 전달한다")
    void uploadsThroughServerModerationPath() {
        byte[] imageBytes = new byte[]{1, 2, 3};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "night-sky.png",
                "image/png",
                imageBytes
        );
        UploadedImageResponseDto expected = UploadedImageResponseDto.of(
                "https://cdn.example.com/uploads/id.png",
                "uploads/id.png",
                "night-sky.png",
                "image/png"
        );
        when(s3Service.uploadModeratedImage("night-sky.png", imageBytes))
                .thenReturn(expected);

        UploadedImageResponseDto result = secureS3Service.uploadModeratedImage(file);

        assertThat(result).isEqualTo(expected);
        verify(s3Service).uploadModeratedImage("night-sky.png", imageBytes);
    }

    @Test
    @DisplayName("허용하지 않는 파일은 검열이나 S3 업로드 전에 거부한다")
    void rejectsUnsupportedFileBeforeUpload() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.svg",
                "image/svg+xml",
                "<svg/>".getBytes()
        );

        assertThatThrownBy(() -> secureS3Service.uploadModeratedImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원되지 않는 파일 형식");
        verify(s3Service, never()).uploadModeratedImage(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("파일 확장자와 콘텐츠 타입이 다르면 업로드를 거부한다")
    void rejectsMismatchedContentTypeBeforeUpload() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "night-sky.png",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> secureS3Service.uploadModeratedImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("콘텐츠 타입이 일치하지 않습니다");
        verify(s3Service, never()).uploadModeratedImage(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}
