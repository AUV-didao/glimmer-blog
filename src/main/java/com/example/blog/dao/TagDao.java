package com.example.blog.dao;

import com.example.blog.bean.Tag;
import com.example.blog.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** tags / post_tags 表访问对象。 */
public class TagDao {

    /** 标签云：按使用次数倒序。 */
    public List<Tag> listTop(int limit) throws SQLException {
        String sql = "SELECT t.id, t.name, COUNT(pt.post_id) AS post_count "
                + "FROM tags t LEFT JOIN post_tags pt ON pt.tag_id = t.id "
                + "GROUP BY t.id, t.name ORDER BY post_count DESC, t.id LIMIT ?";
        List<Tag> list = new ArrayList<Tag>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** 某篇文章的所有标签。 */
    public List<Tag> listByPostId(int postId) throws SQLException {
        String sql = "SELECT t.id, t.name FROM tags t JOIN post_tags pt ON pt.tag_id = t.id "
                + "WHERE pt.post_id = ? ORDER BY t.id";
        List<Tag> list = new ArrayList<Tag>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** 批量取多个文章的标签，避免列表页 N+1 查询：一次查出 post_id → 标签列表。 */
    public Map<Integer, List<Tag>> mapByPostIds(List<Integer> postIds) throws SQLException {
        Map<Integer, List<Tag>> result = new LinkedHashMap<Integer, List<Tag>>();
        if (postIds == null || postIds.isEmpty()) {
            return result;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT pt.post_id, t.id, t.name FROM post_tags pt JOIN tags t ON t.id = pt.tag_id WHERE pt.post_id IN (");
        for (int i = 0; i < postIds.size(); i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(") ORDER BY pt.post_id, t.id");
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < postIds.size(); i++) {
                ps.setInt(i + 1, postIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int postId = rs.getInt("post_id");
                    List<Tag> tags = result.get(postId);
                    if (tags == null) {
                        tags = new ArrayList<Tag>();
                        result.put(postId, tags);
                    }
                    tags.add(map(rs));
                }
            }
        }
        return result;
    }

    /** 按名字取标签 id，没有就新建（发表文章时用户直接输入标签名）。 */
    public int ensure(String name) throws SQLException {
        String select = "SELECT id FROM tags WHERE name = ?";
        try (Connection conn = DBUtil.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO tags (name) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        }
    }

    private Tag map(ResultSet rs) throws SQLException {
        Tag t = new Tag();
        t.setId(rs.getInt("id"));
        t.setName(rs.getString("name"));
        try {
            t.setPostCount(rs.getInt("post_count"));
        } catch (SQLException ignored) {
            // 部分查询没有统计列
        }
        return t;
    }
}