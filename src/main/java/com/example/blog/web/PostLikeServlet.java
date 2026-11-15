package com.example.blog.web;

import com.example.blog.dao.PostDao;
import com.example.blog.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 点赞 / 取消点赞（AJAX 接口，返回 JSON）。
 *
 * <p>为什么用 POST 而不是 GET？GET 应当是「只读」的语义，
 * 否则浏览器的预取、爬虫、甚至刷新都会意外改变数据。</p>
 *
 * <p>未登录时请求会被 AuthFilter 拦下并返回 401，前端据此提示「请先登录」。</p>
 */
@WebServlet(name = "postLikeServlet", urlPatterns = "/post/like")
public class PostLikeServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final PostDao postDao = new PostDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int postId = WebUtil.intParam(req, "id", 0);
        if (postId <= 0) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"ok\":false,\"message\":\"文章编号不正确\"}");
            return;
        }
        try {
            boolean liked = postDao.toggleLike(postId, currentUserId(req));
            int count = postDao.findLikeCount(postId);
            resp.getWriter().write("{\"ok\":true,\"liked\":" + liked + ",\"count\":" + count + "}");
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"ok\":false,\"message\":\"文章不存在\"}");
        } catch (Exception e) {
            throw fail("点赞失败", e);
        }
    }
}