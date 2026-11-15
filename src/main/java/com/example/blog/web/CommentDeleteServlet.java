package com.example.blog.web;

import com.example.blog.bean.User;
import com.example.blog.dao.CommentDao;
import com.example.blog.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 删除评论：本人可删自己的，管理员可删任意一条。
 *
 * <p>权限判断写两遍是故意的：DAO 层的 SQL 条件（user_id = ?）是最后一道保险，
 * 这里的 if 是给用户友好提示用的。两者都在，才叫「双重校验」。</p>
 */
@WebServlet(name = "commentDeleteServlet", urlPatterns = "/comment/delete")
public class CommentDeleteServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final CommentDao commentDao = new CommentDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int commentId = WebUtil.intParam(req, "id", 0);
        int postId = WebUtil.intParam(req, "postId", 0);
        User me = currentUser(req);
        try {
            int rows;
            if (me != null && me.isAdmin()) {
                rows = commentDao.deleteByAdmin(commentId);
            } else {
                rows = commentDao.delete(commentId, currentUserId(req));
            }
            if (rows > 0) {
                flashSuccess(req, "评论已删除");
            } else {
                flashError(req, "只能删除自己的评论");
            }
            redirect(req, resp, "/post?id=" + postId + "#comments");
        } catch (Exception e) {
            throw fail("删除评论失败", e);
        }
    }
}