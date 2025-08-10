package com.byeolnight.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * 개인정보 암호화/복호화 유틸리티
 * 
 * 역할:
 * - AES-128 암호화로 전화번호 등 민감정보 보호
 * - 암호화된 데이터의 안전한 저장/조회 지원
 * - SHA-256 해시로 중복 검사용 해시값 생성
 */
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";

    @Value("${app.encryption.secret-key:MySecretKey12345}")
    private String secretKey;

    /**
     * 문자열 암호화
     */
    public String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("암호화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 문자열 복호화
     */
    public String decrypt(String encryptedText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("복호화 중 오류가 발생했습니다.", e);
        }
    }


}