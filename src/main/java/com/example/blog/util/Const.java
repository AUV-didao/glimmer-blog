package com.example.blog.util;

/**
 * 全局常量。
 *
 * <p>把「魔法字符串」集中在这里：Session 的 key 写错一次排查半天，
 * 用常量后编译期就能发现拼写问题。</p>
 */
public final class Const {

    private Const() {
    }

    /** 站点名（页头、标题统一从这里取）。 */
    public static final String SITE_NAME = "微光博客";
    public static final String SITE_SLOGAN = "技术、生活与兴趣的自留地";

    /** Session 中保存登录用户的 key。 */
    public static final String SESSION_USER = "loginUser";
    /** ServletContext 中保存在线人数的 key。 */
    public static final String CONTEXT_ONLINE = "onlineCount";
    /** ServletContext 中保存站点累计访问量的 key。 */
    public static final String CONTEXT_PV = "pageViews";

    /** 列表页每页条数。 */
    public static final int PAGE_SIZE = 6;
    /** 上传文件允许的图片后缀。 */
    public static final String[] IMAGE_EXT = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    /** 上传大小上限：5MB（与 @MultipartConfig 保持一致）。 */
    public static final long MAX_UPLOAD_SIZE = 5L * 1024 * 1024;

    /** 视图目录（全部 JSP 放在 WEB-INF 下，浏览器无法直接访问，只能通过 Servlet 转发）。 */
    public static final String VIEW_PREFIX = "/WEB-INF/views/";
}