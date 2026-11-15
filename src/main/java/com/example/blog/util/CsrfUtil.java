package com.example.blog.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * CSRF 令牌工具（同步器令牌模式 Synchronizer Token Pattern）。
 *
 * <p>要防的攻击：用户登录着本站，被诱导打开一个第三方页面，
 * 那个页面里藏着 {@code <form action="http://本站/post/delete" method="post">}
 * 并自动提交。浏览器会自动带上本站的 Session Cookie，
 * 于是「删除文章」就这么执行了 —— 用户全程不知情。</p>
 *
 * <p>防御思路：凡是会改数据的请求，都必须额外带上一段
 * <b>只有本站页面能拿到</b>的随机串。第三方站点读不到本站的页面内容，
 * 也就拼不出正确令牌。</p>
 *
 * <p>令牌从哪里提交（二者任一，服务端都接受）：</p>
 * <ol>
 *   <li>表单隐藏域 {@code _csrf}（普通表单，报告里一眼能看到实现）；</li>
 *   <li>请求头 {@code X-CSRF-Token}（AJAX / fetch，令牌由页面 meta 标签提供）。</li>
 * </ol>
 *
 * <p>注意：令牌比较用 {@link MessageDigest#isEqual} 做定长时间比较，
 * 避免通过响应时间差逐字节猜令牌。</p>
 */
public final class CsrfUtil {

    /** Session 中保存令牌的 key。 */
    public static final String SESSION_KEY = "csrfToken";
    /** 表单隐藏域名。 */
    public static final String PARAM_NAME = "_csrf";
    /** AJAX 请求头名。 */
    public static final String HEADER_NAME = "X-CSRF-Token";

    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfUtil() {
    }

    /** 确保当前会话有令牌（没有就生成一个），返回令牌。 */
    public static String ensureToken(HttpServletRequest req) {
        String exist = tokenOf(req);
        if (exist != null) {
            return exist;
        }
        HttpSession session = req.getSession();
        String token = newToken();
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    /** 只读地取当前会话令牌，没有返回 null。 */
    public static String tokenOf(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_KEY);
        return value instanceof String && !((String) value).isEmpty() ? (String) value : null;
    }

    /** 校验请求里的令牌是否与会话令牌一致。 */
    public static boolean isValid(HttpServletRequest req) {
        String sessionToken = tokenOf(req);
        if (sessionToken == null) {
            return false;
        }
        String provided = req.getParameter(PARAM_NAME);
        if (isEmpty(provided)) {
            provided = req.getHeader(HEADER_NAME);
        }
        return matches(sessionToken, provided);
    }

    /**
     * 纯函数版本：定长时间比较，便于单元测试。
     */
    public static boolean matches(String sessionToken, String provided) {
        if (isEmpty(sessionToken) || isEmpty(provided)) {
            return false;
        }
        return MessageDigest.isEqual(
                sessionToken.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }

    private static String contextPath(HttpServletRequest req) {
        String ctx = req.getContextPath();
        return (ctx == null || ctx.isEmpty()) ? "/" : ctx;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    /** 32 字节随机数转十六进制，得到 64 个字符的令牌。 */
    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String s = Integer.toHexString(b & 0xFF);
            if (s.length() == 1) {
                sb.append('0');
            }
            sb.append(s);
        }
        return sb.toString();
    }
}