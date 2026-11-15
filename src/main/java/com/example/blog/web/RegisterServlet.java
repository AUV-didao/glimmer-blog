package com.example.blog.web;

import com.example.blog.bean.User;
import com.example.blog.dao.UserDao;
import com.example.blog.util.PasswordUtil;
import com.example.blog.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 注册：GET 表单，POST 校验 + 入库。
 *
 * <p>服务端校验是必须的（前端 required/maxlength 只是体验，抓包就能绕过）。
 * 这里把校验规则集中成一个方法，返回第一条错误信息，页面显示在表单上方。</p>
 */
@WebServlet(name = "registerServlet", urlPatterns = "/register")
public class RegisterServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    /** 用户名：4-20 位字母、数字、下划线。 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,20}$");
    /** 邮箱：简单校验，不做 RFC 全量匹配。 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (currentUser(req) != null) {
            redirect(req, resp, "/");
            return;
        }
        render(req, resp, "auth/register");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = WebUtil.param(req, "username");
        String nickname = WebUtil.param(req, "nickname");
        String email = WebUtil.param(req, "email");
        String password = WebUtil.param(req, "password");
        String password2 = WebUtil.param(req, "password2");

        // 回填，避免用户每次校验失败都要重敲一遍
        req.setAttribute("username", username);
        req.setAttribute("nickname", nickname);
        req.setAttribute("email", email);

        String error = validate(username, nickname, email, password, password2);
        if (error != null) {
            req.setAttribute("error", error);
            render(req, resp, "auth/register");
            return;
        }

        try {
            if (userDao.existsUsername(username)) {
                req.setAttribute("error", "这个用户名已经有人用了，换一个试试");
                render(req, resp, "auth/register");
                return;
            }
            String salt = PasswordUtil.randomSalt();
            User user = new User();
            user.setUsername(username);
            user.setSalt(salt);
            user.setPassword(PasswordUtil.hash(salt, password));
            user.setNickname(nickname == null ? username : nickname);
            user.setEmail(email);
            user.setBio("这个家伙很懒，还没有写简介～");
            user.setRole("USER");
            userDao.insert(user);

            flashSuccess(req, "注册成功，请登录");
            redirect(req, resp, "/login");
        } catch (Exception e) {
            throw fail("注册时数据库出错", e);
        }
    }

    /** 校验，返回错误信息；全部通过返回 null。 */
    private String validate(String username, String nickname, String email, String password, String password2) {
        if (username == null) {
            return "请填写用户名";
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "用户名需为 4-20 位字母、数字或下划线";
        }
        if (password == null || password.length() < 6) {
            return "密码至少 6 位";
        }
        if (!password.equals(password2)) {
            return "两次输入的密码不一致";
        }
        if (nickname != null && nickname.length() > 32) {
            return "昵称最多 32 个字";
        }
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            return "邮箱格式不正确";
        }
        return null;
    }
}