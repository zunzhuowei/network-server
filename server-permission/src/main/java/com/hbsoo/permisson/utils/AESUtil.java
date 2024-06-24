package com.hbsoo.permisson.utils;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@ConfigurationProperties(prefix = "hbsoo.server")
public class AESUtil {
    private String ALGORITHM = "AES";
    private int KEY_SIZE = 128;
    private String aesKey = "Su7@yC#kdb%gV@AN"; // 16 bytes key for AES

    public String encrypt(String data) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decrypt(String encryptedData) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }

    public static void main(String[] args) {
        try {
            AESUtil aesUtil = new AESUtil();
            String originalText = "Hello World!";
            String encryptedText = aesUtil.encrypt(originalText);
            String decryptedText = aesUtil.decrypt(encryptedText);

            System.out.println("Original Text : " + originalText);
            System.out.println("Encrypted Text : " + encryptedText);
            System.out.println("Decrypted Text : " + decryptedText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
}