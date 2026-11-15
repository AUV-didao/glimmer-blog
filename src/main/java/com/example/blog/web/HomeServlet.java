package com.example.blog.web;

import com.example.blog.bean.Post;
import com.example.blog.bean.Tag;
import com.example.blog.dao.CategoryDao;
import com.example.blog.dao.PostDao;
import com.example.blog.dao.TagDao;
import com.example.blog.dao.UserDao;
import com.example.blog.util.Const;
import com.example.blog.util.PageBean;
import com.example.blog.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 首页 = 文章列表 + 搜索 + 分类/标签筛选，三个功能共用一套查询。
 *
 * <p>映射了两个路径："" 和 "/home"。"" 表示应用根路径（/blog/），
 * 这样用户打开 /blog/ 就直接进首页，不用输 /blog/home。</p>
 *
 * <p>首页和搜索页其实是同一个 Servlet：有 keyword 就是搜索，有 categoryId 就是分类筛选，
 * 有 tagId 就是按标签筛选，三者可以叠加。查询条件统一交给
 * {@link PostDao#search} 处理，Servlet 只负责把参数收集起来。</p>
 */
@WebServlet(name = "homeServlet", urlPatterns = {"", "/home"})
public class HomeServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final PostDao postDao = new PostDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final TagDao tagDao = new TagDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String keyword = WebUtil.param(req, "keyword");
        Integer categoryId = WebUtil.intParamOrNull(req, "categoryId");
        Integer tagId = WebUtil.intParamOrNull(req, "tagId");
        int pageNo = WebUtil.intParam(req, "page", 1);

        try {
            PageBean<Post> page = postDao.search(keyword, categoryId, tagId, null, false, pageNo, Const.PAGE_SIZE);

            req.setAttribute("page", page);
            req.setAttribute("keyword", keyword);
            req.setAttribute("activeCategoryId", categoryId);
            req.setAttribute("activeTagId", tagId);
            req.setAttribute("categories", categoryDao.listAll());
            req.setAttribute("tags", tagDao.listTop(12));
            req.setAttribute("hotPosts", postDao.listHot(5));

            // 统计数字实时刷新：启动监听器只做「预热」，文章增删后首页数字要跟着变
            getServletContext().setAttribute("statPosts", Integer.valueOf(postDao.countPublished()));
            getServletContext().setAttribute("statUsers", Integer.valueOf(new UserDao().countAll()));

            // 页面标题会随筛选条件变化，方便浏览器标签页区分
            String title = "全部文章";
            if (keyword != null) {
                title = "搜索：" + keyword;
            } else if (categoryId != null) {
                title = categoryNameOf(categoryDao.listAll(), categoryId);
            } else if (tagId != null) {
                title = tagNameOf(tagDao.listTop(50), tagId);
            }
            req.setAttribute("listTitle", title);
            req.setAttribute("activeNav", "home");

            render(req, resp, "home");
        } catch (Exception e) {
            throw fail("加载首页失败", e);
        }
    }

    private String categoryNameOf(List<com.example.blog.bean.Category> categories, Integer id) {
        for (com.example.blog.bean.Category c : categories) {
            if (c.getId() == id.intValue()) {
                return c.getName();
            }
        }
        return "分类文章";
    }

    private String tagNameOf(List<Tag> tags, Integer id) {
        for (Tag t : tags) {
            if (t.getId() == id.intValue()) {
                return "#" + t.getName();
            }
        }
        return "标签文章";
    }
}