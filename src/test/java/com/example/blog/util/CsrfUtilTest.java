package com.example.blog.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CSRF 令牌比对逻辑的单元测试。
 *
 * <p>只测纯函数 {@link CsrfUtil#matches(String, String)}：
 * 真正取 Session / 读 Cookie 的部分依赖容器，放在 HTTP 回归脚本里验证
 * （见 scripts/test-http.ps1 第 14 组用例）。</p>
 *
 * <p>为什么这些用例重要：一旦比对写错（比如用 contains、忽略大小写、
 * 空值当通过），整个 CSRF 防护就等于没有，而页面看起来一切正常。</p>
 */
class CsrfUtilTest {

    @Test
    @DisplayName("完全相同才算通过")
    void equalTokensPass() {
        assertTrue(CsrfUtil.matches("abc123", "abc123"));
        assertTrue(CsrfUtil.matches("0123456789abcdef", "0123456789abcdef"));
    }

    @Test
    @DisplayName("差一个字符就不通过")
    void differentTokensFail() {
        assertFalse(CsrfUtil.matches("abc123", "abc124"));
        assertFalse(CsrfUtil.matches("abc123", "ABC123"));
        assertFalse(CsrfUtil.matches("abc123", "abc1234"));
    }

    @Test
    @DisplayName("空值 / null 一律不通过（不能因为没传就放行）")
    void emptyNeverPasses() {
        assertFalse(CsrfUtil.matches(null, null));
        assertFalse(CsrfUtil.matches(null, "abc"));
        assertFalse(CsrfUtil.matches("abc", null));
        assertFalse(CsrfUtil.matches("", ""));
        assertFalse(CsrfUtil.matches("abc", ""));
        assertFalse(CsrfUtil.matches("", "abc"));
    }

    @Test
    @DisplayName("前缀相同但长度不同的令牌不通过")
    void prefixIsNotEnough() {
        assertFalse(CsrfUtil.matches("0123456789abcdef", "0123456789abcde"));
        assertFalse(CsrfUtil.matches("0123456789abcde", "0123456789abcdef"));
    }

    @Test
    @DisplayName("常量名与实现约定一致，避免 JSP / 前端写错字段")
    void constantsAreStable() {
        assertTrue("_csrf".equals(CsrfUtil.PARAM_NAME));
        assertTrue("X-CSRF-Token".equals(CsrfUtil.HEADER_NAME));
        assertTrue("csrfToken".equals(CsrfUtil.SESSION_KEY));
    }
}
