package com.example.blog.util;

/**
 * 请求参数处理工具。
 *
 * <p>Servlet 里最烦的两件事：①每个参数都要判空、去空格；②Integer.parseInt 会抛异常。
 * 这两个工具方法把这部分代码收起来，Servlet 里只关心业务。</p>
 */
public final class WebUtil {

    private WebUtil() {
    }

    /** 取参数并去首尾空格，null 或空串统一返回 null。 */
    public static String param(javax.servlet.http.HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    /** 取参数，空值返回默认值。 */
    public static String param(javax.servlet.http.HttpServletRequest req, String name, String defaultValue) {
        String value = param(req, name);
        return value == null ? defaultValue : value;
    }

    /** 取整型参数，非法或缺失返回默认值（不抛异常）。 */
    public static int intParam(javax.servlet.http.HttpServletRequest req, String name, int defaultValue) {
        String value = param(req, name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 取可空整型参数（如分类筛选，不传就是 null）。 */
    public static Integer intParamOrNull(javax.servlet.http.HttpServletRequest req, String name) {
        int value = intParam(req, name, 0);
        return value > 0 ? Integer.valueOf(value) : null;
    }

    /** 截断字符串，防止超长内容写库时被数据库报错截断。 */
    public static String limit(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    /** 安全的下一跳地址：只允许站内相对路径，防止开放重定向漏洞。 */
    public static String safeRedirect(String target, String fallback) {
        if (target == null || target.isEmpty()) {
            return fallback;
        }
        if (!target.startsWith("/") || target.startsWith("//")) {
            return fallback;
        }
        return target;
    }

    /**
     * 校验并规范化文章封面地址。
     * 只允许本站上传目录或 http/https 绝对地址；拒绝引号、控制字符和其他协议，
     * 避免把用户输入写进 img src 时打断 HTML 属性。
     *
     * @return 规范化后的地址；空值返回 null；非法值抛 IllegalArgumentException
     */
    public static String safeImageUrl(String value) {
        if (value == null) {
            return null;
        }
        String url = value.trim();
        if (url.isEmpty()) {
            return null;
        }
        for (int i = 0; i < url.length(); i++) {
            char ch = url.charAt(i);
            if (ch == '"' || ch == '\'' || ch == '<' || ch == '>' || ch < 0x20 || ch == 0x7F) {
                throw new IllegalArgumentException("封面地址包含非法字符");
            }
        }
        String lower = url.toLowerCase(java.util.Locale.ROOT);
        if (url.startsWith("/upload/cover/") || lower.startsWith("http://") || lower.startsWith("https://")) {
            return url;
        }
        throw new IllegalArgumentException("封面地址只支持 http/https 或本站上传图片");
    }

    /** 从文件名里取出小写后缀（含点），没有后缀返回空串。 */
    public static String extOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot).toLowerCase();
    }
}