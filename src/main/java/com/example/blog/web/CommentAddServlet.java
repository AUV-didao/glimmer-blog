package com.example.blog.web;

import com.example.blog.bean.Comment;
import com.example.blog.bean.Post;
import com.example.blog.bean.User;
import com.example.blog.dao.CommentDao;
import com.example.blog.dao.PostDao;
import com.example.blog.service.CommentService;
import com.example.blog.util.Logs;
import com.example.blog.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * 发表评论 / 回复评论。
 *
 * <p>用 POST + 重定向（PRG 模式，Post-Redirect-Get）：
 * 提交成功后 302 回详情页，用户刷新时浏览器重发的是 GET，
 * 不会出现「刷新一次多一条评论」的重复提交问题。</p>
 *
 * <p>一条评论要过四道关：</p>
 * <ol>
 *   <li>参数校验（非空、长度 ≤ 500）；</li>
 *   <li>文章必须存在；</li>
 *   <li>如果是回复，被回复的评论必须存在且属于同一篇文章
 *       —— 否则可以构造 parentId 把回复挂到别的文章下面；</li>
 *   <li>落库成功后交给 NoticeService 决定通知谁（自己人不通知）。</li>
 * </ol>
 */
@WebServlet(name = "commentAddServlet", urlPatterns = "/comment/add")
public class CommentAddServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logs.get(CommentAddServlet.class);
    private static final int MAX_LENGTH = 500;

    private final CommentDao commentDao = new CommentDao();
    private final PostDao postDao = new PostDao();
    private final CommentService commentService = new CommentService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int postId = parseInt(req.getParameter("postId"));
        int parentId = parseInt(req.getParameter("parentId"));
        String content = WebUtil.param(req, "content");

        if (content == null || content.isEmpty()) {
            flashError(req, "评论不能为空");
            redirect(req, resp, "/post?id=" + postId);
            return;
        }
        if (content.length() > MAX_LENGTH) {
            flashError(req, "评论最多 " + MAX_LENGTH + " 字");
            redirect(req, resp, "/post?id=" + postId);
            return;
        }

        try {
            Post post = postDao.findById(postId);
            if (post == null) {
                flashError(req, "文章不存在，评论未保存");
                redirect(req, resp, "/");
                return;
            }

            Comment parent = null;
            if (parentId > 0) {
                parent = commentDao.findById(parentId);
                if (parent == null || parent.getPostId() != postId) {
                    flashError(req, "要回复的评论不存在");
                    redirect(req, resp, "/post?id=" + postId + "#comments");
                    return;
                }
            }

            User me = currentUser(req);
            // 评论与通知在同一个事务里：要么都写成功，要么都回滚
            int newCommentId = commentService.addComment(post, parent, me, content);
            LOG.info("新增评论：commentId=" + newCommentId + " postId=" + postId
                    + " parentId=" + parentId + " userId=" + me.getId());

            flashSuccess(req, parent == null ? "评论成功" : "回复成功");
            redirect(req, resp, "/post?id=" + postId + "#comments");
        } catch (Exception e) {
            throw fail("保存评论失败", e);
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