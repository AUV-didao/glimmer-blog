package com.example.blog.web.filter;

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
 * 登录拦截过滤器。
 *
 * <p>只拦「必须登录才能做」的地址（发文章、评论、点赞、改资料…），
 * 浏览文章、看详情、搜索都放行。这样游客也能正常阅读，转换率更高。</p>
 *
 * <p>两个细节：</p>
 * <ol>
 *   <li>未登录时记住原始目标地址（next 参数），登录成功后直接跳回去，用户体验好；</li>
 *   <li>点赞等 AJAX 请求不能重定向，否则前端拿到的是一整页 HTML。
 *       这里用 401 状态码 + JSON，让前端自己决定跳登录页。</li>
 * </ol>
 */
// 注册在 web.xml（执行顺序由 web.xml 决定）
public class AuthFilter implements Filter {

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
        boolean logged = session != null && session.getAttribute(Const.SESSION_USER) != null;

        if (logged) {
            chain.doFilter(request, response);
            return;
        }

        if (isAjax(req)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"ok\":false,\"message\":\"请先登录\"}");
            return;
        }

        String target = req.getRequestURI().substring(req.getContextPath().length());
        if ("GET".equalsIgnoreCase(req.getMethod()) && req.getQueryString() != null) {
            target = target + "?" + req.getQueryString();
        }
        resp.sendRedirect(req.getContextPath() + "/login?next=" + java.net.URLEncoder.encode(target, "UTF-8"));
    }

    /** 判断是否 AJAX 请求（前端 fetch 时带了 X-Requested-With 头）。 */
    private boolean isAjax(HttpServletRequest req) {
        return "XMLHttpRequest".equalsIgnoreCase(req.getHeader("X-Requested-With"));
    }

    @Override
    public void destroy() {
        // 无资源需要释放
    }
}