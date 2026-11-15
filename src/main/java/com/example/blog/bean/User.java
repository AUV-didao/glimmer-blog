package com.example.blog.bean;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户实体，与 users 表一一对应。
 *
 * <p>password / salt 只用于登录校验，登录成功后放进 Session 的用户对象会把它们清空，
 * 避免把散列值顺着会话带到页面上（JSP 若误用 EL 输出会造成信息泄露）。</p>
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String password;
    private String salt;
    private String nickname;
    private String email;
    private String avatar;
    private String bio;
    /** USER 或 ADMIN。 */
    private String role;
    private Date createdAt;
    /** 该用户发布的文章数（统计用，不是 users 表字段）。 */
    private int postCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getPostCount() {
        return postCount;
    }

    public void setPostCount(int postCount) {
        this.postCount = postCount;
    }

    /** 是否为管理员，JSP 里可直接 ${sessionScope.loginUser.admin}。 */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /** 昵称为空时退回用户名，页面显示兜底。 */
    public String getDisplayName() {
        return (nickname == null || nickname.trim().isEmpty()) ? username : nickname;
    }

    /** 头像上显示的首字（没有上传头像时用文字头像）。 */
    public String getInitial() {
        String name = getDisplayName();
        return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
    }
}