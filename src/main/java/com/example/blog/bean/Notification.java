package com.example.blog.bean;

import java.io.Serializable;
import java.util.Date;

/**
 * 一条站内通知：别人评论了你的文章（COMMENT）或回复了你的评论（REPLY）。
 *
 * <p>actorName / actorAvatar / postTitle / commentContent 是联表查出来的字段，
 * 页面直接用，避免 JSP 里再查一次库。</p>
 */
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_REPLY = "REPLY";

    private int id;
    private int userId;      // 接收者
    private int actorId;     // 触发者
    private String type;
    private int postId;
    private int commentId;
    private boolean isRead;
    private Date createdAt;

    /** 联表字段。 */
    private String actorName;
    private String actorAvatar;
    private String actorUsername;
    private String postTitle;
    private String commentContent;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getActorId() {
        return actorId;
    }

    public void setActorId(int actorId) {
        this.actorId = actorId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getCommentId() {
        return commentId;
    }

    public void setCommentId(int commentId) {
        this.commentId = commentId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        this.isRead = read;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorAvatar() {
        return actorAvatar;
    }

    public void setActorAvatar(String actorAvatar) {
        this.actorAvatar = actorAvatar;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getCommentContent() {
        return commentContent;
    }

    public void setCommentContent(String commentContent) {
        this.commentContent = commentContent;
    }

    /** 通知类型的展示文案。 */
    public String getTypeText() {
        return TYPE_REPLY.equals(type) ? "回复了你的评论" : "评论了你的文章";
    }

    public String getActorDisplayName() {
        return (actorName == null || actorName.trim().isEmpty()) ? actorUsername : actorName;
    }

    public String getActorInitial() {
        String name = getActorDisplayName();
        return (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase();
    }

    /** 评论内容过长时截断，避免通知列表被撑爆。 */
    public String getExcerpt() {
        if (commentContent == null) {
            return "";
        }
        return commentContent.length() > 60 ? commentContent.substring(0, 60) + "…" : commentContent;
    }
}