package com.example.blog.web.listener;

import com.example.blog.dao.CategoryDao;
import com.example.blog.dao.PostDao;
import com.example.blog.dao.UserDao;
import com.example.blog.util.Const;
import com.example.blog.util.DBUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 应用启动/关闭监听器。
 *
 * <p>做三件事：</p>
 * <ol>
 *   <li>启动时打印数据库地址，并做一次连通性自检 —— 连不上就在日志里说清楚，
 *       而不是等用户点注册才报 500；</li>
 *   <li>把统计数字（用户数、文章数）预热进 ServletContext，首页直接读，省一次查询；</li>
 *   <li>创建上传目录（Tomcat 部署新 war 时 webapp 目录是新的，目录不会自动存在）。</li>
 * </ol>
 */
@WebListener
public class AppInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext ctx = event.getServletContext();
        ctx.setAttribute(Const.CONTEXT_ONLINE, new AtomicInteger(0));
        ctx.setAttribute(Const.CONTEXT_PV, new AtomicLong(0));

        log(ctx, "站点启动：" + Const.SITE_NAME);
        log(ctx, "数据库：" + DBUtil.getUrlForLog());

        try {
            int users = new UserDao().countAll();
            int posts = new PostDao().countPublished();
            int categories = new CategoryDao().listAll().size();
            ctx.setAttribute("statUsers", Integer.valueOf(users));
            ctx.setAttribute("statPosts", Integer.valueOf(posts));
            ctx.setAttribute("statCategories", Integer.valueOf(categories));
            log(ctx, "数据自检通过：用户 " + users + " 人，已发布文章 " + posts + " 篇，分类 " + categories + " 个");
        } catch (Exception e) {
            log(ctx, "数据库连接失败，请检查 db.properties 与 SQLite 数据库文件：" + e.getMessage());
        }

        ensureDirs(ctx, "/upload/avatar", "/upload/cover");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        log(event.getServletContext(), "站点关闭：" + Const.SITE_NAME);
    }

    /** 在应部署目录下创建上传子目录。 */
    private void ensureDirs(ServletContext ctx, String... relativePaths) {
        for (String path : relativePaths) {
            String real = ctx.getRealPath(path);
            if (real == null) {
                log(ctx, "无法解析目录（应用可能未解压部署）：" + path);
                continue;
            }
            File dir = new File(real);
            if (!dir.exists() && dir.mkdirs()) {
                log(ctx, "已创建上传目录：" + real);
            }
        }
    }

    private void log(ServletContext ctx, String message) {
        ctx.log("[微光博客] " + message);
        System.out.println("[微光博客] " + message);
    }
}