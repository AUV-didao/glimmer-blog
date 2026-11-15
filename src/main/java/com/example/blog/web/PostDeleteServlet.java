package com.example.blog.web;

import com.example.blog.dao.PostDao;
import com.example.blog.bean.User;
import com.example.blog.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 删除文章。
 *
 * <p>必须用 POST：删除是有副作用的操作，用 GET 的话任何一张 <img src="/post/delete?id=1">
 * 都能让用户「莫名其妙被删文章」（CSRF 的一种）。</p>
 *
 * <p>普通用户的 SQL 带 user_id 条件；管理员走独立 DAO 方法删除任意文章。</p>
 */
@WebServlet(name = "postDeleteServlet", urlPatterns = "/post/delete")
public class PostDeleteServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final PostDao postDao = new PostDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int id = WebUtil.intParam(req, "id", 0);
        if (id <= 0) {
            flashError(req, "文章编号不正确");
            redirect(req, resp, "/profile?tab=posts");
            return;
        }
        try {
            User me = currentUser(req);
            boolean admin = me != null && me.isAdmin();
            int rows = admin ? postDao.deleteByAdmin(id) : postDao.delete(id, currentUserId(req));
            if (rows > 0) {
                flashSuccess(req, "文章已删除");
            } else {
                flashError(req, "删除失败：文章不存在或无权删除");
            }
            redirect(req, resp, admin ? "/admin" : "/profile?tab=posts");
        } catch (Exception e) {
            throw fail("删除文章失败", e);
        }
    }
}