package com.example.blog.web;

import com.example.blog.bean.Comment;
import com.example.blog.bean.Post;
import com.example.blog.dao.CommentDao;
import com.example.blog.dao.PostDao;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章详情页。
 *
 * <p>加载顺序：文章 → 浏览量 +1 → 评论树 → 当前用户是否点过赞。
 *
 * <p>评论树只在内存里组装一次：一次 SQL 取出全部评论，再按 parent_id 挂到
 * 父评论身上（用 Map 索引，避免 O(n²) 的双层遍历）。
 * 这样「一级评论 + N 条回复」只要一次数据库往返，比每条评论再查一次子查询快得多。</p>
 * 浏览量直接在数据库里 UPDATE ... + 1，而不是「先查再写」，
 * 避免两个人同时打开时互相覆盖（这就是「读改写」的经典并发问题）。</p>
 */
@WebServlet(name = "postDetailServlet", urlPatterns = "/post")
public class PostDetailServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final PostDao postDao = new PostDao();
    private final CommentDao commentDao = new CommentDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int id = parseInt(req.getParameter("id"));
        try {
            Post post = postDao.findById(id);
            if (post == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                req.setAttribute("errorTitle", "文章不存在");
                req.setAttribute("errorDetail", "它可能已被作者删除，或者链接里的编号不对。");
                render(req, resp, "error");
                return;
            }
            // 草稿只有作者本人和管理员能看到
            int currentId = currentUserId(req);
            com.example.blog.bean.User me = currentUser(req);
            boolean privileged = currentId == post.getUserId() || (me != null && me.isAdmin());
            if (post.getStatus() != 1 && !privileged) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                req.setAttribute("errorTitle", "该文章还是草稿");
                req.setAttribute("errorDetail", "只有作者本人登录后才能预览。");
                render(req, resp, "error");
                return;
            }

            postDao.increaseView(id);
            post.setViewCount(post.getViewCount() + 1);

            List<Comment> all = commentDao.listByPostId(id);
            Map<Integer, Comment> byId = new HashMap<Integer, Comment>();
            for (Comment c : all) {
                byId.put(c.getId(), c);
            }
            List<Comment> topComments = new ArrayList<Comment>();
            for (Comment c : all) {
                if (c.getParentId() > 0) {
                    Comment parent = byId.get(c.getParentId());
                    if (parent != null) {
                        c.setReplyToName(parent.getDisplayName());
                        parent.getReplies().add(c);
                        continue;
                    }
                }
                topComments.add(c);
            }
            // 点「回复」时带上 replyTo=<评论 id>，页面据此显示「正在回复 @某某」
            Comment replyTarget = byId.get(parseInt(req.getParameter("replyTo")));
            post.setLikedByMe(currentId > 0 && postDao.isLiked(id, currentId));

            req.setAttribute("post", post);
            req.setAttribute("comments", topComments);
            req.setAttribute("commentCount", all.size());
            req.setAttribute("replyTarget", replyTarget);
            req.setAttribute("canEdit", privileged);
            req.setAttribute("activeNav", "home");
            render(req, resp, "post/detail");
        } catch (Exception e) {
            throw fail("加载文章详情失败", e);
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