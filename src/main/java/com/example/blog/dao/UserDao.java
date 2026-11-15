package com.example.blog.dao;

import com.example.blog.bean.User;
import com.example.blog.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * users 表的数据库访问对象。
 *
 * <p>DAO 层只负责「和数据库打交道」，不抛业务异常、不碰 request/session。
 * 所有 SQL 都用 PreparedStatement 占位符，杜绝注入。</p>
 */
public class UserDao {

    /** 按用户名查（登录用），找不到返回 null。 */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT u.*, (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id) AS post_count "
                + "FROM users u WHERE u.username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** 按主键查。 */
    public User findById(int id) throws SQLException {
        String sql = "SELECT u.*, (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id) AS post_count "
                + "FROM users u WHERE u.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** 用户名是否已被占用（注册时校验）。 */
    public boolean existsUsername(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** 新增用户，返回自增主键。 */
    public int insert(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, salt, nickname, email, bio, role) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getSalt());
            ps.setString(4, user.getNickname());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getBio());
            ps.setString(7, user.getRole() == null ? "USER" : user.getRole());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return 0;
    }

    /** 更新个人资料（昵称、邮箱、简介）。 */
    public void updateProfile(int id, String nickname, String email, String bio) throws SQLException {
        String sql = "UPDATE users SET nickname = ?, email = ?, bio = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            ps.setString(2, email);
            ps.setString(3, bio);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    /** 更新头像路径。 */
    public void updateAvatar(int id, String avatarPath) throws SQLException {
        String sql = "UPDATE users SET avatar = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, avatarPath);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** 修改密码：同时换新盐，旧散列失效。 */
    public void updatePassword(int id, String salt, String hash) throws SQLException {
        String sql = "UPDATE users SET password = ?, salt = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /** 全部用户（管理员后台用）。 */
    public List<User> listAll() throws SQLException {
        String sql = "SELECT u.*, (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id) AS post_count "
                + "FROM users u ORDER BY u.id";
        List<User> list = new ArrayList<User>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    /** users 表总数（首页统计卡片用）。 */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setSalt(rs.getString("salt"));
        u.setNickname(rs.getString("nickname"));
        u.setEmail(rs.getString("email"));
        u.setAvatar(rs.getString("avatar"));
        u.setBio(rs.getString("bio"));
        u.setRole(rs.getString("role"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        try {
            u.setPostCount(rs.getInt("post_count"));
        } catch (SQLException ignored) {
            // 某些查询不取 post_count，忽略即可
        }
        return u;
    }
}