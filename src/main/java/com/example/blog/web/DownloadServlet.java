package com.example.blog.web;

import com.example.blog.bean.Post;
import com.example.blog.bean.Tag;
import com.example.blog.dao.PostDao;
import com.example.blog.bean.User;
import com.example.blog.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

/**
 * 文件下载，支持两种资源：
 *
 * <ol>
 *   <li><b>导出文章</b>：scope=post&id=1，把文章拼成 txt 字节流直接写回浏览器（不落盘）；</li>
 *   <li><b>下载图片</b>：scope=file&path=/upload/cover/x.png，读部署目录里的文件。</li>
 * </ol>
 *
 * <p>两个关键点：</p>
 * <ol>
 *   <li>中文文件名：HTTP 头只能是 ISO-8859-1，必须用 URLEncoder 编码后放进
 *       filename*=UTF-8''... 参数，否则下载下来文件名会变成乱码；</li>
 *   <li>路径穿越：path 参数由用户控制，必须校验它只能落在 upload 目录内部，
 *       否则 ?path=/../../WEB-INF/web.xml 就能把配置文件下走。</li>
 * </ol>
 */
@WebServlet(name = "downloadServlet", urlPatterns = "/download")
public class DownloadServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final PostDao postDao = new PostDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String scope = req.getParameter("scope") == null ? "post" : req.getParameter("scope").trim();
        if ("file".equals(scope)) {
            downloadFile(req, resp);
        } else {
            exportPost(req, resp);
        }
    }

    /** 把一篇文章导出成 txt。 */
    private void exportPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = WebUtil.intParam(req, "id", 0);
        Post post;
        try {
            post = postDao.findById(id);
        } catch (Exception e) {
            throw new IOException("读取文章失败", e);
        }
        if (post == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "文章不存在");
            return;
        }
        User me = currentUser(req);
        boolean canReadDraft = me != null && (me.isAdmin() || me.getId() == post.getUserId());
        if (post.getStatus() != 1 && !canReadDraft) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权导出未发布的草稿");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(post.getTitle()).append("\r\n");
        sb.append("========================================\r\n");
        sb.append("作者：").append(post.getAuthorDisplayName()).append("\r\n");
        sb.append("分类：").append(post.getCategoryName() == null ? "未分类" : post.getCategoryName()).append("\r\n");
        sb.append("标签：");
        for (Tag tag : post.getTags()) {
            sb.append("#").append(tag.getName()).append(' ');
        }
        sb.append("\r\n");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sb.append("发布时间：").append(post.getCreatedAt() == null ? "-" : fmt.format(post.getCreatedAt())).append("\r\n");
        sb.append("浏览：").append(post.getViewCount()).append("　点赞：").append(post.getLikeCount()).append("\r\n");
        sb.append("========================================\r\n\r\n");
        sb.append(post.getContent() == null ? "" : post.getContent());
        sb.append("\r\n\r\n—— 本文由 微光博客 导出 ——\r\n");

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String fileName = post.getTitle() + ".txt";
        resp.setContentType("text/plain;charset=UTF-8");
        setDownloadHeader(resp, fileName);
        resp.setContentLength(bytes.length);
        try (OutputStream out = resp.getOutputStream()) {
            out.write(bytes);
            out.flush();
        }
    }

    /** 下载 upload 目录内的文件（图片等）。 */
    private void downloadFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getParameter("path");
        if (path == null || !path.startsWith("/upload/") || path.contains("..")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "非法的文件路径");
            return;
        }
        String realBase = getServletContext().getRealPath("/upload");
        if (realBase == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "应用未解压部署");
            return;
        }
        File file = new File(getServletContext().getRealPath(path));
        // 再校验一次真实路径确实在 upload 目录下（防止符号链接等绕过）
        if (!file.getCanonicalPath().startsWith(new File(realBase).getCanonicalPath())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "越界访问");
            return;
        }
        if (!file.exists() || file.isDirectory()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }
        resp.setContentType(getServletContext().getMimeType(file.getName()) == null
                ? "application/octet-stream" : getServletContext().getMimeType(file.getName()));
        resp.setContentLengthLong(file.length());
        setDownloadHeader(resp, file.getName());
        try (InputStream in = new FileInputStream(file); OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }
    }

    /** 设置 Content-Disposition，兼容中文文件名。 */
    private void setDownloadHeader(HttpServletResponse resp, String fileName) {
        try {
            String encoded = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        } catch (Exception e) {
            resp.setHeader("Content-Disposition", "attachment");
        }
    }
}