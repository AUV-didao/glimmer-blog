package com.example.blog.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 一条评论。userNickname / userAvatar 来自联表查询，方便页面直接显示。 */
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int postId;
    private int userId;
    /** 被回复的评论 id，0 表示直接评论文章（也就是一级评论）。 */
    private int parentId;
    /** 页面字段：被回复者的昵称，用于「回复 @某某」的展示。 */
    private String replyToName;
    /** 页面字段：挂在这条评论下面的回复（二级评论）。 */
    private List<Comment> replies = new ArrayList<Comment>();
    private String content;
    private Date createdAt;
    /** 联表字段：评论人昵称、头像、用户名。 */
    private String userNickname;
    private String userAvatar;
    private String username;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public String getReplyToName() {
        return replyToName;
    }

    public void setReplyToName(String replyToName) {
        this.replyToName = replyToName;
    }

    public List<Comment> getReplies() {
        return replies;
    }

    public void setReplies(List<Comment> replies) {
        this.replies = replies;
    }

    /** 内容截断，用于「正在回复」提示里的引用。 */
    public String getExcerpt() {
        if (content == null) {
            return "";
        }
        return content.length() > 30 ? content.substring(0, 30) + "…" : content;
    }

    /** 是不是一级评论。 */
    public boolean isTopLevel() {
        return parentId <= 0;
    }

    public String getDisplayName() {
        return (userNickname == null || userNickname.trim().isEmpty()) ? username : userNickname;
    }

    public String getInitial() {
        String name = getDisplayName();
        return (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase();
    }
}