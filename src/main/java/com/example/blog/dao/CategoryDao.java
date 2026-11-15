package com.example.blog.dao;

import com.example.blog.bean.Category;
import com.example.blog.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** categories 表访问对象。 */
public class CategoryDao {

    /** 全部分类，附带各分类下的文章数（左连接 + 分组统计）。 */
    public List<Category> listAll() throws SQLException {
        String sql = "SELECT c.id, c.name, c.sort_order, COUNT(p.id) AS post_count "
                + "FROM categories c LEFT JOIN posts p ON p.category_id = c.id AND p.status = 1 "
                + "GROUP BY c.id, c.name, c.sort_order "
                + "ORDER BY c.sort_order, c.id";
        List<Category> list = new ArrayList<Category>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category c = new Category();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setSortOrder(rs.getInt("sort_order"));
                c.setPostCount(rs.getInt("post_count"));
                list.add(c);
            }
        }
        return list;
    }

    public Category findById(int id) throws SQLException {
        String sql = "SELECT id, name, sort_order FROM categories WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Category c = new Category();
                    c.setId(rs.getInt("id"));
                    c.setName(rs.getString("name"));
                    c.setSortOrder(rs.getInt("sort_order"));
                    return c;
                }
            }
        }
        return null;
    }
}