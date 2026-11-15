package com.example.blog.web;

import com.example.blog.util.RememberMeUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 退出登录。
 *
 * <p>只接受 <b>POST</b>：退出会改状态（销毁会话、清 Cookie），
 * 如果做成 GET 链接，第三方页面只要放一张
 * {@code <img src="http://本站/logout">} 就能把你踢下线，
 * 属于「会改数据却用 GET」的典型问题。页头现在用一个小表单提交。</p>
 *
 * <p>注意 getSession(false)：不要在这里新建 Session 再 invalidate，
 * 那样会白白创建会话，还会让在线人数监听器多算一个人。</p>
 *
 * <p>invalidate() 会清空会话所有属性并让失效的 sessionId 无法再使用，
 * 同时触发 OnlineUserListener 的 sessionDestroyed，在线人数自动减一。</p>
 */
@WebServlet(name = "logoutServlet", urlPatterns = "/logout")
public class LogoutServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        // 同时删掉「记住我」Cookie，否则退出后一刷新又被自动登录回来
        RememberMeUtil.clearCookie(req, resp);

        // 新建一个会话只为放一句提示
        req.getSession().setAttribute("flashType", "success");
        req.getSession().setAttribute("flashMessage", "已退出登录");
        redirect(req, resp, "/");
    }

    /** 误用 GET 访问时不做任何状态变更，直接回首页。 */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        redirect(req, resp, "/");
    }
}