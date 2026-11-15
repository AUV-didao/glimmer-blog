package com.example.blog.service;

import com.example.blog.bean.Comment;
import com.example.blog.bean.Notification;
import com.example.blog.bean.Post;
import com.example.blog.bean.User;
import com.example.blog.dao.NotificationDao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 通知业务规则。
 *
 * <p>为什么单独抽一层 service，而不是写在 Servlet 里？</p>
 * <p>「谁该收到通知」是业务规则，不是数据访问，也不该和 HTTP 绑死：
 * 后台批量导入评论时同样要发通知，如果写在 Servlet 里就复用不了。
 * 这层只依赖 DAO 和 bean，将来换成定时任务/消息队列也只改这一处。</p>
 *
 * <p>规则（去重是关键）：</p>
 * <ol>
 *   <li>回复别人的评论 → 通知被回复者（REPLY）；</li>
 *   <li>任何评论 → 通知文章作者（COMMENT）；</li>
 *   <li>自己评论自己的文章、自己回复自己 —— 不发通知；</li>
 *   <li>「回复的正好是文章作者」时只发一条，避免同一个动作收到两条通知。</li>
 * </ol>
 */
public class NoticeService {

    private final NotificationDao notificationDao = new NotificationDao();

    /**
     * 一条新评论产生的通知，返回实际发出的条数（0/1/2）。
     */
    public int notifyComment(Connection conn, Post post, Comment parent, User actor, int newCommentId)
            throws SQLException {
        if (post == null || actor == null) {
            return 0;
        }
        int created = 0;
        int actorId = actor.getId();

        // 规则 1：回复了某条评论 → 通知被回复者
        if (parent != null && parent.getUserId() != actorId) {
            created += notificationDao.insert(conn, parent.getUserId(), actorId,
                    Notification.TYPE_REPLY, post.getId(), newCommentId);
        }
        // 规则 2 + 4：通知文章作者，但如果作者就是被回复者（已发过）或就是自己，则跳过
        boolean authorAlreadyNotified = parent != null && parent.getUserId() == post.getUserId();
        if (post.getUserId() != actorId && !authorAlreadyNotified) {
            created += notificationDao.insert(conn, post.getUserId(), actorId,
                    Notification.TYPE_COMMENT, post.getId(), newCommentId);
        }
        return created;
    }

    public int unreadCount(int userId) throws SQLException {
        return userId <= 0 ? 0 : notificationDao.countUnread(userId);
    }
}