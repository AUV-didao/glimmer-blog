package com.example.blog.web.filter;

import com.example.blog.bean.User;
import com.example.blog.service.NoticeService;
import com.example.blog.util.Const;
import com.example.blog.util.Logs;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 未读通知数过滤器。
 *
 * <p>页头的铃铛要显示「未读几条」，但每个页面是由不同的 Servlet 转发出来的，
 * 在每个 Servlet 里都查一遍未读数既重复又容易漏 —— 典型的横切关注点，
 * 交给 Filter 统一处理：已登录用户访问页面时把 unreadCount 放进 request，JSP 直接取。</p>
 *
 * <p>细节：静态资源（/assets）不查库；数据库出错时降级为 0，绝不因为「统计个徽标」
 * 把整个页面搞成 500。</p>
 */
public class NoticeBadgeFilter implements Filter {

    private static final Logger LOG = Logs.get(NoticeBadgeFilter.class);

    private final NoticeService noticeService = new NoticeService();

    @Override
    public void init(FilterConfig config) throws ServletException {
        // 无初始化参数
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (!isStatic(req)) {
            HttpSession session = req.getSession(false);
            User me = session == null ? null : (User) session.getAttribute(Const.SESSION_USER);
            if (me != null) {
                try {
                    req.setAttribute("unreadCount", noticeService.unreadCount(me.getId()));
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "统计未读通知数失败，本次按 0 处理", e);
                    req.setAttribute("unreadCount", 0);
                }
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isStatic(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return uri != null && uri.contains("/assets/");
    }

    @Override
    public void destroy() {
        // 无需释放资源
    }
}