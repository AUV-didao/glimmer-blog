package com.example.blog.web;

import com.example.blog.bean.Post;
import com.example.blog.bean.User;
import com.example.blog.bean.Tag;
import com.example.blog.dao.PostDao;
import com.example.blog.util.Const;
import com.example.blog.util.UploadUtil;
import com.example.blog.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 保存文章（新增或修改），同时处理封面图上传。
 *
 * <p>@MultipartConfig 让容器把表单按 multipart/form-data 解析：
 * 文本字段仍然用 request.getParameter 取，文件用 request.getPart 取。</p>
 *
 * <p>校验失败时不能简单重定向（用户写的内容会丢），
 * 这里把已提交的内容回填到 request 里重新渲染编辑页。</p>
 */
@WebServlet(name = "postSaveServlet", urlPatterns = "/post/save")
@MultipartConfig(fileSizeThreshold = 1024 * 512, maxFileSize = Const.MAX_UPLOAD_SIZE, maxRequestSize = 8L * 1024 * 1024)
public class PostSaveServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final PostDao postDao = new PostDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int id = WebUtil.intParam(req, "id", 0);
        String title = WebUtil.param(req, "title");
        String summary = WebUtil.limit(WebUtil.param(req, "summary"), 255);
        String content = WebUtil.param(req, "content");
        String tagText = WebUtil.param(req, "tags");
        int categoryId = WebUtil.intParam(req, "categoryId", 0);
        int status = WebUtil.intParam(req, "status", 1) == 0 ? 0 : 1;

        Post post = new Post();
        post.setId(id);
        post.setUserId(currentUserId(req));
        post.setTitle(title);
        post.setSummary(summary);
        post.setContent(content);
        post.setCategoryId(categoryId > 0 ? Integer.valueOf(categoryId) : null);
        post.setStatus(status);
        post.setTags(parseTags(tagText));

        String error = validate(title, content);
        if (error != null) {
            forwardWithError(req, resp, post, error);
            return;
        }

        try {
            Post old = null;
            boolean admin = false;
            if (id > 0) {
                old = postDao.findById(id);
                User me = currentUser(req);
                admin = me != null && me.isAdmin();
                if (old == null || (old.getUserId() != currentUserId(req) && !admin)) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    req.setAttribute("errorTitle", "无权保存");
                    req.setAttribute("errorDetail", "这篇文章不存在，或者你无权修改。");
                    render(req, resp, "error");
                    return;
                }
            }

            String cover = old == null ? null : old.getCover();
            String requestedCover = WebUtil.safeImageUrl(WebUtil.param(req, "coverUrl"));
            Part coverPart = UploadUtil.imagePart(req, "coverFile");
            String uploaded = UploadUtil.saveImage(getServletContext(), coverPart,
                    "upload/cover", Const.MAX_UPLOAD_SIZE);
            if (uploaded != null) {
                cover = uploaded;
            } else if ("1".equals(WebUtil.param(req, "removeCover"))) {
                cover = null;
            } else if (requestedCover != null) {
                cover = requestedCover;
            }
            post.setCover(cover);

            if (id > 0) {
                int rows = admin ? postDao.updateByAdmin(post) : postDao.update(post);
                if (rows == 0) {
                    req.setAttribute("errorTitle", "保存失败");
                    req.setAttribute("errorDetail", "这篇文章不存在，或者你无权修改。");
                    render(req, resp, "error");
                    return;
                }
                flashSuccess(req, "文章已更新");
                redirect(req, resp, "/post?id=" + id);
            } else {
                int newId = postDao.insert(post);
                flashSuccess(req, status == 1 ? "文章已发布" : "草稿已保存");
                redirect(req, resp, "/post?id=" + newId);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 上传校验失败（后缀不对 / 图片过大被容器拦截），把提示带回表单
            forwardWithError(req, resp, post, e.getMessage());
        } catch (Exception e) {
            throw fail("保存文章失败", e);
        }
    }

    /** 把逗号/空格分隔的标签串转成 Tag 列表（去重、限长、最多 5 个）。 */
    private List<Tag> parseTags(String tagText) {
        List<Tag> tags = new ArrayList<Tag>();
        if (tagText == null) {
            return tags;
        }
        // 支持中英文逗号、顿号、空格分隔
        String[] parts = tagText.split("[,，、\\s]+");
        for (String part : parts) {
            String name = part.trim();
            if (name.isEmpty() || name.length() > 24) {
                continue;
            }
            boolean duplicated = false;
            for (Tag exists : tags) {
                if (exists.getName().equals(name)) {
                    duplicated = true;
                    break;
                }
            }
            if (duplicated) {
                continue;
            }
            Tag tag = new Tag();
            tag.setName(name);
            tags.add(tag);
            if (tags.size() >= 5) {
                break;
            }
        }
        return tags;
    }

    private String validate(String title, String content) {
        if (title == null || title.length() < 2) {
            return "标题至少 2 个字";
        }
        if (title.length() > 120) {
            return "标题最多 120 个字";
        }
        if (content == null || content.length() < 10) {
            return "正文至少 10 个字，写点真东西吧";
        }
        if (content.length() > 60000) {
            return "正文太长了（超过 6 万字）";
        }
        return null;
    }

    private void forwardWithError(HttpServletRequest req, HttpServletResponse resp, Post post, String error)
            throws ServletException, IOException {
        req.setAttribute("error", error);
        req.setAttribute("post", post.getId() > 0 ? post : null);
        req.setAttribute("form", post);
        try {
            req.setAttribute("categories", new com.example.blog.dao.CategoryDao().listAll());
        } catch (Exception ignored) {
            // 分类查不出来不影响显示错误提示
        }
        render(req, resp, "post/edit");
    }
}