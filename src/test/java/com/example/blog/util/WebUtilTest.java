package com.example.blog.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 请求参数工具单元测试：全是纯函数，直接调。 */
class WebUtilTest {

    @Test
    @DisplayName("extOf：取小写后缀，无后缀或 null 返回空串")
    void extOf() {
        assertEquals(".png", WebUtil.extOf("cover.PNG"));
        assertEquals(".jpeg", WebUtil.extOf("a.b.JPEG"));
        assertEquals("", WebUtil.extOf("noext"));
        assertEquals("", WebUtil.extOf(null));
    }

    @Test
    @DisplayName("limit：超长才截断，空值原样返回")
    void limit() {
        assertEquals("abc", WebUtil.limit("abc", 5));
        assertEquals("ab", WebUtil.limit("abcdef", 2));
        assertNull(WebUtil.limit(null, 5));
    }

    @Test
    @DisplayName("safeRedirect：只放行站内相对路径，挡住开放重定向")
    void safeRedirect() {
        assertEquals("/post?id=1", WebUtil.safeRedirect("/post?id=1", "/"));
        assertEquals("/", WebUtil.safeRedirect("http://evil.com", "/"));
        assertEquals("/", WebUtil.safeRedirect("//evil.com", "/"));
        assertEquals("/", WebUtil.safeRedirect("relative/path", "/"));
        assertEquals("/", WebUtil.safeRedirect(null, "/"));
        assertEquals("/", WebUtil.safeRedirect("  ", "/"));
    }

    @Test
    @DisplayName("safeImageUrl：只放行本站封面和 http/https，拒绝属性注入")
    void safeImageUrl() {
        assertEquals("/upload/cover/a.png", WebUtil.safeImageUrl(" /upload/cover/a.png "));
        assertEquals("https://img.example/a.webp", WebUtil.safeImageUrl("https://img.example/a.webp"));
        assertNull(WebUtil.safeImageUrl("  "));
        assertThrows(IllegalArgumentException.class,
                () -> WebUtil.safeImageUrl("x\" onerror=\"alert(1)"));
        assertThrows(IllegalArgumentException.class,
                () -> WebUtil.safeImageUrl("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class,
                () -> WebUtil.safeImageUrl("/assets/not-cover.png"));
    }
}