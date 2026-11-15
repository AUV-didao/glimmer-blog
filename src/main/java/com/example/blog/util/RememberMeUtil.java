package com.example.blog.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 「记住我」Cookie 的签发与校验 —— 这是本项目里 <b>自己读写 Cookie</b> 的地方。
 *
 * <p>和 JSESSIONID 的区别：JSESSIONID 是容器自动发的、我们只能被动接受；
 * 这里的 Cookie 是我们自己 new 出来、自己写进响应、下次请求自己从
 * {@code request.getCookies()} 里翻出来的，属于「显式使用 Cookie」。</p>
 *
 * <p>令牌结构：<code>userId.expireAtMillis.hmacHex</code></p>
 * <ul>
 *   <li>userId：谁的令牌；</li>
 *   <li>expireAtMillis：过期时间戳，服务端自己判断，改浏览器时间没用；</li>
 *   <li>hmacHex：对前两段做的 HMAC-SHA256 签名，密钥 = 站点密钥 + 该用户的密码盐，
 *       所以用户改密码后旧令牌自动失效（盐变了，签名对不上）。</li>
 * </ul>
 *
 * <p>安全细节：Cookie 带 HttpOnly（JS 读不到）、Path 限定在应用上下文、
 * 值只用数字 / 点 / 十六进制，不需要 URL 编码。</p>
 */
public final class RememberMeUtil {

    /** Cookie 名。 */
    public static final String COOKIE_NAME = "blog_remember";
    /** 有效期：7 天。 */
    public static final int MAX_AGE = 7 * 24 * 60 * 60;

    private static final String SITE_SECRET = "weiguang-blog-remember-me-2024";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private RememberMeUtil() {
    }

    /** 计算过期时间戳（当前时间 + 7 天）。 */
    public static long expireAt(long nowMillis) {
        return nowMillis + MAX_AGE * 1000L;
    }

    /** 生成令牌。userSalt 为空时退化为只用站点密钥签名。 */
    public static String buildToken(int userId, String userSalt, long expireAtMillis) {
        String payload = userId + "." + expireAtMillis;
        return payload + "." + sign(payload, userSalt);
    }

    /**
     * 只解析出 userId，<b>不校验签名</b>。
     * 因为校验签名需要该用户的盐（在数据库里），必须先拿 id 查库。
     */
    public static int userIdOf(String token) {
        String[] parts = split(token);
        if (parts == null) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 完整校验：签名正确且没过期才返回 userId，否则返回 0。 */
    public static int verify(String token, String userSalt) {
        String[] parts = split(token);
        if (parts == null) {
            return 0;
        }
        int userId;
        long expireAt;
        try {
            userId = Integer.parseInt(parts[0]);
            expireAt = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return 0;
        }
        if (expireAt < System.currentTimeMillis()) {
            return 0; // 过期
        }
        String expected = sign(parts[0] + "." + parts[1], userSalt);
        // 定长时间比较，避免通过响应时间猜签名
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            return 0;
        }
        return userId;
    }

    /** 写 Cookie（7 天持久化 + HttpOnly + 限定上下文路径）。 */
    public static void writeCookie(HttpServletRequest req, HttpServletResponse resp, String token) {
        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setMaxAge(MAX_AGE);
        cookie.setHttpOnly(true);
        cookie.setPath(contextPath(req));
        resp.addCookie(cookie);
    }

    /** 删 Cookie：把 Max-Age 置 0，浏览器收到后立即丢弃。 */
    public static void clearCookie(HttpServletRequest req, HttpServletResponse resp) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setPath(contextPath(req));
        resp.addCookie(cookie);
    }

    /** 从请求里找出这张 Cookie，没有则 null。 */
    public static Cookie findCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())
                    && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                return cookie;
            }
        }
        return null;
    }

    private static String contextPath(HttpServletRequest req) {
        String ctx = req.getContextPath();
        return (ctx == null || ctx.isEmpty()) ? "/" : ctx;
    }

    private static String[] split(String token) {
        if (token == null) {
            return null;
        }
        String[] parts = token.split("\\.");
        return parts.length == 3 ? parts : null;
    }

    private static String sign(String payload, String userSalt) {
        String key = SITE_SECRET + (userSalt == null ? "" : userSalt);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return hex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("计算 Cookie 签名失败", e);
        }
    }

    private static String hex(byte[] bytes) {
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
