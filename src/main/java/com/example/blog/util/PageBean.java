package com.example.blog.util;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页结果对象。
 *
 * <p>把「当前页数据 + 总数 + 页码信息」装在一个对象里，Servlet 只需 request.setAttribute("page", page)，
 * JSP 里就能同时渲染列表和翻页条。翻页条的所有边界判断（有没有上一页、共几页）都收在这里，
 * 页面里不再写 if 判断，模板干净很多。</p>
 *
 * @param <T> 列表元素类型
 */
public class PageBean<T> {

    /** 当前页码，从 1 开始。 */
    private final int page;
    /** 每页条数。 */
    private final int size;
    /** 记录总数。 */
    private final long total;
    /** 当前页数据。 */
    private final List<T> list;

    public PageBean(int page, int size, long total, List<T> list) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.list = list == null ? Collections.<T>emptyList() : list;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotal() {
        return total;
    }

    public List<T> getList() {
        return list;
    }

    /** 总页数，最少 1 页（空结果也显示第 1/1 页，避免页面出现「第 1/0 页」）。 */
    public int getTotalPages() {
        if (total <= 0) {
            return 1;
        }
        return (int) ((total + size - 1) / size);
    }

    public boolean isHasPrev() {
        return page > 1;
    }

    public boolean isHasNext() {
        return page < getTotalPages();
    }

    public int getPrevPage() {
        return Math.max(1, page - 1);
    }

    public int getNextPage() {
        return Math.min(getTotalPages(), page + 1);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }
}