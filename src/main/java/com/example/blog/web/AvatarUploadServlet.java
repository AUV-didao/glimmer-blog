package com.example.blog.web;

import com.example.blog.bean.User;
import com.example.blog.dao.UserDao;
import com.example.blog.util.Const;
import com.example.blog.util.UploadUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.IOException;

/**
 * 上传头像（multipart/form-data）。
 *
 * <p>处理流程：接住文件 → 校验类型与大小 → 用 UUID 重命名存盘 → 把相对路径写进数据库
 * → 同步更新 Session 里的用户对象（否则页头头像不刷新）。</p>
 *
 * <p>数据库里只存路径（/upload/avatar/xxx.png），不存二进制：
 * 图片文件放文件系统，查库时不用把图片一起读出来，性能更好，备份也各自独立。</p>
 */
@WebServlet(name = "avatarUploadServlet", urlPatterns = "/profile/avatar")
@MultipartConfig(fileSizeThreshold = 1024 * 256, maxFileSize = Const.MAX_UPLOAD_SIZE, maxRequestSize = 6L * 1024 * 1024)
public class AvatarUploadServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final UserDao userDao = new UserDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Part part = UploadUtil.imagePart(req, "avatarFile");
            String path = UploadUtil.saveImage(getServletContext(), part, "upload/avatar", Const.MAX_UPLOAD_SIZE);
            if (path == null) {
                flashError(req, "还没有选择图片");
                redirect(req, resp, "/profile");
                return;
            }
            userDao.updateAvatar(currentUserId(req), path);
            HttpSession session = req.getSession();
            User sessionUser = (User) session.getAttribute(Const.SESSION_USER);
            if (sessionUser != null) {
                sessionUser.setAvatar(path);
            }
            flashSuccess(req, "头像已更新");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flashError(req, e.getMessage());
        } catch (Exception e) {
            throw fail("上传头像失败", e);
        }
        redirect(req, resp, "/profile");
    }
}