package com.example.blog.web;

import com.example.blog.bean.Post;
import com.example.blog.bean.User;
import com.example.blog.dao.CategoryDao;
import com.example.blog.util.WebUtil;
import com.example.blog.dao.PostDao;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 发表 / 编辑文章的「表单页」。
 *
 * <p>一个 Servlet 两种模式：不带 id 是新建，带 id 是编辑。
 * 编辑时会校验作者身份——别人的文章不允许编辑（管理员除外）。</p>
 */
@WebServlet(name = "postEditServlet", urlPatterns = "/post/edit")
public class PostEditServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final PostDao postDao = new PostDao();
    private final CategoryDao categoryDao = new CategoryDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int id = WebUtil.intParam(req, "id", 0);
        try {
            Post post = null;
            if (id > 0) {
                post = postDao.findById(id);
                if (post == null) {
                    req.setAttribute("errorTitle", "文章不存在");
                    req.setAttribute("errorDetail", "无法编辑一篇不存在的文章。");
                    render(req, resp, "error");
                    return;
                }
                User me = currentUser(req);
                boolean owner = post.getUserId() == currentUserId(req);
                if (!owner && (me == null || !me.isAdmin())) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    req.setAttribute("errorTitle", "无权编辑");
                    req.setAttribute("errorDetail", "你只能编辑自己发表的文章。");
                    render(req, resp, "error");
                    return;
                }
            }
            req.setAttribute("post", post);
            req.setAttribute("categories", categoryDao.listAll());
            req.setAttribute("activeNav", "write");
            render(req, resp, "post/edit");
        } catch (Exception e) {
            throw fail("打开发文页失败", e);
        }
    }
}