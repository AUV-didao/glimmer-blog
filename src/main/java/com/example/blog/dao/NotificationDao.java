package com.example.blog.dao;

import com.example.blog.bean.Notification;
import com.example.blog.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * notifications 表访问对象。
 *
 * <p>只用得到四种动作：新增一条通知、数未读数、按用户列通知、标记已读。
 * 所有语句都带 user_id 条件，防止「改个 id 就能把别人的通知标成已读」。</p>
 */
public class NotificationDao {

    /** 新增通知，返回主键（0 表示没插入）。 */
    public int insert(int userId, int actorId, String type, int postId, int commentId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            return insert(conn, userId, actorId, type, postId, commentId);
        }
    }

    /**
     * 在调用方给的连接上插入通知（供事务复用，不提交）。
     */
    public int insert(Connection conn, int userId, int actorId, String type,
                      int postId, int commentId) throws SQLException {
        if (userId == actorId) {
            return 0; // 自己操作自己，不给自己发通知
        }
        String sql = "INSERT INTO notifications (user_id, actor_id, type, post_id, comment_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setInt(2, actorId);
            ps.setString(3, type);
            ps.setInt(4, postId);
            ps.setInt(5, commentId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public int countUnread(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** 某人的通知列表：未读在前，同状态按时间倒序。 */
    public List<Notification> listByUser(int userId, int limit) throws SQLException {
        String sql = "SELECT n.*, u.nickname, u.username, u.avatar, p.title AS post_title, "
                + "c.content AS comment_content "
                + "FROM notifications n "
                + "JOIN users u ON u.id = n.actor_id "
                + "JOIN posts p ON p.id = n.post_id "
                + "JOIN comments c ON c.id = n.comment_id "
                + "WHERE n.user_id = ? ORDER BY n.is_read ASC, n.created_at DESC, n.id DESC LIMIT ?";
        List<Notification> list = new ArrayList<Notification>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** 标记一条为已读（带 user_id 条件，只能动自己的）。 */
    public int markRead(int id, int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate();
        }
    }

    /** 全部标为已读。 */
    public int markAllRead(int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate();
        }
    }

    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        n.setActorId(rs.getInt("actor_id"));
        n.setType(rs.getString("type"));
        n.setPostId(rs.getInt("post_id"));
        n.setCommentId(rs.getInt("comment_id"));
        n.setRead(rs.getInt("is_read") == 1);
        n.setCreatedAt(rs.getTimestamp("created_at"));
        n.setActorName(rs.getString("nickname"));
        n.setActorUsername(rs.getString("username"));
        n.setActorAvatar(rs.getString("avatar"));
        n.setPostTitle(rs.getString("post_title"));
        n.setCommentContent(rs.getString("comment_content"));
        return n;
    }
}