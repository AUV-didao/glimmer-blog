package com.example.blog.service;

import com.example.blog.bean.Comment;
import com.example.blog.bean.Post;
import com.example.blog.bean.User;
import com.example.blog.dao.CommentDao;
import com.example.blog.service.NoticeService;
import com.example.blog.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 评论业务：把「写评论」和「发通知」放进同一个数据库事务。
 *
 * <p>为什么需要事务？如果两条语句不在一个事务里：
 * 评论插入成功、通知插入失败，用户看到 500 以为没发出去，
 * 实际评论已经进库了 —— 他再点一次就变成两条重复评论，
 * 被回复的人也永远收不到提醒。</p>
 *
 * <p>事务边界放在 Service 层（而不是 DAO）：DAO 只负责单表 SQL，
 * 「多个表要么都成功要么都失败」是业务语义，属于 Service 的职责。</p>
 */
public class CommentService {

    private final CommentDao commentDao = new CommentDao();
    private final NoticeService noticeService = new NoticeService();

    /**
     * 在一个事务里：插入评论 → 按规则生成通知 → 提交。
     *
     * @param parent 被回复的评论，一级评论传 null
     * @return 新评论主键
     */
    public int addComment(Post post, Comment parent, User actor, String content) throws SQLException {
        Connection conn = DBUtil.getConnection();
        try {
            conn.setAutoCommit(false);
            try {
                int newId = commentDao.insert(conn, post.getId(), actor.getId(),
                        parent == null ? 0 : parent.getId(), content);
                noticeService.notifyComment(conn, post, parent, actor, newId);
                conn.commit();
                return newId;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } finally {
            conn.close();
        }
    }
}