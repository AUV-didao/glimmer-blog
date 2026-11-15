package com.example.blog.web.filter;

import com.example.blog.util.CsrfUtil;
import com.example.blog.util.Logs;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * CSRF 防护过滤器。
 *
 * <p>规则很简单：</p>
 * <ol>
 *   <li>每个页面请求都保证会话里有令牌（页面 meta 标签 + 表单隐藏域负责回传）；</li>
 *   <li>POST / PUT / DELETE / PATCH 一律校验令牌，缺失或不匹配返回 403；</li>
 *   <li>AJAX 请求返回 JSON（前端好处理），普通请求转统一错误页；</li>
 *   <li>GET / HEAD 等安全方法只发令牌，不校验（它们本来就不该改数据）。</li>
 * </ol>
 *
 * <p>顺序：必须排在 EncodingFilter 之后；本项目排在 AuthFilter 之后，
 * 让未登录 AJAX 先得到 401，已登录的写请求再做 CSRF 校验。</p>
 */
public class CsrfFilter implements Filter {

    private static final Logger LOG = Logs.get(CsrfFilter.class);

    /** 安全方法：不修改业务数据，只发令牌不校验。 */
    private static final Set<String> SAFE_METHODS =
            new HashSet<String>(Arrays.asList("GET", "HEAD", "OPTIONS", "TRACE"));

    @Override
    public void init(FilterConfig config) throws ServletException {
        // 无初始化参数
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (isStaticAsset(req)) {
            chain.doFilter(request, response);
            return;
        }

        // 1. 保证会话里有令牌（页面通过 meta / 隐藏域把它带给浏览器）
        CsrfUtil.ensureToken(req);

        // 2. 改数据的方法必须带对令牌
        if (!SAFE_METHODS.contains(req.getMethod().toUpperCase())) {
            if (!CsrfUtil.isValid(req)) {
                LOG.warning("CSRF 校验失败：method=" + req.getMethod()
                        + " uri=" + req.getRequestURI() + " ip=" + req.getRemoteAddr());
                reject(req, resp);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /** 403：AJAX 给 JSON，普通请求给统一错误页。 */
    private void reject(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        if ("XMLHttpRequest".equalsIgnoreCase(req.getHeader("X-Requested-With"))) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"ok\":false,\"message\":\"请求校验失败，请刷新页面后重试\"}");
            return;
        }
        req.setAttribute("errorTitle", "请求被拦截");
        req.setAttribute("errorDetail", "页面可能停留太久或来自第三方站点，刷新后重新提交即可。");
        req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
    }

    private boolean isStaticAsset(HttpServletRequest req) {
        String uri = req.getRequestURI();
        if (uri == null) {
            return false;
        }
        String lower = uri.toLowerCase();
        return lower.contains("/assets/")
                || lower.endsWith(".css") || lower.endsWith(".js")
                || lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                || lower.endsWith(".webp") || lower.endsWith(".ico")
                || lower.endsWith(".svg");
    }

    @Override
    public void destroy() {
        // 无需释放资源
    }
}