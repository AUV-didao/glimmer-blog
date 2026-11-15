package com.example.blog.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 密码工具：加盐 + SHA-256。
 *
 * <p>为什么不直接存明文？一次拖库就全泄露。
 * 为什么不用 MD5？MD5 已被证明可构造碰撞，且速度太快，容易被彩虹表/GPU 暴力破解。
 * 这里采用的是课程范围内最易解释的方案：每个用户一个 16 位随机盐，
 * 存 SHA-256(salt + 明文) 的十六进制串。相同密码的两个用户，散列值也不同。</p>
 */
public final class PasswordUtil {

    /** 十六进制字符表。 */
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** 密码学安全的随机源（比 new Random() 适合作盐）。 */
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** 生成 16 位十六进制随机盐，例如 "a1b2c3d4e5f60718"。 */
    public static String randomSalt() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    /** 计算 SHA-256(salt + 明文)，返回 64 位小写十六进制串。 */
    public static String hash(String salt, String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest((salt + rawPassword).getBytes(StandardCharsets.UTF_8));
            return toHex(out);
        } catch (NoSuchAlgorithmException e) {
            // JDK 一定带 SHA-256，属于不可恢复的环境错误
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }

    /** 校验明文密码是否与库里的散列一致（用等长比较，避免时序泄露）。 */
    public static boolean matches(String salt, String rawPassword, String storedHash) {
        if (salt == null || rawPassword == null || storedHash == null) {
            return false;
        }
        return constantTimeEquals(hash(salt, rawPassword), storedHash);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    private static String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            chars[i * 2] = HEX[v >>> 4];
            chars[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(chars);
    }
}
