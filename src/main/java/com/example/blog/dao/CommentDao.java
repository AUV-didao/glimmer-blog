package com.example.blog.dao;

import com.example.blog.bean.Comment;
import com.example.blog.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** comments 表访问对象。 */
public class CommentDao {

    /** 某篇文章的评论，按时间正序（老评论在上，符合阅读习惯）。 */
    public List<Comment> listByPostId(int postId) throws SQLException {
        String sql = "SELECT cm.*, u.nickname, u.username, u.avatar "
                + "FROM comments cm JOIN users u ON u.id = cm.user_id "
                + "WHERE cm.post_id = ? ORDER BY cm.created_at";
        List<Comment> list = new ArrayList<Comment>();
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

    /** 按主键取一条评论（发通知前要拿到「被回复者」是谁）。 */
    public Comment findById(int id) throws SQLException {
        String sql = "SELECT cm.*, u.nickname, u.username, u.avatar "
                + "FROM comments cm JOIN users u ON u.id = cm.user_id WHERE cm.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * 新增评论，返回主键。
     *
     * @param parentId 被回复的评论 id；0 表示一级评论（直接评论文章）
     */
    public int insert(int postId, int userId, int parentId, String content) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            return insert(conn, postId, userId, parentId, content);
        }
    }

    /**
     * 在调用方给的连接上插入评论（供事务复用：评论与通知必须同生共死）。
     * 本方法不提交事务，由调用方统一 commit / rollback。
     */
    public int insert(Connection conn, int postId, int userId, int parentId, String content) throws SQLException {
        String sql = "INSERT INTO comments (post_id, user_id, parent_id, content) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            if (parentId > 0) {
                ps.setInt(3, parentId);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, content);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    /**
     * 删除评论：带上 user_id 条件，只有作者本人能删掉自己的评论。
     * 返回受影响行数，0 表示没有权限或评论不存在。
     */
    public int delete(int commentId, int userId) throws SQLException {
        String sql = "DELETE FROM comments WHERE id = ? AND user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            ps.setInt(2, userId);
            return ps.executeUpdate();
        }
    }

    /** 管理员删除任意评论。 */
    public int deleteByAdmin(int commentId) throws SQLException {
        String sql = "DELETE FROM comments WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            return ps.executeUpdate();
        }
    }

    public int countByPostId(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM comments WHERE post_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** 最新评论（后台首页动态列表）。 */
    public List<Comment> listLatest(int limit) throws SQLException {
        String sql = "SELECT cm.*, u.nickname, u.username, u.avatar, p.title AS post_title "
                + "FROM comments cm JOIN users u ON u.id = cm.user_id JOIN posts p ON p.id = cm.post_id "
                + "ORDER BY cm.created_at DESC LIMIT ?";
        List<Comment> list = new ArrayList<Comment>();
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

    private Comment map(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setId(rs.getInt("id"));
        c.setPostId(rs.getInt("post_id"));
        c.setUserId(rs.getInt("user_id"));
        c.setParentId(rs.getInt("parent_id"));
        c.setContent(rs.getString("content"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUserNickname(rs.getString("nickname"));
        c.setUsername(rs.getString("username"));
        c.setUserAvatar(rs.getString("avatar"));
        return c;
    }
}