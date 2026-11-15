package com.example.blog.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * 统一日志入口。
 *
 * <p>用 JDK 自带的 java.util.logging（JUL）：Tomcat 自带、无需额外依赖，
 * 不会像 System.out.println 那样只留在控制台、重启就没了。</p>
 *
 * <p>设计要点：所有业务 Logger 都用 <code>Logs.get(Xxx.class)</code> 取，
 * 名字都挂在 {@code com.example.blog} 这个父 Logger 下面，
 * 于是「加一个文件输出」只需要配置一次父 Logger，全部子 Logger 自动继承，
 * 不用每个类写一遍 Handler。</p>
 */
public final class Logs {

    /** 所有业务 Logger 的公共父节点。 */
    private static final String APP_LOGGER = "com.example.blog";

    private static volatile boolean ready;

    private Logs() {
    }

    /** 取一个挂在 com.example.blog 下的 Logger。 */
    public static Logger get(Class<?> type) {
        return Logger.getLogger(type.getName());
    }

    /**
     * 应用启动时调用一次：给父 Logger 装配「控制台 + 滚动文件」两个 Handler。
     *
     * @param logDir 日志目录，为 null 时只输出到控制台
     */
    public static synchronized void init(String logDir) {
        if (ready) {
            return;
        }
        Logger root = Logger.getLogger(APP_LOGGER);
        root.setUseParentHandlers(false); // 不再重复交给 Tomcat 根 Logger，避免一行日志打两遍
        root.setLevel(Level.INFO);

        ConsoleHandler console = new ConsoleHandler();
        console.setLevel(Level.INFO);
        console.setFormatter(new SimpleFormatter());
        root.addHandler(console);

        if (logDir != null) {
            try {
                File dir = new File(logDir);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new IOException("无法创建日志目录：" + dir);
                }
                // 单个文件 2MB，最多滚动 5 个，追加写；%g 会被替换成 0/1/2…
                FileHandler file = new FileHandler(new File(dir, "blog-%g.log").getPath(),
                        2 * 1024 * 1024, 5, true);
                file.setEncoding("UTF-8");
                file.setLevel(Level.INFO);
                file.setFormatter(new SimpleFormatter());
                root.addHandler(file);
            } catch (IOException e) {
                root.log(Level.WARNING, "日志文件初始化失败，日志只输出到控制台", e);
            }
        }
        ready = true;
    }
}
