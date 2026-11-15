package com.example.blog.web.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * 统一编码过滤器。
 *
 * <p>解决问题：POST 提交的中文在 Servlet 里读出来是乱码。
 * 根因是 request.setCharacterEncoding 必须在第一次读取参数之前调用，
 * 每个 Servlet 各写一遍既啰嗦又容易漏。</p>
 *
 * <p>Filter 的作用位置：请求到达 Servlet 之前先经过这里，统一设置好编码再放行。
 * 注意顺序：response 的编码要在 chain.doFilter 之前设置，否则页面可能已经按错误编码提交。</p>
 */
// 注册在 web.xml（执行顺序由 web.xml 决定）
public class EncodingFilter implements Filter {

    private String encoding = "UTF-8";

    @Override
    public void init(FilterConfig config) throws ServletException {
        String param = config.getInitParameter("encoding");
        if (param != null && !param.trim().isEmpty()) {
            this.encoding = param.trim();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding(encoding);
        response.setCharacterEncoding(encoding);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 无需清理资源
    }
}