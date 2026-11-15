package com.example.blog.web;

import com.example.blog.bean.Post;
import com.example.blog.bean.User;
import com.example.blog.dao.PostDao;
import com.example.blog.dao.UserDao;
import com.example.blog.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 个人中心：查看/修改个人资料，列出我的文章。
 *
 * <p>GET 显示页面（资料表单 + 我的文章），POST 提交资料修改。
 * 修改成功后要同步更新 Session 里的 User 对象，
 * 否则页头显示的昵称会一直是旧的（这是很常见的「改了没生效」问题）。</p>
 */
@WebServlet(name = "profileServlet", urlPatterns = "/profile")
public class ProfileServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final UserDao userDao = new UserDao();
    private final PostDao postDao = new PostDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            User me = userDao.findById(currentUserId(req));
            req.setAttribute("profileUser", me);
            req.setAttribute("myPosts", postDao.search(null, null, null, me.getId(), true, 1, 100).getList());
            req.setAttribute("activeNav", "profile");
            render(req, resp, "user/profile");
        } catch (Exception e) {
            throw fail("加载个人中心失败", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String nickname = WebUtil.param(req, "nickname");
        String email = WebUtil.param(req, "email");
        String bio = WebUtil.limit(WebUtil.param(req, "bio"), 255);

        if (nickname == null || nickname.length() > 32) {
            flashError(req, "昵称不能为空，且最多 32 个字");
            redirect(req, resp, "/profile");
            return;
        }
        try {
            userDao.updateProfile(currentUserId(req), nickname, email, bio);
            // 同步 Session 中的对象，页头立刻生效
            HttpSession session = req.getSession();
            User sessionUser = (User) session.getAttribute(com.example.blog.util.Const.SESSION_USER);
            if (sessionUser != null) {
                sessionUser.setNickname(nickname);
                sessionUser.setEmail(email);
                sessionUser.setBio(bio);
            }
            flashSuccess(req, "资料已更新");
            redirect(req, resp, "/profile");
        } catch (Exception e) {
            throw fail("更新资料失败", e);
        }
    }
}