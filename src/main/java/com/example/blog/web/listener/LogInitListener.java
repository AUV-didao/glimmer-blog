package com.example.blog.web.listener;

import com.example.blog.util.Logs;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;
import java.util.logging.Logger;

/**
 * 日志初始化监听器。
 *
 * <p>应用启动时把日志输出接到「控制台 + 文件」，文件放在 Tomcat 的 logs 目录下
 * （catalina.base/logs/blog-0.log），这样即使是 500 错误也能事后翻日志定位，
 * 而不是只能盯着页面发呆。</p>
 *
 * <p>为什么放在 Listener 而不是某个 Servlet 的 init()？
 * Listener 是「应用生命周期」级别的，早于所有 Servlet 初始化，
 * 保证第一条业务日志（AppInitListener 的自检）就已经能落盘。</p>
 */
@WebListener
public class LogInitListener implements ServletContextListener {

    private static final Logger LOG = Logs.get(LogInitListener.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        String base = System.getProperty("catalina.base");
        String dir = (base == null || base.isEmpty())
                ? System.getProperty("java.io.tmpdir") + File.separator + "blog-logs"
                : base + File.separator + "logs";
        Logs.init(dir);
        LOG.info("微光博客启动，日志目录：" + dir);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        LOG.info("微光博客停止");
    }
}