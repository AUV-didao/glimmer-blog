package com.example.blog.dao;

import com.example.blog.bean.Post;
import com.example.blog.bean.Tag;
import com.example.blog.util.DBUtil;
import com.example.blog.util.PageBean;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * posts 表访问对象，是本项目里最复杂的一个 DAO。
 *
 * <p>难点有两个：</p>
 * <ol>
 *   <li><b>列表页要多表联查还要分页</b>：先 COUNT 出总数，再 LIMIT 取当前页。
 *       两个 SQL 的 WHERE 条件必须完全一致，所以条件用 {@link #buildWhere} 统一拼一次，
 *       两处复用，避免「总数算对了但列表里有/多一条」的经典 bug。</li>
 *   <li><b>标签是多对多</b>：列表页若每篇文章单独查一次标签，就是 N+1 查询。
 *       这里先用 {@link TagDao#mapByPostIds} 一次查完，再回填到每个 Post 上。</li>
 * </ol>
 */
public class PostDao {

    /** 列表查询字段：不带 content（正文可能很长，列表页用不上）。 */
    private static final String LIST_COLUMNS =
            "SELECT p.id, p.user_id, p.category_id, p.title, p.summary, p.cover, p.view_count, "
            + "p.like_count, p.status, p.created_at, p.updated_at, "
            + "u.nickname AS author_nickname, u.username AS author_username, u.avatar AS author_avatar, "
            + "c.name AS category_name, "
            + "(SELECT COUNT(*) FROM comments cm WHERE cm.post_id = p.id) AS comment_count "
            + "FROM posts p JOIN users u ON u.id = p.user_id "
            + "LEFT JOIN categories c ON c.id = p.category_id ";

    /** 详情查询字段：带上 content。 */
    private static final String DETAIL_COLUMNS =
            "SELECT p.id, p.user_id, p.category_id, p.title, p.summary, p.content, p.cover, p.view_count, "
            + "p.like_count, p.status, p.created_at, p.updated_at, "
            + "u.nickname AS author_nickname, u.username AS author_username, u.avatar AS author_avatar, "
            + "c.name AS category_name, "
            + "(SELECT COUNT(*) FROM comments cm WHERE cm.post_id = p.id) AS comment_count "
            + "FROM posts p JOIN users u ON u.id = p.user_id "
            + "LEFT JOIN categories c ON c.id = p.category_id ";

    private final TagDao tagDao = new TagDao();

    /**
     * 组合条件分页查询。
     *
     * @param keyword    标题/摘要/正文模糊匹配，可为 null
     * @param categoryId 分类筛选，可为 null
     * @param tagId      标签筛选，可为 null
     * @param userId     只看某人的文章，可为 null
     * @param includeDraft 是否包含草稿（个人主页/后台显示，首页只显示已发布）
     */
    public PageBean<Post> search(String keyword, Integer categoryId, Integer tagId, Integer userId,
                                 boolean includeDraft, int page, int size) throws SQLException {
        List<Object> params = new ArrayList<Object>();
        String where = buildWhere(keyword, categoryId, tagId, userId, includeDraft, params);

        long total;
        String countSql = "SELECT COUNT(*) FROM posts p JOIN users u ON u.id = p.user_id " + where;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                total = rs.next() ? rs.getLong(1) : 0L;
            }
        }

        int totalPages = total <= 0 ? 1 : (int) ((total + size - 1) / size);
        int safePage = Math.min(Math.max(page, 1), totalPages);
        List<Post> list = new ArrayList<Post>();
        if (total > 0) {
            String listSql = LIST_COLUMNS + where + " ORDER BY p.created_at DESC, p.id DESC LIMIT ?, ?";
            List<Object> listParams = new ArrayList<Object>(params);
            listParams.add((safePage - 1) * size);
            listParams.add(size);
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(listSql)) {
                bind(ps, listParams);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs, false));
                    }
                }
            }
            fillTags(list);
        }
        return new PageBean<Post>(safePage, size, total, list);
    }

    /** 拼接 WHERE 子句，同时把参数按顺序塞进 params。 */
    private String buildWhere(String keyword, Integer categoryId, Integer tagId, Integer userId,
                              boolean includeDraft, List<Object> params) {
        StringBuilder sb = new StringBuilder("WHERE 1 = 1 ");
        if (!includeDraft) {
            sb.append("AND p.status = 1 ");
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sb.append("AND (p.title LIKE ? OR p.summary LIKE ? OR p.content LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (categoryId != null && categoryId > 0) {
            sb.append("AND p.category_id = ? ");
            params.add(categoryId);
        }
        if (tagId != null && tagId > 0) {
            sb.append("AND EXISTS (SELECT 1 FROM post_tags pt WHERE pt.post_id = p.id AND pt.tag_id = ?) ");
            params.add(tagId);
        }
        if (userId != null && userId > 0) {
            sb.append("AND p.user_id = ? ");
            params.add(userId);
        }
        return sb.toString();
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    /** 一次查完当前页所有文章的标签并回填。 */
    private void fillTags(List<Post> posts) throws SQLException {
        if (posts == null || posts.isEmpty()) {
            return;
        }
        List<Integer> ids = new ArrayList<Integer>(posts.size());
        for (Post p : posts) {
            ids.add(p.getId());
        }
        Map<Integer, List<Tag>> tagMap = tagDao.mapByPostIds(ids);
        for (Post p : posts) {
            List<Tag> tags = tagMap.get(p.getId());
            if (tags != null) {
                p.setTags(tags);
            }
        }
    }

    /** 按主键查详情，不存在返回 null。 */
    public Post findById(int id) throws SQLException {
        String sql = DETAIL_COLUMNS + "WHERE p.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Post p = map(rs, true);
                    p.setTags(tagDao.listByPostId(id));
                    return p;
                }
            }
        }
        return null;
    }

    /** 浏览量 +1（每次打开详情页调用）。 */
    public void increaseView(int id) throws SQLException {
        String sql = "UPDATE posts SET view_count = view_count + 1 WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * 新增文章，返回主键。
     *
     * <p>文章行与标签关联（post_tags）在<b>同一个连接、同一个事务</b>里写：
     * 任何一步失败都整体回滚，不会出现「文章保存了、标签丢了」的半成品。
     * 提交/回滚后把 autoCommit 复位，避免连接带着脏事务状态被复用。</p>
     */
    public int insert(Post post) throws SQLException {
        String sql = "INSERT INTO posts (user_id, category_id, title, summary, content, cover, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int newId = insertPost(conn, sql, post);
                syncTags(conn, newId, post.getTags());
                conn.commit();
                return newId;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** 单条插入，供事务内部复用（不负责提交）。 */
    private int insertPost(Connection conn, String sql, Post post) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, post.getUserId());
            if (post.getCategoryId() == null || post.getCategoryId() <= 0) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, post.getCategoryId());
            }
            ps.setString(3, post.getTitle());
            ps.setString(4, post.getSummary());
            ps.setString(5, post.getContent());
            ps.setString(6, post.getCover());
            ps.setInt(7, post.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("新增文章后拿不到主键");
                }
                return keys.getInt(1);
            }
        }
    }

    /** 修改文章；普通用户必须是作者，管理员可修改任意文章。 */
    public int update(Post post) throws SQLException {
        return updateInternal(post, false);
    }

    public int updateByAdmin(Post post) throws SQLException {
        return updateInternal(post, true);
    }

    private int updateInternal(Post post, boolean admin) throws SQLException {
        String sql = "UPDATE posts SET category_id = ?, title = ?, summary = ?, content = ?, cover = ?, "
                + "status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?" + (admin ? "" : " AND user_id = ?");
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    if (post.getCategoryId() == null || post.getCategoryId() <= 0) {
                        ps.setNull(1, java.sql.Types.INTEGER);
                    } else {
                        ps.setInt(1, post.getCategoryId());
                    }
                    ps.setString(2, post.getTitle());
                    ps.setString(3, post.getSummary());
                    ps.setString(4, post.getContent());
                    ps.setString(5, post.getCover());
                    ps.setInt(6, post.getStatus());
                    ps.setInt(7, post.getId());
                    if (!admin) {
                        ps.setInt(8, post.getUserId());
                    }
                    rows = ps.executeUpdate();
                }
                if (rows > 0) {
                    syncTags(conn, post.getId(), post.getTags());
                }
                conn.commit();
                return rows;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** 删除文章（只删自己的）。post_tags / comments / post_likes 由外键 ON DELETE CASCADE 级联删除。 */
    public int delete(int id, int userId) throws SQLException {
        String sql = "DELETE FROM posts WHERE id = ? AND user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate();
        }
    }

    /** 管理员删除任意文章。 */
    public int deleteByAdmin(int id) throws SQLException {
        String sql = "DELETE FROM posts WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    /** 同步标签：先清空旧关联，再逐个 ensure + 建立关联。 */
    private void syncTags(Connection conn, int postId, List<Tag> tags) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM post_tags WHERE post_id = ?")) {
            del.setInt(1, postId);
            del.executeUpdate();
        }
        if (tags == null || tags.isEmpty()) {
            return;
        }
        for (Tag tag : tags) {
            String name = tag.getName() == null ? "" : tag.getName().trim();
            if (name.isEmpty()) {
                continue;
            }
            int tagId = findOrCreateTag(conn, name);
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT OR IGNORE INTO post_tags (post_id, tag_id) VALUES (?, ?)")) {
                ins.setInt(1, postId);
                ins.setInt(2, tagId);
                ins.executeUpdate();
            }
        }
    }

    private int findOrCreateTag(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM tags WHERE name = ?")) {
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

    /** 点赞 / 取消点赞；SQLite 单文件写入在 JVM 内串行，并在同一事务内同步计数。 */
    public synchronized boolean toggleLike(int postId, int userId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement lock = conn.prepareStatement(
                        "SELECT id FROM posts WHERE id = ?")) {
                    lock.setInt(1, postId);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("文章不存在");
                        }
                    }
                }

                boolean existed;
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT 1 FROM post_likes WHERE post_id = ? AND user_id = ?")) {
                    check.setInt(1, postId);
                    check.setInt(2, userId);
                    try (ResultSet rs = check.executeQuery()) {
                        existed = rs.next();
                    }
                }

                boolean liked = !existed;
                String sql = liked
                        ? "INSERT INTO post_likes (post_id, user_id) VALUES (?, ?)"
                        : "DELETE FROM post_likes WHERE post_id = ? AND user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, postId);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }
                exec(conn, liked
                        ? "UPDATE posts SET like_count = like_count + 1 WHERE id = ?"
                        : "UPDATE posts SET like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END WHERE id = ?", postId);
                conn.commit();
                return liked;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** 当前用户是否已点赞。 */
    public boolean isLiked(int postId, int userId) throws SQLException {
        if (userId <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM post_likes WHERE post_id = ? AND user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** 当前点赞数（AJAX 返回给前端刷新数字）。 */
    public int findLikeCount(int postId) throws SQLException {
        String sql = "SELECT like_count FROM posts WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void exec(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** 全站已发布文章数。 */
    public int countPublished() throws SQLException {
        return count("SELECT COUNT(*) FROM posts WHERE status = 1");
    }

    /** 某用户的文章数（含草稿）。 */
    public int countByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM posts WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 热门文章（右侧栏「大家都在看」）：按浏览量倒序取前 N 篇。
     * 只查列表字段，不带正文，避免一次把几十 KB 的 content 读到内存里。
     */
    public List<Post> listHot(int limit) throws SQLException {
        String sql = LIST_COLUMNS + "WHERE p.status = 1 ORDER BY p.view_count DESC, p.id DESC LIMIT ?";
        List<Post> list = new ArrayList<Post>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs, false));
                }
            }
        }
        return list;
    }

    private int count(String sql) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Post map(ResultSet rs, boolean withContent) throws SQLException {
        Post p = new Post();
        p.setId(rs.getInt("id"));
        p.setUserId(rs.getInt("user_id"));
        int categoryId = rs.getInt("category_id");
        p.setCategoryId(rs.wasNull() ? null : Integer.valueOf(categoryId));
        p.setTitle(rs.getString("title"));
        p.setSummary(rs.getString("summary"));
        if (withContent) {
            p.setContent(rs.getString("content"));
        }
        p.setCover(rs.getString("cover"));
        p.setViewCount(rs.getInt("view_count"));
        p.setLikeCount(rs.getInt("like_count"));
        p.setStatus(rs.getInt("status"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        p.setAuthorNickname(rs.getString("author_nickname"));
        p.setAuthorUsername(rs.getString("author_username"));
        p.setAuthorAvatar(rs.getString("author_avatar"));
        p.setCategoryName(rs.getString("category_name"));
        p.setCommentCount(rs.getInt("comment_count"));
        return p;
    }
}