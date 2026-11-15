package com.example.blog.web;

import com.example.blog.bean.User;
import com.example.blog.dao.UserDao;
import com.example.blog.util.Const;
import com.example.blog.util.Logs;
import com.example.blog.util.PasswordUtil;
import com.example.blog.util.RememberMeUtil;
import com.example.blog.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * 登录：GET 显示表单，POST 校验账号密码。
 *
 * <p>会话相关的三个关键动作都在这里：</p>
 * <ol>
 *   <li>登录成功 → request.getSession() 拿到会话 → setAttribute 存 User 对象。
 *       从这一刻起，服务器端的 Session 里就有「谁登录了」，后续所有页面都靠它；</li>
 *   <li>changeSessionId() —— 换一个新的 JSESSIONID，防止「会话固定攻击」：
 *       攻击者先给受害者一个已知的 sessionId，受害者登录后攻击者就能复用这个会话；</li>
 *   <li>登录失败也只提示同一句「用户名或密码不正确」，不告诉对方到底是哪一项错了，
 *       避免帮攻击者枚举用户名。</li>
 * </ol>
 */
@WebServlet(name = "loginServlet", urlPatterns = "/login")
public class LoginServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logs.get(LoginServlet.class);

    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (currentUser(req) != null) {
            redirect(req, resp, "/");
            return;
        }
        req.setAttribute("next", WebUtil.param(req, "next", "/"));
        render(req, resp, "auth/login");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = WebUtil.param(req, "username");
        String password = WebUtil.param(req, "password");
        String next = WebUtil.safeRedirect(WebUtil.param(req, "next"), "/");

        if (username == null || password == null) {
            req.setAttribute("error", "用户名和密码都要填");
            req.setAttribute("username", username);
            req.setAttribute("next", next);
            render(req, resp, "auth/login");
            return;
        }

        try {
            User user = userDao.findByUsername(username);
            if (user == null || !PasswordUtil.matches(user.getSalt(), password, user.getPassword())) {
                req.setAttribute("error", "用户名或密码不正确");
                req.setAttribute("username", username);
                req.setAttribute("next", next);
                render(req, resp, "auth/login");
                return;
            }
            // 放进 Session 前清掉敏感字段
            String salt = user.getSalt();
            user.setPassword(null);
            user.setSalt(null);

            HttpSession session = req.getSession();
            req.changeSessionId();
            session.setAttribute(Const.SESSION_USER, user);

            // 勾了「记住我」就额外发一张自己签名的持久化 Cookie，7 天内免登录；没勾就清掉旧令牌
            if ("1".equals(req.getParameter("remember")) && salt != null) {
                RememberMeUtil.writeCookie(req, resp,
                        RememberMeUtil.buildToken(user.getId(), salt, RememberMeUtil.expireAt(System.currentTimeMillis())));
                LOG.info("登录成功并签发记住我 Cookie：username=" + username);
            } else {
                RememberMeUtil.clearCookie(req, resp);
                LOG.info("登录成功：username=" + username);
            }

            flashSuccess(req, "欢迎回来，" + user.getDisplayName());
            redirect(req, resp, next);
        } catch (Exception e) {
            throw fail("登录时数据库出错", e);
        }
    }
}