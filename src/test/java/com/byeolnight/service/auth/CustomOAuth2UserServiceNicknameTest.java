package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceNicknameTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    private Method generateUniqueNicknameMethod;
    private Method normalizeNicknameMethod;

    @BeforeEach
    void setUp() throws Exception {
        // private 메서드에 접근하기 위해 리플렉션 사용
        generateUniqueNicknameMethod = CustomOAuth2UserService.class
                .getDeclaredMethod("generateUniqueNickname", String.class);
        generateUniqueNicknameMethod.setAccessible(true);

        normalizeNicknameMethod = CustomOAuth2UserService.class
                .getDeclaredMethod("normalizeNickname", String.class);
        normalizeNicknameMethod.setAccessible(true);
    }

    @Test
    void testGenerateUniqueNickname_WhenNoDuplicate() throws Exception {
        // Given
        String baseNickname = "asdf";
        when(userRepository.existsByNickname("asdf")).thenReturn(false);

        // When
        String result = (String) generateUniqueNicknameMethod.invoke(customOAuth2UserService, baseNickname);

        // Then
        assertEquals("asdf", result);
    }

    @Test
    void testGenerateUniqueNickname_WhenDuplicate() throws Exception {
        // Given
        String baseNickname = "asdf";
        when(userRepository.existsByNickname("asdf")).thenReturn(true);
        when(userRepository.existsByNickname(anyString())).thenReturn(false); // UUID 기반 닉네임은 중복 없음

        // When
        String result = (String) generateUniqueNicknameMethod.invoke(customOAuth2UserService, baseNickname);

        // Then
        assertNotEquals("asdf", result);
        assertTrue(result.startsWith("asdf"));
        assertEquals(8, result.length()); // "asdf" + 4자리 UUID
    }

    @Test
    void testGenerateUniqueNickname_WhenLongNickname() throws Exception {
        // Given
        String baseNickname = "verylongnicknametest";
        when(userRepository.existsByNickname(anyString())).thenReturn(false);

        // When
        String result = (String) generateUniqueNicknameMethod.invoke(customOAuth2UserService, baseNickname);

        // Then
        assertEquals("verylongn", result); // 8자로 제한됨
    }

    @Test
    void testNormalizeNickname_ShortNickname() throws Exception {
        // Given
        String shortNickname = "a";

        // When
        String result = (String) normalizeNicknameMethod.invoke(customOAuth2UserService, shortNickname);

        // Then
        assertEquals("사용자", result);
    }

    @Test
    void testNormalizeNickname_LongNickname() throws Exception {
        // Given
        String longNickname = "verylongnicknametest";

        // When
        String result = (String) normalizeNicknameMethod.invoke(customOAuth2UserService, longNickname);

        // Then
        assertEquals("verylongn", result); // 8자로 제한
    }

    @Test
    void testNormalizeNickname_NormalNickname() throws Exception {
        // Given
        String normalNickname = "asdf";

        // When
        String result = (String) normalizeNicknameMethod.invoke(customOAuth2UserService, normalNickname);

        // Then
        assertEquals("asdf", result);
    }
}