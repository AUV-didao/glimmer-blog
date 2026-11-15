package com.example.blog.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 密码工具单元测试。
 *
 * <p>其中 {@link #seedAdminHashIsStable()} 是回归保护：blog.sql 里预置的
 * admin/123456 的哈希是写死的常量，一旦有人改了摘要算法（比如把
 * salt+password 改成 password+salt），种子账号立刻登不进去，这条用例先红。</p>
 */
class PasswordUtilTest {

    private static final String SALT = "a1b2c3d4e5f60718";

    @Test
    @DisplayName("随机盐是 16 位小写十六进制")
    void saltFormat() {
        String salt = PasswordUtil.randomSalt();

        assertEquals(16, salt.length());
        assertTrue(salt.matches("[0-9a-f]{16}"), "盐应该只含 0-9a-f，实际：" + salt);
    }

    @Test
    @DisplayName("两次生成的盐不同（不能是固定值）")
    void saltIsRandom() {
        assertNotEquals(PasswordUtil.randomSalt(), PasswordUtil.randomSalt());
    }

    @Test
    @DisplayName("同一个盐 + 同一个明文 → 同一个哈希（可重复校验）")
    void hashIsStable() {
        assertEquals(PasswordUtil.hash(SALT, "123456"), PasswordUtil.hash(SALT, "123456"));
    }

    @Test
    @DisplayName("不同盐 + 同一个明文 → 不同哈希（防彩虹表）")
    void saltChangesHash() {
        assertNotEquals(PasswordUtil.hash(SALT, "123456"), PasswordUtil.hash("0000000000000000", "123456"));
    }

    @Test
    @DisplayName("matches：密码正确返回 true，错误返回 false")
    void matchesWorks() {
        String hash = PasswordUtil.hash(SALT, "123456");

        assertTrue(PasswordUtil.matches(SALT, "123456", hash));
        assertFalse(PasswordUtil.matches(SALT, "123457", hash));
        assertFalse(PasswordUtil.matches("ffffffffffffffff", "123456", hash));
    }

    @Test
    @DisplayName("回归：blog.sql 里 admin/123456 的哈希必须仍能校验通过")
    void seedAdminHashIsStable() {
        String stored = "85cc99c527a2588b8d387b5a7f75008ac9561738dc9e18d1b9e74699e1718bbf";

        assertEquals(PasswordUtil.hash(SALT, "123456"), stored);
        assertTrue(PasswordUtil.matches(SALT, "123456", stored));
    }
}