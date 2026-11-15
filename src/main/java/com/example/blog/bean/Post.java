package com.example.blog.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 博客文章实体，对应 posts 表。
 *
 * <p>除表字段外还带几个联表/统计字段：作者昵称、分类名、评论数、标签列表、当前用户是否已点赞。
 * 这些字段不是 posts 表的列，由 DAO 在查询时一次装好，页面直接读取，避免 JSP 里再查数据库。</p>
 */
public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private Integer categoryId;
    private String title;
    private String summary;
    private String content;
    private String cover;
    private int viewCount;
    private int likeCount;
    private int status;
    private Date createdAt;
    private Date updatedAt;

    /** 联表字段：作者信息。 */
    private String authorNickname;
    private String authorUsername;
    private String authorAvatar;
    /** 联表字段：分类名。 */
    private String categoryName;
    /** 统计字段：评论数、标签、当前用户是否点过赞。 */
    private int commentCount;
    private List<Tag> tags = new ArrayList<Tag>();
    private boolean likedByMe;

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

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public void setAuthorNickname(String authorNickname) {
        this.authorNickname = authorNickname;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public boolean isLikedByMe() {
        return likedByMe;
    }

    public void setLikedByMe(boolean likedByMe) {
        this.likedByMe = likedByMe;
    }

    /** 作者显示名，昵称优先。 */
    public String getAuthorDisplayName() {
        return (authorNickname == null || authorNickname.trim().isEmpty()) ? authorUsername : authorNickname;
    }

    /** 作者头像文字（无头像时显示）。 */
    public String getAuthorInitial() {
        String name = getAuthorDisplayName();
        return (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase();
    }

    /** 摘要缺失时从正文截取，保证列表页卡片高度一致。 */
    public String getSummaryOrTrimmed() {
        if (summary != null && !summary.trim().isEmpty()) {
            return summary.trim();
        }
        if (content == null) {
            return "";
        }
        String text = content.replaceAll("\\s+", " ").trim();
        return text.length() <= 90 ? text : text.substring(0, 90) + "…";
    }
}