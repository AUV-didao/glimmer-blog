package com.example.blog.web;

import com.example.blog.bean.User;
import com.example.blog.util.Const;
import com.example.blog.util.Logs;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 所有 Servlet 的父类，抽出三件重复的事。
 *
 * <ol>
 *   <li>转发到 /WEB-INF/views 下的视图（视图放在 WEB-INF 里，浏览器无法直接访问，
 *       只能由 Servlet 转发进来，天然防止「绕过 Servlet 直接打开 JSP」）；</li>
 *   <li>读取当前登录用户；</li>
 *   <li>一次性提示消息（flash）：写入 Session → 重定向 → 下一个页面读一次就删掉。
 *       这样刷新页面提示不会重复出现。</li>
 * </ol>
 */
public abstract class BaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logs.get(BaseServlet.class);

    /** 转发到视图，view 传相对路径，例如 "post/detail"。 */
    protected void render(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.getRequestDispatcher(Const.VIEW_PREFIX + view + ".jsp").forward(req, resp);
    }

    /** 当前登录用户，未登录返回 null。 */
    protected User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (User) session.getAttribute(Const.SESSION_USER);
    }

    /** 当前登录用户 id，未登录返回 0。 */
    protected int currentUserId(HttpServletRequest req) {
        User user = currentUser(req);
        return user == null ? 0 : user.getId();
    }

    /** 写一条成功提示。 */
    protected void flashSuccess(HttpServletRequest req, String message) {
        flash(req, "success", message);
    }

    /** 写一条失败提示。 */
    protected void flashError(HttpServletRequest req, String message) {
        flash(req, "error", message);
    }

    private void flash(HttpServletRequest req, String type, String message) {
        req.getSession().setAttribute("flashType", type);
        req.getSession().setAttribute("flashMessage", message);
    }

    /**
     * 先记日志，再包装成 ServletException 抛出。
     *
     * <p>用法：<code>throw fail("保存文章失败", e);</code></p>
     *
     * <p>为什么不能只 <code>throw new ServletException(e)</code>？
     * 那样异常只出现在容器错误页上，堆栈不一定落盘、也没有业务上下文
     * （谁、哪个操作、哪篇文章）。这里统一先写一条 SEVERE 日志，
     * 出错后能直接 grep 日志定位。</p>
     */
    protected ServletException fail(String message, Throwable cause) {
        LOG.log(Level.SEVERE, "[" + getClass().getSimpleName() + "] " + message, cause);
        return new ServletException(message, cause);
    }

    /** 重定向到站内地址（自动补应用上下文）。 */
    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }
}