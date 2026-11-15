package com.example.blog.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * JDBC 工具类。
 *
 * <p>职责很小，只做三件事：</p>
 * <ol>
 *   <li>类加载时读 classpath 下的 db.properties（配置与代码分离，换环境不用改代码）；</li>
 *   <li>注册驱动，提供一个获取连接的入口 {@link #getConnection()}；</li>
 *   <li>提供静默关闭结果的工具方法 {@link #close(ResultSet, Statement, Connection)}。</li>
 * </ol>
 *
 * <p>注意：这里没有引入连接池。课程作业规模下，每个 DAO 方法用
 * try-with-resources 开一个连接、用完即关，逻辑最直白，也便于讲解。
 * 若要上生产，把 getConnection() 换成 DataSource.getConnection() 即可，
 * DAO 层代码一行都不用改。</p>
 */
public final class DBUtil {

    private static final String URL;
    private static final String USERNAME;
    private static final String PASSWORD;

    static {
        Properties props = new Properties();
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("找不到 db.properties，请确认它在 resources 目录下");
            }
            props.load(in);
            Class.forName(props.getProperty("jdbc.driver", "org.sqlite.JDBC"));
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError("初始化数据库配置失败：" + e.getMessage());
        }
        URL = resolveUrl(props.getProperty("jdbc.url"));
        USERNAME = props.getProperty("jdbc.username");
        PASSWORD = props.getProperty("jdbc.password");
    }

    private static String resolveUrl(String configuredUrl) {
        String override = System.getProperty("blog.db.path");
        if (override == null || override.trim().isEmpty()) {
            override = System.getenv("BLOG_DB_PATH");
        }
        if (override != null && !override.trim().isEmpty()) {
            return "jdbc:sqlite:" + override.trim().replace('\\', '/');
        }
        return configuredUrl;
    }

    private DBUtil() {
    }

    /** 获取一个数据库连接，调用方负责关闭（推荐 try-with-resources）。 */
    public static Connection getConnection() throws SQLException {
        Connection conn = (USERNAME == null || USERNAME.trim().isEmpty())
                ? DriverManager.getConnection(URL)
                : DriverManager.getConnection(URL, USERNAME, PASSWORD);
        if (URL != null && URL.startsWith("jdbc:sqlite:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
                st.execute("PRAGMA busy_timeout = 10000");
                st.execute("PRAGMA journal_mode = WAL");
            }
        }
        return conn;
    }

    /** 数据库地址（用于启动日志，不含密码）。 */
    public static String getUrlForLog() {
        return URL;
    }

    /** 按 ResultSet → Statement → Connection 的顺序静默关闭。 */
    public static void close(ResultSet rs, Statement st, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
                // 关闭失败不影响业务结果，忽略
            }
        }
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ignored) {
                // 同上
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
                // 同上
            }
        }
    }
}
