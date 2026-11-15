package com.example.blog.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 「记住我」令牌的单元测试 —— 纯逻辑，不启动 Tomcat、不连数据库。
 *
 * <p>这块代码安全相关：如果验签能被绕过，任何人都能把令牌里的 userId 改成 1
 * 冒充管理员登录，所以每个防御点都要有对应用例。</p>
 */
class RememberMeUtilTest {

    private static final String SALT = "a1b2c3d4e5f60718";

    @Test
    @DisplayName("正常令牌：签发后能解析并校验出同一个 userId")
    void validToken() {
        String token = RememberMeUtil.buildToken(7, SALT, RememberMeUtil.expireAt(System.currentTimeMillis()));

        assertEquals(7, RememberMeUtil.userIdOf(token));
        assertEquals(7, RememberMeUtil.verify(token, SALT));
    }

    @Test
    @DisplayName("篡改 userId：能解析出来，但验签必须失败（防伪造越权登录）")
    void tamperedUserIdRejected() {
        String token = RememberMeUtil.buildToken(7, SALT, RememberMeUtil.expireAt(System.currentTimeMillis()));
        String hacked = token.replaceFirst("^7\\.", "1.");

        assertEquals(1, RememberMeUtil.userIdOf(hacked));
        assertEquals(0, RememberMeUtil.verify(hacked, SALT));
    }

    @Test
    @DisplayName("用户改了密码（盐变了）：旧令牌立即失效")
    void saltChangedInvalidatesToken() {
        String token = RememberMeUtil.buildToken(7, SALT, RememberMeUtil.expireAt(System.currentTimeMillis()));

        assertNotEquals(0, RememberMeUtil.verify(token, SALT));
        assertEquals(0, RememberMeUtil.verify(token, "ffffffffffffffff"));
    }

    @Test
    @DisplayName("过期令牌：验签失败")
    void expiredRejected() {
        String token = RememberMeUtil.buildToken(7, SALT, System.currentTimeMillis() - 1000L);

        assertEquals(0, RememberMeUtil.verify(token, SALT));
    }

    @Test
    @DisplayName("格式非法（null / 空 / 缺段 / 非数字）一律返回 0，不抛异常")
    void malformedToken() {
        assertEquals(0, RememberMeUtil.userIdOf(null));
        assertEquals(0, RememberMeUtil.userIdOf(""));
        assertEquals(0, RememberMeUtil.userIdOf("abc"));
        assertEquals(0, RememberMeUtil.userIdOf("x.1.2"));
        assertEquals(0, RememberMeUtil.verify("1.2", SALT));
        assertEquals(0, RememberMeUtil.verify(null, SALT));
    }

    @Test
    @DisplayName("同一个用户在有效期内的令牌也必须一致（2 次签发用同一过期时间）")
    void deterministicForSameInput() {
        long expireAt = RememberMeUtil.expireAt(1_700_000_000_000L);

        assertEquals(RememberMeUtil.buildToken(7, SALT, expireAt), RememberMeUtil.buildToken(7, SALT, expireAt));
    }
}