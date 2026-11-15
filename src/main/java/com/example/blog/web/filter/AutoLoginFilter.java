package com.example.blog.web.filter;

import com.example.blog.bean.User;
import com.example.blog.dao.UserDao;
import com.example.blog.util.Const;
import com.example.blog.util.Logs;
import com.example.blog.util.RememberMeUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 「记住我」自动登录过滤器。
 *
 * <p>典型场景：上次登录时勾了「记住我」，这次打开浏览器直接访问首页，
 * 服务器端的 Session 早就过期了，但浏览器还带着我们自己写的那张
 * {@code blog_remember} Cookie —— 这里把它读出来、验签、查库，
 * 然后重新建 Session 并放进登录用户，用户就「免登录」回来了。</p>
 *
 * <p>执行顺序在 web.xml 里排在 EncodingFilter 之后、AuthFilter 之前：
 * 必须先自动登录，后面的登录拦截才不会把老用户误判成游客。</p>
 *
 * <p>安全要点：</p>
 * <ol>
 *   <li>令牌带 HMAC 签名，改一个字符就验不过（防止伪造 userId 越权登录）；</li>
 *   <li>签名密钥掺入该用户的密码盐，用户改密码后旧令牌自动失效；</li>
 *   <li>校验通过后立刻 changeSessionId()，避免会话固定攻击；</li>
 *   <li>每次自动登录都换一张新令牌（滑动续期），同时校验通过才会续期。</li>
 * </ol>
 */
public class AutoLoginFilter implements Filter {

    private static final Logger LOG = Logs.get(AutoLoginFilter.class);

    private final UserDao userDao = new UserDao();

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

        if (!logged) {
            Cookie cookie = RememberMeUtil.findCookie(req);
            if (cookie != null) {
                autoLogin(req, resp, cookie.getValue());
            }
        }
        chain.doFilter(request, response);
    }

    /** 校验 Cookie 并重建登录态；失败就清掉这张没用的 Cookie。 */
    private void autoLogin(HttpServletRequest req, HttpServletResponse resp, String token) {
        int userId = RememberMeUtil.userIdOf(token);
        if (userId <= 0) {
            RememberMeUtil.clearCookie(req, resp);
            return;
        }
        try {
            User user = userDao.findById(userId);
            if (user == null) {
                RememberMeUtil.clearCookie(req, resp);
                return;
            }
            String salt = user.getSalt();
            if (RememberMeUtil.verify(token, salt) != userId) {
                RememberMeUtil.clearCookie(req, resp);
                LOG.warning("自动登录失败：令牌签名或有效期校验不通过，userId=" + userId);
                return;
            }

            user.setPassword(null);
            user.setSalt(null);
            HttpSession fresh = req.getSession();
            req.changeSessionId();
            fresh.setAttribute(Const.SESSION_USER, user);

            // 滑动续期：再发一张 7 天有效的新令牌
            RememberMeUtil.writeCookie(req, resp,
                    RememberMeUtil.buildToken(userId, salt, RememberMeUtil.expireAt(System.currentTimeMillis())));
            LOG.info("Cookie 自动登录成功：userId=" + userId + "，username=" + user.getUsername());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Cookie 自动登录异常，已忽略并清除 Cookie", e);
            RememberMeUtil.clearCookie(req, resp);
        }
    }

    @Override
    public void destroy() {
        // 无需释放资源
    }
}