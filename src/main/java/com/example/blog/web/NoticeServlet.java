package com.example.blog.web;

import com.example.blog.bean.Notification;
import com.example.blog.bean.User;
import com.example.blog.dao.NotificationDao;
import com.example.blog.service.NoticeService;
import com.example.blog.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 通知中心。
 *
 * <ul>
 *   <li>GET  /notice       —— 我的通知列表（未读在前）</li>
 *   <li>POST /notice/read  —— 标记已读，id 有值时标一条，all=1 时全部标记</li>
 * </ul>
 *
 * <p>标记已读必须用 POST：如果做成 GET 链接，别人把图片地址写成
 * {@code /notice/read?all=1} 就能让你「看一眼页面」被动清空所有未读。</p>
 */
@WebServlet(name = "noticeServlet", urlPatterns = {"/notice", "/notice/read"})
public class NoticeServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private static final int PAGE_LIMIT = 50;

    private final NotificationDao notificationDao = new NotificationDao();
    private final NoticeService noticeService = new NoticeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User me = currentUser(req);
        try {
            List<Notification> notices = notificationDao.listByUser(me.getId(), PAGE_LIMIT);
            req.setAttribute("notices", notices);
            req.setAttribute("unreadCount", noticeService.unreadCount(me.getId()));
            req.setAttribute("activeNav", "notice");
            render(req, resp, "notice");
        } catch (Exception e) {
            throw fail("加载通知列表失败", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User me = currentUser(req);
        try {
            if ("1".equals(WebUtil.param(req, "all"))) {
                int n = notificationDao.markAllRead(me.getId());
                flashSuccess(req, "已把 " + n + " 条通知标为已读");
            } else {
                int id = parseInt(req.getParameter("id"));
                if (id > 0 && notificationDao.markRead(id, me.getId()) > 0) {
                    flashSuccess(req, "已标为已读");
                }
            }
            redirect(req, resp, "/notice");
        } catch (Exception e) {
            throw fail("标记通知已读失败", e);
        }
    }

    private int parseInt(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}