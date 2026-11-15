package com.example.blog.web;

import com.example.blog.dao.CommentDao;
import com.example.blog.dao.PostDao;
import com.example.blog.dao.UserDao;
import com.example.blog.util.PageBean;
import com.example.blog.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 后台管理首页（仅 ADMIN 可进，权限由 AdminFilter 统一把关）。
 *
 * <p>这里展示的是一份「站点运营概览」：用户表、全部文章（含草稿）、最新评论。
 * 管理员可以删除任意文章和评论，方便收拾垃圾内容。</p>
 */
@WebServlet(name = "adminServlet", urlPatterns = "/admin")
public class AdminServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final UserDao userDao = new UserDao();
    private final PostDao postDao = new PostDao();
    private final CommentDao commentDao = new CommentDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int pageNo = WebUtil.intParam(req, "page", 1);
        try {
            PageBean<com.example.blog.bean.Post> page =
                    postDao.search(null, null, null, null, true, pageNo, 8);
            req.setAttribute("page", page);
            req.setAttribute("users", userDao.listAll());
            req.setAttribute("latestComments", commentDao.listLatest(8));
            req.setAttribute("activeNav", "admin");
            render(req, resp, "admin/index");
        } catch (Exception e) {
            throw fail("加载后台失败", e);
        }
    }
}