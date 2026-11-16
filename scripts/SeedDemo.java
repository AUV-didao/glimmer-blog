import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;

// 在 Tomcat 的 Java 17 环境执行。事务内仅追加示例，已有记录按作者和标题识别。
class SeedDemo {
    record Article(String category, String tag, String title, String opening, String detail) {}

    static final Article[] ARTICLES = {
        new Article("技术分享", "Servlet", "Servlet 请求从哪里开始", "把浏览器的请求和服务器的处理过程画在同一张草图上。", "用浏览器网络面板观察路径、状态码与响应头，再到 Servlet 里对应 doGet 和 doPost。"),
        new Article("技术分享", "Servlet", "Servlet 登录后为什么要跳转", "从登录表单到个人主页，地址栏的变化能记录完整的请求顺序。", "提交成功后重定向到个人主页，刷新时浏览器重新读取页面，表单提交只有一次。"),
        new Article("技术分享", "Servlet", "Servlet 与 JSP 怎样传递数据", "做首页文章列表时，我先写好查询结果，再用请求属性传给 JSP。", "JSTL 循环负责输出每张文章卡片，日期格式由 fmt 标签统一处理。"),
        new Article("技术分享", "Servlet", "Servlet 的 Filter 链如何排顺序", "登录检查前有两步准备工作，统一编码和恢复 Cookie 登录状态。", "把编码过滤器放在前面，再依次检查会话和角色。不同的访问者可以用不同窗口演示。"),
        new Article("技术分享", "Servlet", "Servlet 上传封面时的数据格式", "普通字段和图片会一起出现在 multipart 请求里。", "我用一张小 PNG 验证表单名称、文件大小上限和保存后的访问路径。"),
        new Article("技术分享", "Servlet", "Servlet 导出文章时如何保留中文文件名", "导出接口把正文写入响应流，浏览器会保存成文本文件。", "用中文标题试下载并打开文件，就能同时确认文件名和正文编码。"),
        new Article("技术分享", "Servlet", "Servlet 会话过期后的页面行为", "页面打开后闲置一段时间，请求里的会话标识可能已经失效。", "检查个人中心和普通文章的返回结果，可以看出受保护页面与公开页面的差别。"),
        new Article("技术分享", "SQLite", "SQLite 里如何保存评论和通知", "同一次回复会写入评论表，并按接收人生成通知。", "把两次写入放进同一个 JDBC 事务，可以让评论和通知保持一致。"),
        new Article("技术分享", "SQLite", "用 SQLite 检查点赞数", "点赞记录通过文章和用户构成联合主键。", "我对照关联表和文章的点赞数，在两次点击之间观察两个数值怎样变化。"),
        new Article("学习笔记", "Java", "用 C++ 和 Java 对照理解引用", "同一份小练习分别用 C++ 和 Java 写一遍，可以看清对象传递的细节。", "先修改字段，再改变局部变量引用的对象，把两次输出分别记下来。"),
        new Article("学习笔记", "调试", "浏览器网络面板里的三条线索", "调试表单时我按请求地址、请求体和响应顺序检查。", "先看请求有没有发出，再核对表单字段，最后根据返回状态定位是哪一层处理了请求。"),
        new Article("学习笔记", "写作", "我的期中演示排练清单", "我把演练控制在五分钟内，预先打开首页、正文和关键代码。", "准备两个普通窗口或不同的浏览器配置，分别扮演写作者和留言者。"),
        new Article("生活记录", "校园", "图书馆关门前的一小时", "傍晚的借阅区比白天安静，座位旁能看到桥上的路灯亮起来。", "我把当晚要读的内容缩成两节，读完在纸上写了三个问题，回宿舍再补资料。"),
        new Article("生活记录", "校园", "雨后校园的十分钟散步", "雨停时从教学楼走到操场，路边倒影刚好映着还有光的云。", "我拍了三张照片，最终留下一张水面有波纹的，边缘刚好露出一角跑道。"),
        new Article("生活记录", "写作", "周末整理了一周的随手记", "桌上的便签里有课上听到的概念，也有路上突然想起的句子。", "先给每条便签补上日期和地点，以后打开就能想起当时在做什么。"),
        new Article("兴趣交流", "摄影", "手机摄影里的光线方向", "我在同一个路口分别用顺光、侧光和逆光拍了三张片。", "傍晚的侧光最容易保留楼面的起伏，拍之前可以先绕着主体走半圈看阴影。"),
        new Article("兴趣交流", "摄影", "给校园照片试一组低饱和色调", "选了四张阴天的照片，一次只调亮度或色温其中一项。", "我保留了墙面的暖色，也给树叶留下一点绿，四张放在一起看更统一。"),
        new Article("兴趣交流", "阅读", "交换书单的第一个月", "和同学约好每月各选一本书，阅读结束时分享一段印象最深的内容。", "我把书名、推荐理由和读后疑问分别记录下来，下次碰面时可以接着聊。"),
        new Article("技术分享", "Servlet", "一次表单从校验失败到重新提交", "注册表单漏填昵称时，我保留用户名并把焦点移到提示附近。", "回填普通字段可以减少重复输入，密码框则保持为空。"),
        new Article("技术分享", "Servlet", "JSP 分页链接怎样带上搜索条件", "翻页仍应沿用关键词、分类和标签。", "测试时用一个带加号的词，确认第二页的地址没有把加号解释成空格。"),
        new Article("技术分享", "SQLite", "给博客数据做一次备份演练", "我把演示数据库复制到临时目录，先核对表结构和文章数。", "恢复后抽查一个带评论的帖子，确认正文、评论和点赞关联都能读出来。"),
        new Article("技术分享", "SQLite", "为什么删除文章时要检查外键", "文章下挂着标签、点赞、评论和通知。", "用外键级联清理这些依赖记录后，通知页面就不会留下打不开的链接。"),
        new Article("技术分享", "调试", "读懂服务器里的第一条异常日志", "加载详情页失败时，我先找请求路径，再沿异常链找最内层错误。", "复现用同一个文章编号，避免把不同请求的日志混在一起。"),
        new Article("技术分享", "Java", "把 JDBC 资源交给 try-with-resources", "查询通常同时使用连接、语句和结果集。", "把资源声明在 try 中，异常路径也会按反向顺序关闭资源。"),
        new Article("技术分享", "Servlet", "发出通知后怎样核对接收人", "同一条回复可能涉及作者和被回复的人。", "我按三种角色关系分别准备数据，再用不同账号查看通知列表。"),
        new Article("技术分享", "Servlet", "给登录状态加一个受保护入口", "先打开个人中心会跳到登录页，登录完成后回到原来的位置。", "目标地址使用本站路径，重复打开时便于核对重定向链。"),
        new Article("学习笔记", "Java", "把 Java 集合练习写成小例题", "同一组数据先用 List 保存顺序，再用 Set 查重复项。", "练习最后输出重复出现的元素，顺便检查输入里含空值时的行为。"),
        new Article("学习笔记", "调试", "写用例时先记录操作动机", "一次完整的用例包含前置状态、用户操作和页面结果。", "我把失败场景也写进去，例如密码错误后仍停在登录流程。"),
        new Article("学习笔记", "写作", "先画请求顺序再截图", "我在纸上标出浏览器、Servlet、DAO 和数据库。", "紧接着按同一个流程截图，报告上的箭头和页面变化就能一一对应。"),
        new Article("学习笔记", "阅读", "把课件里的例子换成自己的数据", "改写例题时，我先寻找最小输入，再增加边界情况。", "保留一组熟悉的数据便于手算，另一组用来检查程序的实际输出。"),
        new Article("学习笔记", "写作", "小组项目的交接记录怎样写", "我把入口地址、演示账号、启动命令和改动文件放在同一页。", "最后让同学照着记录跑一遍，再补上他遇到的环境差异。"),
        new Article("学习笔记", "SQLite", "用手写 SQL 验证分页条数", "我先查分类下的文章总数，再给结果加 LIMIT 和 OFFSET。", "把第一页和第二页的主键对照起来，就能检查是不是重复或漏掉了文章。"),
        new Article("学习笔记", "调试", "网络面板里找不到请求怎么办", "我先检查按钮所在表单是否有必填项未填。", "通过表单校验后，再检查脚本事件是否成功绑定。"),
        new Article("生活记录", "校园", "食堂窗口换了一张菜单", "周三中午去得早，第一次发现窗口会把当天的菜分别排在手写纸上。", "同桌选了最普通的一份盖饭，我记下那家窗口下周还会再去。"),
        new Article("生活记录", "校园", "早八路上的桂花树", "走去教学楼的坡道上，有几棵树远远闻起来比看起来更显眼。", "我换了一条小路慢慢走，顺手记下最容易经过的那个路口。"),
        new Article("生活记录", "写作", "关掉通知之后专心做完一件事", "我给手机设了四十分钟免打扰，然后把桌上的小任务都收进清单。", "结束时只勾掉一个条目，终于把拖了两周的读书笔记写完。"),
        new Article("生活记录", "校园", "在空教室等下一堂课", "走廊人声比教室里热闹，靠窗那排桌子晒得有些暖。", "课间只剩二十分钟，我把刚才没听懂的问题重新抄了一次。"),
        new Article("生活记录", "写作", "和室友商量一周的值日表", "上周的纸条被吹落在门后，大家总需要重新查今天轮到谁。", "这次把名字和周次写在固定的位置，临时换班只加一笔备注。"),
        new Article("生活记录", "摄影", "晚霞比天气预报早到十分钟", "出门时天空还是淡蓝色，走到桥边已经出现一条橙色的云。", "我站在同一处观察到颜色变淡，留下了一张最接近当时感觉的照片。"),
        new Article("生活记录", "阅读", "借来的旧书里夹着一张车票", "纸质车票背面只写了一个日期，书页边缘留着前一次阅读的折角。", "归还前我把车票夹回原处，在自己的本子上记下那段读过的文字。"),
        new Article("生活记录", "校园", "第一次在操场跑完五圈", "一开始跑得急，第二圈过后就放慢到可以说出一句完整的话。", "最后一圈没有加速，保持呼吸比刷新时间更容易坚持。"),
        new Article("兴趣交流", "摄影", "给一张逆光照片留一点暗部", "树下的阴影压得有点重，但我仍能看到长椅的形状。", "调整时先降低高光，再提一点阴影，画面保留了光线的方向。"),
        new Article("兴趣交流", "摄影", "拍书桌时先挪走三件东西", "我原先把水杯、充电线和笔袋都塞在取景范围内。", "清理桌面后，最先注意到的是窗边的光，再加回一只杯子作为参照。"),
        new Article("兴趣交流", "阅读", "通勤时适合读什么样的书", "我挑了章节短的散文集，通常能在两站之间读完一小节。", "读到想回头看的句子先折页，晚上再抄进摘记本。"),
        new Article("兴趣交流", "阅读", "从四本书里选下一次共读", "候选书分别是随笔、小说、科普和一部人物传记。", "我们先读每本的目录，再讲一个想读下去的理由，最后按可读时间决定。"),
        new Article("兴趣交流", "写作", "周末试了一次十分钟速写", "我选了桌上的台灯，只画外轮廓和一道投影。", "第二张换个角度把灯座挡住一半，画起来反倒更容易看清线条。"),
        new Article("兴趣交流", "校园", "一条适合周末骑行的短路线", "从校门出发绕河边一圈，途中有两段需要推车通过。", "我标下补水点和几个转弯位置，下次可以带同学一起走。"),
        new Article("兴趣交流", "摄影", "用同一焦段拍完一整天", "限定一个焦段后，我开始靠走近和走远调整画面。", "睡前把照片按拍摄时间排开，观察上午、午后和傍晚的颜色变化。")
    };

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !java.nio.file.Files.isRegularFile(java.nio.file.Path.of(args[0]))) {
            throw new IllegalArgumentException("请传入已初始化的 SQLite 数据库路径");
        }
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + args[0])) {
            execute(db, "PRAGMA foreign_keys=ON");
            execute(db, "PRAGMA busy_timeout=10000");
            db.setAutoCommit(false);
            try {
                seed(db);
                db.commit();
            } catch (Exception failure) {
                db.rollback();
                throw failure;
            }
        }
    }

    static void seed(Connection db) throws Exception {
        int lin = number(db, "SELECT id FROM users WHERE username = ?", "lin");
        int[] authors = {lin, user(db, "demo_ruo", "若水"), user(db, "demo_qi", "小齐"),
                user(db, "demo_yu", "柏舟"), user(db, "demo_chen", "晨曦")};
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int added = 0;
        for (int i = 0; i < ARTICLES.length; i++) {
            Article article = ARTICLES[i];
            int author = authors[i % authors.length];
            if (number(db, "SELECT id FROM posts WHERE user_id = ? AND title = ?", author, article.title()) > 0) {
                continue;
            }
            int reader = authors[(i + 1) % authors.length];
            int category = number(db, "SELECT id FROM categories WHERE name = ?", article.category());
            String date = now.minusHours(12L + i * 18L).format(format);
            String commentDate = now.minusHours(7L + i * 18L).format(format);
            int post = insert(db, """
                    INSERT INTO posts(user_id,category_id,title,summary,content,view_count,like_count,created_at,updated_at)
                    VALUES(?,?,?,?,?,?,1,?,?)""", author, category, article.title(), article.opening(),
                    article.opening() + "\n\n" + article.detail(), 17 + ((i * 23) % 94), date, date);
            execute(db, "INSERT OR IGNORE INTO tags(name) VALUES(?)", article.tag());
            execute(db, "INSERT INTO post_tags(post_id,tag_id) SELECT ?,id FROM tags WHERE name = ?", post, article.tag());
            execute(db, "INSERT INTO post_likes(post_id,user_id,created_at) VALUES(?,?,?)", post, reader, commentDate);
            int comment = insert(db, "INSERT INTO comments(post_id,user_id,content,created_at) VALUES(?,?,?,?)",
                    post, reader, "看完想试试这个方法。我先记下这句：" + article.detail(), commentDate);
            int reply = insert(db, """
                    INSERT INTO comments(post_id,user_id,parent_id,content,created_at) VALUES(?,?,?,?,?)""",
                    post, author, comment, "谢谢留言！试完也欢迎回来分享你的结果。", commentDate);
            execute(db, """
                    INSERT INTO notifications(user_id,actor_id,type,post_id,comment_id,is_read,created_at)
                    VALUES(?,?,?,?,?,?,?)""", author, reader, "COMMENT", post, comment, i % 2, commentDate);
            execute(db, """
                    INSERT INTO notifications(user_id,actor_id,type,post_id,comment_id,is_read,created_at)
                    VALUES(?,?,?,?,?,?,?)""", reader, author, "REPLY", post, reply, (i + 1) % 2, commentDate);
            added++;
        }
        execute(db, """
                INSERT INTO posts(user_id,title,content,status)
                SELECT ?, '下周想写的三篇文章', '整理演示步骤、记录 SQLite 查询、补一份排练时间表。', 0
                WHERE NOT EXISTS (SELECT 1 FROM posts WHERE user_id = ? AND title = '下周想写的三篇文章')
                """, lin, lin);
        System.out.printf("新增公开文章 %d；当前公开文章 %d，用户 %d，评论 %d，通知 %d。%n",
                added, number(db, "SELECT count(*) FROM posts WHERE status = 1"),
                number(db, "SELECT count(*) FROM users"), number(db, "SELECT count(*) FROM comments"),
                number(db, "SELECT count(*) FROM notifications"));
    }

    static int user(Connection db, String username, String nickname) throws Exception {
        int existing = number(db, "SELECT id FROM users WHERE username = ?", username);
        if (existing > 0) {
            return existing;
        }
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        byte[] raw = (salt + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
        return insert(db, "INSERT INTO users(username,nickname,password,salt,bio) VALUES(?,?,?,?,?)",
                username, nickname, hash, salt, "微光博客演示作者");
    }

    static int insert(Connection db, String sql, Object... values) throws SQLException {
        execute(db, sql, values);
        return number(db, "SELECT last_insert_rowid()");
    }

    static void execute(Connection db, String sql, Object... values) throws SQLException {
        try (PreparedStatement stmt = db.prepareStatement(sql)) {
            bind(stmt, values);
            stmt.execute();
        }
    }

    static int number(Connection db, String sql, Object... values) throws SQLException {
        try (PreparedStatement stmt = db.prepareStatement(sql)) {
            bind(stmt, values);
            try (var result = stmt.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    static void bind(PreparedStatement stmt, Object[] values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            stmt.setObject(i + 1, values[i]);
        }
    }
}
