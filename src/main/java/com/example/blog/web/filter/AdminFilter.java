package com.example.blog.web.filter;

import com.example.blog.bean.User;
import com.example.blog.util.Const;

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

/**
 * 管理员权限过滤器。
 *
 * <p>职责：拦住 /admin/* 下的所有请求，只有 role = ADMIN 的用户能进。</p>
 *
 * <p>为什么权限判断要放 Filter 而不是在 Servlet 里写 if？
 * 因为后台以后还会加页面，写 if 就会漏；Filter 一处声明，全路径自动生效，
 * 这就是「横切关注点」交给过滤器的典型场景。</p>
 */
// 注册在 web.xml（执行顺序由 web.xml 决定）
public class AdminFilter implements Filter {

    @Override
    public void init(FilterConfig config) throws ServletException {
        // 无初始化参数
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        User loginUser = session == null ? null : (User) session.getAttribute(Const.SESSION_USER);

        if (loginUser == null) {
            resp.sendRedirect(req.getContextPath() + "/login?next=" + java.net.URLEncoder.encode("/admin", "UTF-8"));
            return;
        }
        if (!loginUser.isAdmin()) {
            // 已登录但不是管理员：返回 403 而不是跳登录页，语义才准确
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().write("<h3>403 无权限访问后台</h3><p><a href=\"" + req.getContextPath()
                    + "/\">返回首页</a></p>");
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 无资源需要释放
    }
}