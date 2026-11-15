package com.example.blog.web.listener;

import com.example.blog.util.Const;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 访问量（PV）统计监听器。
 *
 * <p>ServletRequestListener 能感知「每一个请求」的开始和结束，
 * 这里只统计页面请求：把 .css / .js / .png 等静态资源排除掉，
 * 否则刷新一次页面 PV 会涨十几次，数字没有意义。</p>
 *
 * <p>计数器用 AtomicLong：请求是并发的，i++ 不是原子操作，会丢数。</p>
 */
@WebListener
public class PageViewListener implements ServletRequestListener {

    private static final String[] STATIC_SUFFIX =
            {".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".svg", ".woff", ".woff2", ".map"};

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        ServletRequest request = event.getServletRequest();
        if (!(request instanceof HttpServletRequest)) {
            return;
        }
        String uri = ((HttpServletRequest) request).getRequestURI().toLowerCase();
        for (String suffix : STATIC_SUFFIX) {
            if (uri.endsWith(suffix)) {
                return;
            }
        }
        ServletContext ctx = event.getServletContext();
        Object value = ctx.getAttribute(Const.CONTEXT_PV);
        if (value instanceof AtomicLong) {
            ((AtomicLong) value).incrementAndGet();
        }
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        // 在这里可以做「请求耗时统计」，本项目不需要
    }
}